package com.dummytrading.app.data

import com.dummytrading.app.trading.TradingEngine.AssetType
import com.dummytrading.app.trading.TradingEngine.Exchange

/**
 * Starter catalog (v1). Popular, dual-listed names first so the NSE/BSE
 * selector is meaningful from day one. Expand it by editing this file or,
 * later, by importing a CSV — see README.md "Expanding the catalog".
 * Yahoo symbols: NSE = SYMBOL.NS, BSE = SYMBOL.BO. Kraken pairs use the
 * pair names Kraken actually lists (e.g. XBTUSD for BTC, ETHUSD, POLUSD).
 */
data class CatalogItem(
    val symbol: String,          // e.g. RELIANCE, BTC
    val name: String,
    val assetType: AssetType,
    val yahooNse: String? = null,
    val yahooBse: String? = null,
    val krakenPair: String? = null
)

object Catalog {
    private fun stock(symbol: String, name: String) = CatalogItem(
        symbol = symbol, name = name, assetType = AssetType.STOCK,
        yahooNse = "$symbol.NS", yahooBse = "$symbol.BO"
    )

    val stocks: List<CatalogItem> = listOf(
        stock("RELIANCE", "Reliance Industries"),
        stock("TCS", "Tata Consultancy Services"),
        stock("HDFCBANK", "HDFC Bank"),
        stock("INFY", "Infosys"),
        stock("ICICIBANK", "ICICI Bank"),
        stock("SBIN", "State Bank of India"),
        stock("TATAMOTORS", "Tata Motors"),
        stock("TATASTEEL", "Tata Steel"),
        stock("LT", "Larsen & Toubro"),
        stock("AXISBANK", "Axis Bank"),
        stock("KOTAKBANK", "Kotak Mahindra Bank"),
        stock("ITC", "ITC"),
        stock("HINDUNILVR", "Hindustan Unilever"),
        stock("BHARTIARTL", "Bharti Airtel"),
        stock("ASIANPAINT", "Asian Paints"),
        stock("MARUTI", "Maruti Suzuki"),
        stock("SUNPHARMA", "Sun Pharmaceutical"),
        stock("TITAN", "Titan Company"),
        stock("ULTRACEMCO", "UltraTech Cement"),
        stock("NTPC", "NTPC"),
        stock("POWERGRID", "Power Grid"),
        stock("ONGC", "ONGC"),
        stock("COALINDIA", "Coal India"),
        stock("ADANIENT", "Adani Enterprises"),
        stock("ADANIPORTS", "Adani Ports"),
        stock("BAJFINANCE", "Bajaj Finance"),
        stock("BAJAJFINSV", "Bajaj Finserv"),
        stock("HCLTECH", "HCL Technologies"),
        stock("WIPRO", "Wipro"),
        stock("TECHM", "Tech Mahindra"),
        stock("JSWSTEEL", "JSW Steel"),
        stock("TATAPOWER", "Tata Power"),
        stock("TATAELXSI", "Tata Elxsi"),
        stock("DMART", "Avenue Supermarts"),
        stock("ZOMATO", "Zomato"),
        stock("PAYTM", "One 97 Communications"),
        stock("IRCTC", "IRCTC"),
        stock("BEL", "Bharat Electronics"),
        stock("HAL", "Hindustan Aeronautics"),
        stock("VEDL", "Vedanta"),
        stock("HINDALCO", "Hindalco"),
        stock("GRASIM", "Grasim Industries"),
        stock("DRREDDY", "Dr Reddy's Laboratories"),
        stock("CIPLA", "Cipla"),
        stock("DIVISLAB", "Divi's Laboratories"),
        stock("EICHERMOT", "Eicher Motors"),
        stock("HEROMOTOCO", "Hero MotoCorp"),
        stock("M&M", "Mahindra & Mahindra"),
        stock("BAJAJ-AUTO", "Bajaj Auto"),
        stock("INDUSINDBK", "IndusInd Bank"),
        stock("BANKBARODA", "Bank of Baroda"),
        stock("PNB", "Punjab National Bank"),
        stock("CANBK", "Canara Bank"),
        stock("BPCL", "Bharat Petroleum"),
        stock("IOC", "Indian Oil"),
        stock("GAIL", "GAIL India"),
        stock("DLF", "DLF"),
        stock("GODREJCP", "Godrej Consumer"),
        stock("DABUR", "Dabur India"),
        stock("BRITANNIA", "Britannia"),
        stock("NESTLEIND", "Nestle India"),
        stock("PIDILITIND", "Pidilite Industries"),
        stock("SRF", "SRF"),
        stock("APOLLOHOSP", "Apollo Hospitals"),
        stock("INDIGO", "InterGlobe Aviation"),
        stock("TATACONSUM", "Tata Consumer Products"),
        stock("TRENT", "Trent"),
        stock("SBILIFE", "SBI Life Insurance"),
        stock("HDFCLIFE", "HDFC Life"),
        stock("ICICIPRULI", "ICICI Prudential Life"),
        stock("SHREECEM", "Shree Cement"),
        stock("AMBUJACEM", "Ambuja Cements"),
        stock("SIEMENS", "Siemens"),
        stock("ABB", "ABB India"),
        stock("HAVELLS", "Havells India"),
        stock("VOLTAS", "Voltas"),
        stock("TVSMOTOR", "TVS Motor"),
        stock("ASHOKLEY", "Ashok Leyland"),
        stock("JINDALSTEL", "Jindal Steel & Power"),
        stock("SAIL", "SAIL"),
        stock("NMDC", "NMDC"),
        stock("RECLTD", "REC"),
        stock("PFC", "Power Finance Corp"),
        stock("IRFC", "Indian Railway Finance"),
        stock("RVNL", "Rail Vikas Nigam"),
        stock("MAZAGON", "Mazagon Dock"),
        stock("COCHINSHIP", "Cochin Shipyard"),
        stock("BSE", "BSE Ltd"),
        stock("CDSL", "Central Depository Services"),
        stock("ANGELONE", "Angel One"),
        stock("IEX", "Indian Energy Exchange"),
        stock("TATACHEM", "Tata Chemicals"),
        stock("UPL", "UPL"),
        stock("MUTHOOTFIN", "Muthoot Finance"),
        stock("MANAPPURAM", "Manappuram Finance"),
        stock("CHOLAFIN", "Cholamandalam Finance"),
        stock("PERSISTENT", "Persistent Systems"),
        stock("COFORGE", "Coforge"),
        stock("MPHASIS", "Mphasis"),
        stock("LTIM", "LTIMindtree"),
        stock("KPITTECH", "KPIT Technologies"),
        stock("TATAINVEST", "Tata Investment"),
    )

    val crypto: List<CatalogItem> = listOf(
        CatalogItem("BTC", "Bitcoin", AssetType.CRYPTO, krakenPair = "XBTUSD"),
        CatalogItem("ETH", "Ethereum", AssetType.CRYPTO, krakenPair = "ETHUSD"),
        CatalogItem("SOL", "Solana", AssetType.CRYPTO, krakenPair = "SOLUSD"),
        CatalogItem("XRP", "XRP", AssetType.CRYPTO, krakenPair = "XRPUSD"),
        CatalogItem("DOGE", "Dogecoin", AssetType.CRYPTO, krakenPair = "DOGEUSD"),
        CatalogItem("ADA", "Cardano", AssetType.CRYPTO, krakenPair = "ADAUSD"),
        CatalogItem("DOT", "Polkadot", AssetType.CRYPTO, krakenPair = "DOTUSD"),
        CatalogItem("LINK", "Chainlink", AssetType.CRYPTO, krakenPair = "LINKUSD"),
        CatalogItem("LTC", "Litecoin", AssetType.CRYPTO, krakenPair = "LTCUSD"),
        CatalogItem("AVAX", "Avalanche", AssetType.CRYPTO, krakenPair = "AVAXUSD"),
        CatalogItem("MATIC", "Polygon", AssetType.CRYPTO, krakenPair = "POLUSD"),
        CatalogItem("ARB", "Arbitrum", AssetType.CRYPTO, krakenPair = "ARBUSD"),
        CatalogItem("ATOM", "Cosmos", AssetType.CRYPTO, krakenPair = "ATOMUSD"),
        CatalogItem("NEAR", "NEAR Protocol", AssetType.CRYPTO, krakenPair = "NEARUSD"),
        CatalogItem("APT", "Aptos", AssetType.CRYPTO, krakenPair = "APTUSD"),
        CatalogItem("SUI", "Sui", AssetType.CRYPTO, krakenPair = "SUIUSD"),
    )

    val all: List<CatalogItem> = stocks + crypto

    fun search(query: String): List<CatalogItem> {
        val q = query.trim().uppercase()
        if (q.isEmpty()) return all
        return all.filter { it.symbol.contains(q) || it.name.uppercase().contains(q) }
    }

    fun yahooSymbol(item: CatalogItem, exchange: Exchange): String? = when (exchange) {
        Exchange.NSE -> item.yahooNse
        Exchange.BSE -> item.yahooBse
        Exchange.CRYPTO -> null
    }
}
