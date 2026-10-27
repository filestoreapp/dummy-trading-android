package com.dummytrading.app.prices

import com.dummytrading.app.data.Catalog
import com.dummytrading.app.data.CatalogItem
import com.dummytrading.app.trading.TradingEngine.Exchange
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Single price cache for the whole app. Refresh policy:
 *  - Stocks: Yahoo delayed, refreshed every 30s in market hours, otherwise
 *    last close (labelled delayed + timestamp).
 *  - Crypto: Kraken websocket when connected, otherwise 15s polling.
 *  - Upstox (if configured + valid): tried first for equities; on any
 *    failure the Yahoo delayed quote is used and labelled as such.
 * The cache never invents a price: a missing quote simply stays absent
 * and the UI shows "Price unavailable".
 */
class PriceRepository(
    private val upstox: UpstoxFeed?,
    private val scope: CoroutineScope
) {
    private val _quotes = MutableStateFlow<Map<String, Quote>>(emptyMap())
    val quotes: StateFlow<Map<String, Quote>> = _quotes

    private val _usdInr = MutableStateFlow(83.0)
    val usdInr: StateFlow<Double> = _usdInr

    fun key(symbol: String, exchange: String) = "$symbol|$exchange"

    fun quoteFor(symbol: String, exchange: String): Quote? = _quotes.value[key(symbol, exchange)]

    fun refreshStocks() {
        scope.launch {
            val yahooSymbols = Catalog.stocks.flatMap {
                listOfNotNull(it.yahooNse, it.yahooBse)
            }
            // Yahoo allows many symbols per call; chunk to stay safe.
            val fresh = mutableMapOf<String, Quote>()
            for (chunk in yahooSymbols.chunked(50)) {
                fresh.putAll(YahooPriceProvider.quotes(chunk))
            }
            // Upstox override, only where it actually returned a quote.
            if (upstox != null && upstox.isConfigured() && !upstox.isExpired()) {
                for (item in Catalog.stocks) {
                    for (ex in listOf(Exchange.NSE, Exchange.BSE)) {
                        val q = upstox.quote(item.symbol, ex.name) ?: continue
                        fresh[key(item.symbol, ex.name)] = q
                    }
                }
            }
            if (fresh.isNotEmpty()) {
                _quotes.value = _quotes.value + fresh
            }
        }
    }

    fun refreshCrypto() {
        scope.launch {
            val rate = KrakenPriceProvider.usdInr()
            _usdInr.value = rate
            val pairs = Catalog.crypto.mapNotNull { it.krakenPair }
            val usd = KrakenPriceProvider.pollUsd(pairs)
            if (usd.isEmpty()) return@launch
            val fresh = mutableMapOf<String, Quote>()
            for (item in Catalog.crypto) {
                val pair = item.krakenPair ?: continue
                val priceUsd = usd[pair] ?: continue
                fresh[key(item.symbol, "CRYPTO")] = Quote(
                    symbol = item.symbol, exchange = "CRYPTO",
                    priceInr = priceUsd * rate, source = "Kraken",
                    mode = KrakenPriceProvider.mode,
                    asOfMillis = System.currentTimeMillis(), usdInr = rate
                )
            }
            _quotes.value = _quotes.value + fresh
        }
    }

    fun startKrakenWebSocket() {
        val pairs = Catalog.crypto.mapNotNull { it.krakenPair }
        KrakenPriceProvider.connectWebSocket(pairs) { pair, priceUsd ->
            scope.launch {
                val item = Catalog.crypto.firstOrNull { it.krakenPair == pair } ?: return@launch
                val rate = _usdInr.value
                val q = Quote(
                    symbol = item.symbol, exchange = "CRYPTO",
                    priceInr = priceUsd * rate, source = "Kraken",
                    mode = PriceMode.LIVE_KRAKEN_WS,
                    asOfMillis = System.currentTimeMillis(), usdInr = rate
                )
                _quotes.value = _quotes.value + (key(item.symbol, "CRYPTO") to q)
            }
        }
    }

    fun stopKrakenWebSocket() = KrakenPriceProvider.disconnect()

    /** Convenience for the detail screen. */
    fun quoteFor(item: CatalogItem, exchange: Exchange): Quote? =
        quoteFor(item.symbol, exchange.name)
}
