package com.dummytrading.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dummytrading.app.data.AppDatabase
import com.dummytrading.app.data.CatalogItem
import com.dummytrading.app.data.HoldingEntity
import com.dummytrading.app.data.OrderEntity
import com.dummytrading.app.data.WatchlistEntity
import com.dummytrading.app.prices.MarketHours
import com.dummytrading.app.prices.PriceRepository
import com.dummytrading.app.prices.Quote
import com.dummytrading.app.prices.UpstoxFeed
import com.dummytrading.app.trading.TradingEngine
import com.dummytrading.app.trading.TradingEngine.AssetType
import com.dummytrading.app.trading.TradingEngine.Exchange
import com.dummytrading.app.trading.TradingEngine.Side
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class TradingViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = (application as com.dummytrading.app.DummyTradingApp).database.tradingDao()
    val upstox = UpstoxFeed(application.applicationContext)
    val prices = PriceRepository(upstox, viewModelScope)

    private val mutex = Mutex()

    private val _cash = MutableStateFlow(TradingEngine.STARTING_CASH)
    val cash: StateFlow<Double> = _cash.asStateFlow()

    private val _holdings = MutableStateFlow<List<HoldingEntity>>(emptyList())
    val holdings: StateFlow<List<HoldingEntity>> = _holdings.asStateFlow()

    private val _orders = MutableStateFlow<List<OrderEntity>>(emptyList())
    val orders: StateFlow<List<OrderEntity>> = _orders.asStateFlow()

    private val _watchlist = MutableStateFlow<List<WatchlistEntity>>(emptyList())
    val watchlist: StateFlow<List<WatchlistEntity>> = _watchlist.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _upstoxStatus = MutableStateFlow<UpstoxFeed.Status?>(null)
    val upstoxStatus: StateFlow<UpstoxFeed.Status?> = _upstoxStatus.asStateFlow()

    val marketOpen: Boolean
        get() = MarketHours.isIndianMarketOpen(System.currentTimeMillis())

    init {
        viewModelScope.launch { _cash.value = AppDatabase.cash(dao) }
        viewModelScope.launch { dao.holdings().collect { _holdings.value = it } }
        viewModelScope.launch { dao.orders().collect { _orders.value = it } }
        viewModelScope.launch { dao.watchlist().collect { _watchlist.value = it } }
        prices.startKrakenWebSocket()
        prices.refreshStocks()
        prices.refreshCrypto()
        viewModelScope.launch {
            while (true) {
                delay(30_000)
                prices.refreshStocks()
            }
        }
        viewModelScope.launch {
            while (true) {
                delay(15_000)
                prices.refreshCrypto()
            }
        }
        refreshUpstoxStatus()
    }

    override fun onCleared() {
        prices.stopKrakenWebSocket()
        super.onCleared()
    }

    fun refreshUpstoxStatus() {
        viewModelScope.launch { _upstoxStatus.value = upstox.status() }
    }

    /** Downloads Upstox's public instrument master and builds the
     *  symbol -> instrument_key map used for live quotes. */
    fun syncUpstoxInstruments() {
        viewModelScope.launch {
            _message.value = "Syncing Upstox instruments…"
            val n = upstox.syncInstruments()
            _message.value = if (n > 0) "Upstox instruments synced: $n rows"
            else "Upstox instrument sync failed — delayed prices will be used"
            refreshUpstoxStatus()
        }
    }

    fun refreshPrices() {
        prices.refreshStocks()
        prices.refreshCrypto()
        _message.value = "Prices refreshed"
    }

    fun quoteFor(item: CatalogItem, exchange: Exchange): Quote? =
        prices.quoteFor(item, exchange)

    fun holdingFor(symbol: String, exchange: Exchange): HoldingEntity? =
        _holdings.value.firstOrNull { it.symbol == symbol && it.exchange == exchange.name }

    fun isWatched(symbol: String, exchange: Exchange): Boolean =
        _watchlist.value.any { it.symbol == symbol && it.exchange == exchange.name }

    fun toggleWatchlist(item: CatalogItem, exchange: Exchange) {
        viewModelScope.launch {
            if (isWatched(item.symbol, exchange)) {
                dao.removeWatchlist(item.symbol, exchange.name)
            } else {
                dao.addWatchlist(
                    WatchlistEntity(item.symbol, exchange.name, item.name, item.assetType.name)
                )
            }
        }
    }

    /** Dummy BUY. Validates cash, updates holding average, records the order.
     *  Returns an error string, or null on success. */
    fun buy(
        item: CatalogItem, exchange: Exchange, price: Double, qty: Double,
        note: String, source: String
    ) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                mutex.withLock {
                    val cashNow = AppDatabase.cash(dao)
                    val gross = price * qty
                    val charges = if (item.assetType == AssetType.CRYPTO) {
                        TradingEngine.cryptoChargesTotal(gross, true)
                    } else {
                        TradingEngine.stockChargesTotal(gross, exchange, true)
                    }
                    val err = TradingEngine.validateBuy(cashNow, gross, charges)
                    if (err != null) return@withLock err
                    val net = TradingEngine.netAmount(gross, charges, true)
                    val old = dao.holding(item.symbol, exchange.name)
                    val newAvg = TradingEngine.newAveragePrice(
                        old?.qty ?: 0.0, old?.avgPrice ?: 0.0, qty, price
                    )
                    dao.upsertHolding(
                        HoldingEntity(
                            item.symbol, exchange.name, item.name, item.assetType.name,
                            (old?.qty ?: 0.0) + qty, newAvg
                        )
                    )
                    dao.insertOrder(
                        OrderEntity(
                            symbol = item.symbol, name = item.name,
                            exchange = exchange.name, assetType = item.assetType.name,
                            side = Side.BUY.name, qty = qty, price = price,
                            gross = gross, charges = charges, net = net,
                            realisedPl = 0.0, note = note, priceSource = source,
                            timestamp = System.currentTimeMillis()
                        )
                    )
                    AppDatabase.setCash(dao, cashNow - net)
                    _cash.value = cashNow - net
                    null
                }
            }
            _message.value = result ?: "Dummy BUY placed — no real money moved"
        }
    }

    /** Dummy SELL. Validates holdings, computes realised P&L, updates holding. */
    fun sell(
        item: CatalogItem, exchange: Exchange, price: Double, qty: Double,
        note: String, source: String
    ) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                mutex.withLock {
                    val cashNow = AppDatabase.cash(dao)
                    val old = dao.holding(item.symbol, exchange.name)
                    val held = old?.qty ?: 0.0
                    val err = TradingEngine.validateSell(held, qty)
                    if (err != null) return@withLock err
                    val gross = price * qty
                    val charges = if (item.assetType == AssetType.CRYPTO) {
                        TradingEngine.cryptoChargesTotal(gross, false)
                    } else {
                        TradingEngine.stockChargesTotal(gross, exchange, false)
                    }
                    val net = TradingEngine.netAmount(gross, charges, false)
                    val realised = TradingEngine.realisedPl(
                        price, old?.avgPrice ?: price, qty, charges
                    )
                    val remaining = held - qty
                    if (remaining <= 1e-9) {
                        dao.deleteHolding(item.symbol, exchange.name)
                    } else {
                        dao.upsertHolding(old!!.copy(qty = remaining))
                    }
                    dao.insertOrder(
                        OrderEntity(
                            symbol = item.symbol, name = item.name,
                            exchange = exchange.name, assetType = item.assetType.name,
                            side = Side.SELL.name, qty = qty, price = price,
                            gross = gross, charges = charges, net = net,
                            realisedPl = realised, note = note, priceSource = source,
                            timestamp = System.currentTimeMillis()
                        )
                    )
                    AppDatabase.setCash(dao, cashNow + net)
                    _cash.value = cashNow + net
                    null
                }
            }
            _message.value = result ?: "Dummy SELL placed — no real money moved"
        }
    }

    fun resetPortfolio() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                mutex.withLock {
                    dao.clearHoldings()
                    dao.clearOrders()
                    dao.clearWatchlist()
                    AppDatabase.setCash(dao, TradingEngine.STARTING_CASH)
                    _cash.value = TradingEngine.STARTING_CASH
                }
            }
            _message.value = "Portfolio reset to ₹10,00,000 virtual cash"
        }
    }

    fun clearMessage() { _message.value = null }
}
