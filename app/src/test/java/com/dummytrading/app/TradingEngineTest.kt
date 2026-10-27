package com.dummytrading.app

import com.dummytrading.app.trading.TradingEngine
import com.dummytrading.app.trading.TradingEngine.Exchange
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Test

class TradingEngineTest {

    @Test
    fun startingCashIsTenLakh() {
        assertEquals(1_000_000.0, TradingEngine.STARTING_CASH, 0.0)
    }

    @Test
    fun stockChargesMatchWebPlatformExample() {
        // 2 x RELIANCE @ 1201.80 = 2403.60 gross -> total charges ~3.70
        val c = TradingEngine.stockCharges(2403.60, Exchange.NSE, true)
        assertEquals(3.70, c.total, 0.02)
        assertEquals(0.72, c.brokerage, 0.01)
    }

    @Test
    fun bseAndNseChargesDifferByExchangeRate() {
        val nse = TradingEngine.stockCharges(100_000.0, Exchange.NSE, true)
        val bse = TradingEngine.stockCharges(100_000.0, Exchange.BSE, true)
        // Exchange txn diff 0.30 + 18% GST on that diff = 0.354, rounded to 0.35
        assertEquals(0.35, bse.total - nse.total, 0.02)
    }

    @Test
    fun cryptoChargesIncludeTdsOnlyOnSell() {
        val buy = TradingEngine.cryptoCharges(5000.0, true)
        val sell = TradingEngine.cryptoCharges(5000.0, false)
        assertEquals(12.50, buy.total, 0.01)
        assertEquals(62.50, sell.total, 0.01)
    }

    @Test
    fun oversellIsRejected() {
        assertNotNull(TradingEngine.validateSell(2.0, 999.0))
        assertNull(TradingEngine.validateSell(2.0, 1.0))
    }

    @Test
    fun insufficientCashIsRejected() {
        assertNotNull(TradingEngine.validateBuy(100.0, 1000.0, 10.0))
        assertNull(TradingEngine.validateBuy(10000.0, 1000.0, 10.0))
    }

    @Test
    fun averagePriceUpdatesOnSecondBuy() {
        assertEquals(1250.0, TradingEngine.newAveragePrice(2.0, 1200.0, 2.0, 1300.0), 0.01)
        assertEquals(1200.0, TradingEngine.newAveragePrice(0.0, 0.0, 5.0, 1200.0), 0.01)
    }

    @Test
    fun realisedPlNetOfSellCharges() {
        // Sell 1 @ 1300, avg 1200, charges 10 -> 90
        assertEquals(90.0, TradingEngine.realisedPl(1300.0, 1200.0, 1.0, 10.0), 0.01)
    }
}
