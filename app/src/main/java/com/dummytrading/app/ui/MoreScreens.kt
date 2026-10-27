package com.dummytrading.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.dummytrading.app.data.Catalog
import com.dummytrading.app.trading.TradingEngine.Exchange
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun fmtTime(ts: Long): String =
    SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(ts))

@Composable
fun HoldingsScreen(vm: TradingViewModel, nav: NavHostController) {
    val holdings by vm.holdings.collectAsState()
    val quotes by vm.prices.quotes.collectAsState()
    LazyColumn(Modifier.fillMaxSize().padding(16.dp)) {
        item { Text("Holdings (per exchange)", fontSize = 20.sp, fontWeight = FontWeight.Bold) }
        if (holdings.isEmpty()) {
            item { Text("No holdings yet. Search the Market tab and place a dummy buy.") }
        }
        items(holdings) { h ->
            val q = quotes["${h.symbol}|${h.exchange}"]
            val value = h.qty * (q?.priceInr ?: h.avgPrice)
            Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)
                .clickable { nav.navigate("detail/${h.symbol}/${h.exchange}") }) {
                Column(Modifier.padding(12.dp)) {
                    Text("${h.symbol} (${h.exchange})", fontWeight = FontWeight.Bold)
                    Text("${h.qty} @ avg ${money(h.avgPrice)}  •  Value ${money(value)}")
                    Text("P&L ${money(value - h.qty * h.avgPrice)}  •  ${sourceLabel(q)}")
                }
            }
        }
    }
}

@Composable
fun OrdersScreen(vm: TradingViewModel) {
    val orders by vm.orders.collectAsState()
    val realisedTotal = orders.filter { it.side == "SELL" }.sumOf { it.realisedPl }
    LazyColumn(Modifier.fillMaxSize().padding(16.dp)) {
        item {
            Text("Orders & Journal", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("Total realised P&L (sells): ${money(realisedTotal)}")
        }
        if (orders.isEmpty()) {
            item { Text("No orders yet.") }
        }
        items(orders) { o ->
            Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Column(Modifier.padding(12.dp)) {
                    Text("${o.side} ${o.symbol} (${o.exchange}) — ${o.qty} @ ${money(o.price)}", fontWeight = FontWeight.Bold)
                    Text("Gross ${money(o.gross)}  •  Charges ${money(o.charges)}  •  Net ${money(o.net)}")
                    if (o.side == "SELL") Text("Realised P&L ${money(o.realisedPl)}")
                    if (o.note.isNotBlank()) Text("Note: ${o.note}")
                    Text("${fmtTime(o.timestamp)}  •  Source: ${o.priceSource}")
                }
            }
        }
    }
}

@Composable
fun WatchlistScreen(vm: TradingViewModel, nav: NavHostController) {
    val watchlist by vm.watchlist.collectAsState()
    val quotes by vm.prices.quotes.collectAsState()
    LazyColumn(Modifier.fillMaxSize().padding(16.dp)) {
        item { Text("Watchlist", fontSize = 20.sp, fontWeight = FontWeight.Bold) }
        if (watchlist.isEmpty()) {
            item { Text("Nothing watched yet. Star items from the Market tab.") }
        }
        items(watchlist) { w ->
            val q = quotes["${w.symbol}|${w.exchange}"]
            Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)
                .clickable { nav.navigate("detail/${w.symbol}/${w.exchange}") }) {
                Row(Modifier.padding(12.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text("${w.symbol} (${w.exchange})", fontWeight = FontWeight.Bold)
                        Text(sourceLabel(q))
                    }
                    Text(if (q != null) money(q.priceInr) else "—")
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(vm: TradingViewModel) {
    val status by vm.upstoxStatus.collectAsState()
    var apiKey by remember { mutableStateOf("") }
    var token by remember { mutableStateOf("") }
    var showReset by remember { mutableStateOf(false) }
    var syncMsg by remember { mutableStateOf<String?>(null) }

    LazyColumn(Modifier.fillMaxSize().padding(16.dp)) {
        item {
            Text("Settings", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("This app is phone-local and dummy-only. It never syncs to the web version, and it never places a real order.", fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Text("Upstox live feed (optional, feed only)", fontWeight = FontWeight.Bold)
            Text("Token is stored only on this device (encrypted). It expires at 03:30 IST the next morning. If Upstox is not connected or the token is expired, prices fall back to delayed Yahoo / Kraken data and are labelled as such. Never share your token in chat or screenshots.")
            Text(
                when {
                    status == null -> "Upstox status: checking…"
                    !status!!.configured -> "Upstox: not connected"
                    status!!.tokenExpired -> "Upstox: token expired — enter a fresh token"
                    status!!.tokenValid == true -> "Upstox: connected (token ${vm.upstox.maskedToken()})"
                    else -> "Upstox: token could not be validated — using delayed data"
                }
            )
            Text("Instrument map rows: ${status?.instrumentCount ?: 0}")
            OutlinedTextField(
                value = apiKey, onValueChange = { apiKey = it },
                label = { Text("Upstox API key") }, modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = token, onValueChange = { token = it },
                label = { Text("Upstox access token") }, modifier = Modifier.fillMaxWidth()
            )
            Row {
                Button(onClick = {
                    vm.upstox.saveCredentials(apiKey, token)
                    vm.refreshUpstoxStatus()
                    vm.refreshPrices()
                }) { Text("Save Upstox") }
                Spacer(Modifier.padding(4.dp))
                OutlinedButton(onClick = {
                    vm.upstox.clearCredentials()
                    vm.refreshUpstoxStatus()
                }) { Text("Disconnect") }
            }
            OutlinedButton(onClick = {
                vm.syncUpstoxInstruments()
                syncMsg = "Sync started — see the message on the Portfolio tab"
            }) { Text("Sync Upstox instruments") }
            syncMsg?.let { Text(it) }
            Spacer(Modifier.height(16.dp))
            Text("Price modes", fontWeight = FontWeight.Bold)
            Text("• Delayed — Yahoo, ~15 min old, stocks\n• Live via Upstox — only when connected and valid\n• Live via Kraken — crypto websocket\n• Live polling — Kraken REST, every 15s\nAlways check the label on a price before judging a trade.")
            Spacer(Modifier.height(16.dp))
            Text("Danger zone", fontWeight = FontWeight.Bold)
            Button(onClick = { showReset = true }) { Text("Reset portfolio") }
            if (showReset) {
                AlertDialog(
                    onDismissRequest = { showReset = false },
                    title = { Text("Reset portfolio?") },
                    text = { Text("This deletes all holdings, orders and watchlist on this phone and restores ₹10,00,000 virtual cash. It cannot be undone.") },
                    confirmButton = {
                        TextButton(onClick = {
                            vm.resetPortfolio()
                            showReset = false
                        }) { Text("Reset") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showReset = false }) { Text("Cancel") }
                    }
                )
            }
            Spacer(Modifier.height(16.dp))
            Text("Expanding the catalog: edit Catalog.kt and add a stock() or crypto entry, or follow README.md 'Expanding the catalog'. The full NSE/BSE lists are not bundled in v1 on purpose — the starter list keeps the app fast on a phone.")
            Text("Catalog: ${Catalog.stocks.size} stocks, ${Catalog.crypto.size} cryptos (starter list).")
        }
    }
}
