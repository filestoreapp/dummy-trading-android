package com.dummytrading.app.trading

import kotlin.math.min
import kotlin.math.roundToLong

/**
 * Pure trading maths. No Android dependency, so it can be unit-tested on the JVM.
 * Everything here is VIRTUAL: no code in this app sends an order to a broker
 * or exchange. Orders are recorded locally on the phone only.
 */
object TradingEngine {

    const val STARTING_CASH = 1_000_000.0

    enum class AssetType { STOCK, CRYPTO }
    enum class Exchange { NSE, BSE, CRYPTO }
    enum class Side { BUY, SELL }

    data class ChargeBreakdown(
        val brokerage: Double,
        val stt: Double,
        val exchangeTxn: Double,
        val sebi: Double,
        val stamp: Double,
        val gst: Double,
        val total: Double
    )

    data class CryptoChargeBreakdown(val fee: Double, val tds: Double, val total: Double)

    private fun r2(v: Double): Double = (v * 100.0).roundToLong() / 100.0

    /** Indian delivery charges, matching the web platform's model.
     *  Brokerage 0.03% capped at Rs 20, STT 0.1%, exchange txn 0.00345% (NSE)
     *  / 0.00375% (BSE), SEBI 0.0001%, stamp 0.015% on buys only,
     *  GST 18% on brokerage + exchange txn. Parts are individually rounded,
     *  so their sum may differ from the total by Rs 0.01. */
    fun stockCharges(gross: Double, exchange: Exchange, isBuy: Boolean): ChargeBreakdown {
        val brokerage = r2(min(20.0, gross * 0.0003))
        val stt = r2(gross * 0.001)
        val rate = if (exchange == Exchange.BSE) 0.0000375 else 0.0000345
        val exchangeTxn = r2(gross * rate)
        val sebi = r2(gross * 0.000001)
        val stamp = if (isBuy) r2(gross * 0.00015) else 0.0
        val gst = r2((brokerage + exchangeTxn) * 0.18)
        val total = r2(brokerage + stt + exchangeTxn + sebi + stamp + gst)
        return ChargeBreakdown(brokerage, stt, exchangeTxn, sebi, stamp, gst, total)
    }

    /** Crypto: 0.25% fee on every trade, 1% TDS on sells only. */
    fun cryptoCharges(gross: Double, isBuy: Boolean): CryptoChargeBreakdown {
        val fee = r2(gross * 0.0025)
        val tds = if (isBuy) 0.0 else r2(gross * 0.01)
        return CryptoChargeBreakdown(fee, tds, r2(fee + tds))
    }

    fun stockChargesTotal(gross: Double, exchange: Exchange, isBuy: Boolean): Double =
        stockCharges(gross, exchange, isBuy).total

    fun cryptoChargesTotal(gross: Double, isBuy: Boolean): Double =
        cryptoCharges(gross, isBuy).total

    /** Net cash needed for a buy, or net cash received for a sell. */
    fun netAmount(gross: Double, charges: Double, isBuy: Boolean): Double =
        if (isBuy) r2(gross + charges) else r2(gross - charges)

    fun validateBuy(cash: Double, gross: Double, charges: Double): String? {
        if (gross <= 0) return "Price must be positive"
        return if (cash < gross + charges) "Not enough virtual cash" else null
    }

    fun validateSell(heldQty: Double, sellQty: Double): String? {
        if (sellQty <= 0) return "Quantity must be positive"
        return if (sellQty > heldQty + 1e-9) "Cannot sell more than you hold" else null
    }

    /** New average price after a buy. */
    fun newAveragePrice(oldQty: Double, oldAvg: Double, buyQty: Double, buyPrice: Double): Double {
        if (oldQty <= 0) return buyPrice
        return r2((oldQty * oldAvg + buyQty * buyPrice) / (oldQty + buyQty))
    }

    /** Realised P&L on a sell, before sell charges are added separately by callers
     *  if they want a net figure: realisedNet = (sellPrice - avg) * qty - sellCharges. */
    fun realisedPl(sellPrice: Double, avgPrice: Double, qty: Double, sellCharges: Double): Double =
        r2((sellPrice - avgPrice) * qty - sellCharges)

    fun formatInr(v: Double): String = "₹%,.2f".format(v)
}
