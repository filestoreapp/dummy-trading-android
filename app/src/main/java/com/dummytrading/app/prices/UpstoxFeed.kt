package com.dummytrading.app.prices

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.io.File
import java.util.zip.GZIPInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

/**
 * OPTIONAL Upstox feed — FEED ONLY.
 *
 * There is deliberately NO order-placement code in this file, in this app,
 * or anywhere in the project. Dummy orders are recorded locally only and
 * never leave the phone. If you search the codebase for "place_order",
 * "order/place" or similar, you will find nothing — by design.
 *
 * How it works:
 *  1. The user enters an API key + access token in Settings. They are stored
 *     ONLY on this device, in EncryptedSharedPreferences. Nothing is
 *     hardcoded, nothing is sent anywhere except api.upstox.com.
 *  2. On connect, the token is validated against /v2/user/profile.
 *     A 401 marks the token expired and the app falls back to delayed data.
 *  3. Tokens expire at 03:30 IST the next morning (see MarketHours).
 *  4. Live quotes need Upstox instrument keys. The app downloads Upstox's
 *     public instrument master (complete.csv.gz), builds a
 *     trading_symbol -> instrument_key map for NSE_EQ / BSE_EQ equities,
 *     caches it on-device, then calls /v2/market-quote/quotes.
 *     If the map or the quote fails, prices fall back to Yahoo delayed
 *     and the UI says so — a Yahoo price is never labelled "Upstox live".
 */
class UpstoxFeed(context: Context) {

    private val client = OkHttpClient()
    private val prefs = run {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
        EncryptedSharedPreferences.create(
            context, "upstox_secure", masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
    private val instrumentFile = File(context.filesDir, "upstox_instruments.txt")

    data class Status(
        val configured: Boolean,
        val tokenValid: Boolean?,
        val tokenExpired: Boolean,
        val instrumentCount: Int
    )

    fun saveCredentials(apiKey: String, accessToken: String) {
        prefs.edit()
            .putString("api_key", apiKey.trim())
            .putString("access_token", accessToken.trim())
            .putLong("saved_at", System.currentTimeMillis())
            .apply()
    }

    fun clearCredentials() {
        prefs.edit().clear().apply()
        try { instrumentFile.delete() } catch (ignored: Exception) {}
    }

    fun maskedToken(): String {
        val t = prefs.getString("access_token", "") ?: ""
        return if (t.length > 4) "••••••" + t.takeLast(4) else "not set"
    }

    fun savedAt(): Long = prefs.getLong("saved_at", 0)

    fun isConfigured(): Boolean =
        !prefs.getString("api_key", "").isNullOrBlank() &&
            !prefs.getString("access_token", "").isNullOrBlank()

    fun isExpired(now: Long = System.currentTimeMillis()): Boolean {
        val saved = savedAt()
        return saved == 0L || MarketHours.isUpstoxTokenExpired(saved, now)
    }

    suspend fun status(): Status = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext Status(false, null, false, instrumentCount())
        if (isExpired()) return@withContext Status(true, false, true, instrumentCount())
        val valid = validateToken()
        Status(true, valid, !valid, instrumentCount())
    }

    private fun instrumentCount(): Int = try {
        if (instrumentFile.exists()) instrumentFile.readLines().size else 0
    } catch (e: Exception) { 0 }

    /** Returns true only on a 2xx profile response; 401 = expired/invalid. */
    private fun validateToken(): Boolean {
        return try {
            val req = Request.Builder()
                .url("https://api.upstox.com/v2/user/profile")
                .header("Authorization", "Bearer ${prefs.getString("access_token", "")}")
                .header("Accept", "application/json").build()
            val resp = client.newCall(req).execute()
            val ok = resp.isSuccessful
            resp.close()
            ok
        } catch (e: Exception) {
            false
        }
    }

    /** Download + parse the public instrument master. Returns row count. */
    suspend fun syncInstruments(): Int = withContext(Dispatchers.IO) {
        val urls = listOf(
            "https://assets.upstox.com/market-quote/instruments/exchange/complete.csv.gz"
        )
        for (url in urls) {
            try {
                val req = Request.Builder().url(url).build()
                val resp = client.newCall(req).execute()
                if (!resp.isSuccessful) { resp.close(); continue }
                val body = resp.body ?: continue
                val map = mutableMapOf<String, String>()
                GZIPInputStream(body.byteStream()).bufferedReader().use { reader ->
                    val header = reader.readLine() ?: continue
                    val cols = header.split(",").map { it.trim().lowercase() }
                    val keyIdx = cols.indexOf("instrument_key")
                    val symIdx = cols.indexOfFirst { it == "tradingsymbol" || it == "trading_symbol" }
                    val exchIdx = cols.indexOf("exchange")
                    if (keyIdx < 0 || symIdx < 0) continue
                    reader.forEachLine { line ->
                        val parts = line.split(",")
                        if (parts.size <= maxOf(keyIdx, symIdx)) return@forEachLine
                        val exch = if (exchIdx >= 0 && parts.size > exchIdx) parts[exchIdx] else ""
                        if (exch != "NSE" && exch != "BSE") return@forEachLine
                        map[parts[symIdx].trim() + "|" + exch] = parts[keyIdx].trim()
                    }
                }
                resp.close()
                if (map.isNotEmpty()) {
                    instrumentFile.writeText(
                        map.entries.joinToString("\n") { "${it.key}=${it.value}" }
                    )
                    return@withContext map.size
                }
            } catch (e: Exception) {
                // try next URL, then fall back to delayed data
            }
        }
        0
    }

    private fun instrumentKey(symbol: String, exchange: String): String? {
        if (!instrumentFile.exists()) return null
        return try {
            instrumentFile.readLines().firstNotNullOfOrNull { line ->
                val (k, v) = line.split("=", limit = 2)
                if (k == "$symbol|$exchange") v else null
            }
        } catch (e: Exception) { null }
    }

    /** Live Upstox quote for one equity, or null on any failure. */
    suspend fun quote(symbol: String, exchange: String): Quote? = withContext(Dispatchers.IO) {
        if (!isConfigured() || isExpired()) return@withContext null
        val key = instrumentKey(symbol, exchange) ?: return@withContext null
        try {
            val req = Request.Builder()
                .url("https://api.upstox.com/v2/market-quote/quotes?instrument_key=$key")
                .header("Authorization", "Bearer ${prefs.getString("access_token", "")}")
                .header("Accept", "application/json").build()
            val resp = client.newCall(req).execute()
            if (!resp.isSuccessful) { resp.close(); return@withContext null }
            val body = resp.body?.string() ?: return@withContext null
            resp.close()
            val data = JSONObject(body).optJSONObject("data") ?: return@withContext null
            val firstKey = data.keys().next()
            val quoteObj = data.optJSONObject(firstKey) ?: return@withContext null
            val ltp = quoteObj.optDouble("last_price", Double.NaN)
            if (ltp.isNaN()) return@withContext null
            Quote(
                symbol = symbol, exchange = exchange, priceInr = ltp,
                source = "Upstox", mode = PriceMode.LIVE_UPSTOX,
                asOfMillis = System.currentTimeMillis()
            )
        } catch (e: Exception) {
            null
        }
    }
}
