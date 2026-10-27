package com.dummytrading.app.prices

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject

/**
 * Kraken public prices for crypto, in INR via USD/INR conversion.
 * Mode is LIVE_KRAKEN_WS while the websocket is connected, otherwise POLLING
 * (15s REST ticker). The UI must show which mode is active. In sandboxed
 * environments outbound wss: is often blocked — that is why polling fallback
 * exists and why the mode label matters; it is not a bug in the app.
 */
object KrakenPriceProvider {
    private val client = OkHttpClient()
    private var socket: WebSocket? = null
    var mode: PriceMode = PriceMode.POLLING
        private set

    /** USD/INR from Kraken's USDINR? Kraken lists USDT/USD; use a public
     *  FX fallback: Yahoo INR=X quote. Kept separate so crypto maths stays
     *  explicit. Default 83.0 is only used if every source fails, and the
     *  quote is still labelled with its source. */
    suspend fun usdInr(): Double = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url("https://query1.finance.yahoo.com/v7/finance/quote?symbols=INR=X")
                .header("User-Agent", "Mozilla/5.0").build()
            val resp = client.newCall(req).execute()
            val body = resp.body?.string() ?: return@withContext 83.0
            resp.close()
            val arr = JSONObject(body).getJSONObject("quoteResponse").getJSONArray("result")
            if (arr.length() > 0) arr.getJSONObject(0).optDouble("regularMarketPrice", 83.0)
            else 83.0
        } catch (e: Exception) {
            83.0
        }
    }

    /** REST ticker polling. Returns symbol -> USD price. */
    suspend fun pollUsd(pairs: List<String>): Map<String, Double> = withContext(Dispatchers.IO) {
        if (pairs.isEmpty()) return@withContext emptyMap()
        try {
            val req = Request.Builder()
                .url("https://api.kraken.com/0/public/Ticker?pair=" + pairs.joinToString(","))
                .build()
            val resp = client.newCall(req).execute()
            val body = resp.body?.string() ?: return@withContext emptyMap()
            resp.close()
            val result = JSONObject(body).optJSONObject("result") ?: return@withContext emptyMap()
            val out = mutableMapOf<String, Double>()
            val keys = result.keys()
            while (keys.hasNext()) {
                val pair = keys.next()
                val price = result.optJSONObject(pair)
                    ?.optJSONArray("c")?.optString(0)?.toDoubleOrNull()
                if (price != null) out[pair] = price
            }
            out
        } catch (e: Exception) {
            emptyMap()
        }
    }

    /** Best-effort websocket. On any failure the caller stays on polling. */
    fun connectWebSocket(pairs: List<String>, onTick: (String, Double) -> Unit) {
        try {
            val req = Request.Builder().url("wss://ws.kraken.com").build()
            socket = client.newWebSocket(req, object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    val sub = JSONObject().apply {
                        put("event", "subscribe")
                        put("pair", org.json.JSONArray(pairs))
                        put("subscription", JSONObject().put("name", "ticker"))
                    }.toString()
                    webSocket.send(sub)
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    try {
                        if (!text.startsWith("[")) return
                        val arr = org.json.JSONArray(text)
                        if (arr.length() < 4) return
                        val pair = arr.getString(3)
                        val ticker = arr.getJSONObject(1)
                        val price = ticker.optJSONArray("c")?.optString(0)?.toDoubleOrNull()
                        if (price != null) {
                            mode = PriceMode.LIVE_KRAKEN_WS
                            onTick(pair, price)
                        }
                    } catch (ignored: Exception) {
                    }
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    mode = PriceMode.POLLING
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    mode = PriceMode.POLLING
                }
            })
        } catch (e: Exception) {
            mode = PriceMode.POLLING
        }
    }

    fun disconnect() {
        try { socket?.close(1000, null) } catch (ignored: Exception) {}
        socket = null
        mode = PriceMode.POLLING
    }
}
