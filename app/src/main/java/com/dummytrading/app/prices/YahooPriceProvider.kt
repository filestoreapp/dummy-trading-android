package com.dummytrading.app.prices

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

/** Yahoo Finance quotes for NSE (.NS) and BSE (.BO).
 *  These are DELAYED by about 15 minutes and must be labelled as such.
 *  Never presented as live. Falls back to the last cached price on failure. */
object YahooPriceProvider {
    private val client = OkHttpClient.Builder()
        .addInterceptor { chain ->
            chain.proceed(
                chain.request().newBuilder()
                    .header("User-Agent", "Mozilla/5.0")
                    .build()
            )
        }.build()

    suspend fun quotes(symbols: List<String>): Map<String, Quote> = withContext(Dispatchers.IO) {
        if (symbols.isEmpty()) return@withContext emptyMap()
        val url = "https://query1.finance.yahoo.com/v7/finance/quote?symbols=" +
            symbols.joinToString(",")
        try {
            val resp = client.newCall(Request.Builder().url(url).build()).execute()
            val body = resp.body?.string() ?: return@withContext emptyMap()
            resp.close()
            val results = JSONObject(body)
                .getJSONObject("quoteResponse").getJSONArray("result")
            val out = mutableMapOf<String, Quote>()
            for (i in 0 until results.length()) {
                val o = results.getJSONObject(i)
                val sym = o.optString("symbol")
                val price = o.optDouble("regularMarketPrice", Double.NaN)
                if (price.isNaN()) continue
                val prev = o.optDouble("regularMarketPreviousClose", Double.NaN)
                val chg = o.optDouble("regularMarketChangePercent", Double.NaN)
                val exchange = when {
                    sym.endsWith(".NS") -> "NSE"
                    sym.endsWith(".BO") -> "BSE"
                    else -> "NSE"
                }
                val base = sym.removeSuffix(".NS").removeSuffix(".BO")
                out[base + "|" + exchange] = Quote(
                    symbol = base, exchange = exchange, priceInr = price,
                    previousClose = if (prev.isNaN()) null else prev,
                    changePercent = if (chg.isNaN()) null else chg,
                    source = "Yahoo", mode = PriceMode.DELAYED,
                    asOfMillis = System.currentTimeMillis()
                )
            }
            out
        } catch (e: Exception) {
            emptyMap() // caller keeps the last cached quote, labelled delayed
        }
    }
}
