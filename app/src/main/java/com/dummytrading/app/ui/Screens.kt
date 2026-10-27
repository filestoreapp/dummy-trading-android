package com.dummytrading.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.dummytrading.app.data.Catalog
import com.dummytrading.app.data.CatalogItem
import com.dummytrading.app.prices.PriceMode
import com.dummytrading.app.prices.Quote
import com.dummytrading.app.trading.TradingEngine
import com.dummytrading.app.trading.TradingEngine.AssetType
import com.dummytrading.app.trading.TradingEngine.Exchange

fun money(v: Double): String = TradingEngine.formatInr(v)

fun modeLabel(q: Quote?): String = when (q?.mode) {
    PriceMode.LIVE_UPSTOX -> "Live via Upstox"
    PriceMode.LIVE_KRAKEN_WS -> "Live via Kraken"
    PriceMode.POLLING -> "Live polling (Kraken, 15s)"
    PriceMode.DELAYED, null -> "Delayed price — not live"
}

fun sourceLabel(q: Quote?): String =
    if (q == null) "Price unavailable" else "${q.source} • ${modeLabel(q)}"

@Composable
fun AppNav(vm: TradingViewModel) {
    val nav = rememberNavController()
    Scaffold(
        bottomBar = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf("home" to "Portfolio", "search" to "Market", "holdings" to "Holdings",
                    "orders" to "Orders", "watchlist" to "Watchlist", "settings" to "Settings")
                    .forEach { (route, label) ->
                        OutlinedButton(onClick = { nav.navigate(route) }) { Text(label) }
                    }
            }
        }
    ) { pad ->
        NavHost(
            navController = nav, startDestination = "home",
            modifier = Modifier.padding(pad)
        ) {
            composable("home") { HomeScreen(vm, nav) }
            composable("search") { SearchScreen(vm, nav) }
            composable("holdings") { HoldingsScreen(vm, nav) }
            composable("orders") { OrdersScreen(vm) }
            composable("watchlist") { WatchlistScreen(vm, nav) }
            composable("settings") { SettingsScreen(vm) }
            composable("detail/{symbol}/{exchange}") { back ->
                val symbol = back.arguments?.getString("symbol") ?: ""
                val ex = back.arguments?.getString("exchange") ?: "NSE"
                val item = Catalog.all.firstOrNull { it.symbol == symbol }
                if (item != null) {
                    DetailScreen(vm, item, Exchange.valueOf(ex))
                } else {
                    Text("Unknown symbol", modifier = Modifier.padding(16.dp))
                }
            }
        }
    }
}

@Composable
fun HomeScreen(vm: TradingViewModel, nav: NavHostController) {
    val cash by vm.cash.collectAsState()
    val holdings by vm.holdings.collectAsState()
    val quotes by vm.prices.quotes.collectAsState()
    val message by vm.message.collectAsState()

    var holdingsValue = 0.0
    var invested = 0.0
    for (h in holdings) {
        invested += h.qty * h.avgPrice
        val q = quotes["${h.symbol}|${h.exchange}"]
        holdingsValue += h.qty * (q?.priceInr ?: h.avgPrice)
    }
    val total = cash + holdingsValue

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        item {
            Text("Dummy Trading — phone-local", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("No real money. No orders leave this phone.", color = Color.Gray)
            Spacer(Modifier.height(12.dp))
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Total value: ${money(total)}", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text("Cash: ${money(cash)}")
                    Text("Holdings: ${money(holdingsValue)}  •  Invested: ${money(invested)}")
                    Text("Total P&L: ${money(total - TradingEngine.STARTING_CASH)}")
                    Text(if (vm.marketOpen) "NSE/BSE: market open" else "NSE/BSE: market closed — prices are last close / delayed")
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { nav.navigate("search") }) { Text("Buy / Search") }
                OutlinedButton(onClick = { vm.refreshPrices() }) { Text("Refresh prices") }
            }
            message?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = Color(0xFF1B7A3D))
            }
            Spacer(Modifier.height(12.dp))
            Text("Your holdings", fontWeight = FontWeight.Bold)
        }
        items(holdings) { h ->
            val q = quotes["${h.symbol}|${h.exchange}"]
            val value = h.qty * (q?.priceInr ?: h.avgPrice)
            val pl = value - h.qty * h.avgPrice
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                .clickable { nav.navigate("detail/${h.symbol}/${h.exchange}") }) {
                Column(Modifier.padding(12.dp)) {
                    Text("${h.symbol} (${h.exchange}) — ${h.qty} @ ${money(h.avgPrice)}")
                    Text("Value ${money(value)}  •  P&L ${money(pl)}")
                    Text(sourceLabel(q))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(vm: TradingViewModel, nav: NavHostController) {
    var query by remember { mutableStateOf("") }
    var exchange by remember { mutableStateOf(Exchange.NSE) }
    val quotes by vm.prices.quotes.collectAsState()
    val results = Catalog.search(query)

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Market", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        OutlinedTextField(
            value = query, onValueChange = { query = it },
            label = { Text("Search stocks or crypto") },
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(Exchange.NSE, Exchange.BSE, Exchange.CRYPTO).forEach { ex ->
                FilterChip(
                    selected = exchange == ex,
                    onClick = { exchange = ex },
                    label = { Text(ex.name) }
                )
            }
        }
        Text("Prices: stocks are delayed (Yahoo). Crypto is Kraken polling / websocket where available. Upstox live only when connected in Settings.")
        LazyColumn {
            items(results.filter {
                if (exchange == Exchange.CRYPTO) it.assetType == AssetType.CRYPTO
                else it.assetType == AssetType.STOCK
            }) { item ->
                val q = vm.quoteFor(item, exchange)
                val watched = vm.isWatched(item.symbol, exchange)
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    .clickable { nav.navigate("detail/${item.symbol}/${exchange.name}") }) {
                    Column(Modifier.padding(12.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${item.symbol} — ${item.name}", fontWeight = FontWeight.Bold)
                            Text(if (q != null) money(q.priceInr) else "—")
                        }
                        Text(sourceLabel(q))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { vm.toggleWatchlist(item, exchange) }) {
                                Text(if (watched) "★ Watched" else "☆ Watch")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DetailScreen(vm: TradingViewModel, item: CatalogItem, exchange: Exchange) {
    val quote = vm.quoteFor(item, exchange)
    val holding = vm.holdingFor(item.symbol, exchange)
    val message by vm.message.collectAsState()
    var qtyText by remember { mutableStateOf("1") }
    var amountText by remember { mutableStateOf("5000") }
    var note by remember { mutableStateOf("") }

    val price = quote?.priceInr
    val isCrypto = item.assetType == AssetType.CRYPTO
    val qty = if (isCrypto) {
        val amt = amountText.toDoubleOrNull() ?: 0.0
        if (price != null && price > 0) amt / price else 0.0
    } else {
        qtyText.toDoubleOrNull() ?: 0.0
    }
    val gross = if (price != null) price * qty else 0.0
    val charges = if (price != null && qty > 0) {
        if (isCrypto) TradingEngine.cryptoChargesTotal(gross, true)
        else TradingEngine.stockChargesTotal(gross, exchange, true)
    } else 0.0

    LazyColumn(Modifier.fillMaxSize().padding(16.dp)) {
        item {
            Text("${item.symbol} (${exchange.name})", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(item.name)
            Text(if (price != null) "Price: ${money(price)}" else "Price unavailable — try Refresh in Portfolio")
            Text(sourceLabel(quote))
            Text(if (vm.marketOpen || isCrypto) "Trading window open" else "Market closed — order would use last delayed price")
            holding?.let {
                Text("You hold: ${it.qty} @ avg ${money(it.avgPrice)}")
            }
            Spacer(Modifier.height(12.dp))
            if (isCrypto) {
                OutlinedTextField(
                    value = amountText, onValueChange = { amountText = it },
                    label = { Text("Amount in ₹") }, modifier = Modifier.fillMaxWidth()
                )
                Text("That buys about ${"%.8f".format(qty)} ${item.symbol}")
            } else {
                OutlinedTextField(
                    value = qtyText, onValueChange = { qtyText = it },
                    label = { Text("Quantity") }, modifier = Modifier.fillMaxWidth()
                )
            }
            OutlinedTextField(
                value = note, onValueChange = { note = it },
                label = { Text("Journal note — why this trade?") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Text("Estimated: gross ${money(gross)} + charges ${money(charges)} = ${money(gross + charges)}")
            Text("Dummy only. This price is ${quote?.source ?: "unavailable"} / ${modeLabel(quote)}.")
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    enabled = price != null && qty > 0,
                    onClick = {
                        if (price != null) vm.buy(item, exchange, price, qty, note, quote?.source ?: "—")
                    }
                ) { Text("Dummy BUY") }
                OutlinedButton(
                    enabled = price != null && qty > 0 && (holding?.qty ?: 0.0) >= qty,
                    onClick = {
                        if (price != null) vm.sell(item, exchange, price, qty, note, quote?.source ?: "—")
                    }
                ) { Text("Dummy SELL") }
            }
            message?.let {
                Spacer(Modifier.height(8.dp))
                Text(it)
            }
        }
    }
}
