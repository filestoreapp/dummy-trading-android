package com.dummytrading.app.prices

/** A price is never shown without its provenance. Delayed must never be
 *  mistaken for live. */
enum class PriceMode { DELAYED, LIVE_UPSTOX, LIVE_KRAKEN_WS, POLLING }

data class Quote(
    val symbol: String,
    val exchange: String,
    val priceInr: Double,
    val previousClose: Double? = null,
    val changePercent: Double? = null,
    val source: String,      // Yahoo, Kraken, Upstox
    val mode: PriceMode,
    val asOfMillis: Long,
    val usdInr: Double? = null
) {
    val isMarketOpen: Boolean
        get() {
            // NSE/BSE hours 09:15-15:30 Asia/Kolkata, Mon-Fri. Computed by the
            // repository from the quote timestamp; crypto is always open.
            return exchange == "CRYPTO"
        }
}

object MarketHours {
    /** True if the given epoch millis fall in NSE/BSE hours (IST, Mon-Fri). */
    fun isIndianMarketOpen(nowMillis: Long): Boolean {
        val ist = java.util.TimeZone.getTimeZone("Asia/Kolkata")
        val cal = java.util.Calendar.getInstance(ist).apply { timeInMillis = nowMillis }
        val dow = cal.get(java.util.Calendar.DAY_OF_WEEK)
        if (dow == java.util.Calendar.SATURDAY || dow == java.util.Calendar.SUNDAY) return false
        val minutes = cal.get(java.util.Calendar.HOUR_OF_DAY) * 60 +
            cal.get(java.util.Calendar.MINUTE)
        return minutes in (9 * 60 + 15)..(15 * 60 + 30)
    }

    fun isUpstoxTokenExpired(savedMillis: Long, nowMillis: Long): Boolean {
        // Upstox access tokens expire at 03:30 IST the next morning.
        val ist = java.util.TimeZone.getTimeZone("Asia/Kolkata")
        val cal = java.util.Calendar.getInstance(ist).apply { timeInMillis = savedMillis }
        cal.set(java.util.Calendar.HOUR_OF_DAY, 3)
        cal.set(java.util.Calendar.MINUTE, 30)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        if (cal.timeInMillis <= savedMillis) cal.add(java.util.Calendar.DAY_OF_YEAR, 1)
        return nowMillis >= cal.timeInMillis
    }
}
