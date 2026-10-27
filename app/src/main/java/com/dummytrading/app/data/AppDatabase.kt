package com.dummytrading.app.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Update
import com.dummytrading.app.trading.TradingEngine
import kotlinx.coroutines.flow.Flow

/** A holding is keyed by symbol + exchange, so the same company on NSE and
 *  BSE is deliberately TWO separate positions at two different prices. */
@Entity(tableName = "holdings", primaryKeys = ["symbol", "exchange"])
data class HoldingEntity(
    val symbol: String,
    val exchange: String, // NSE, BSE, CRYPTO
    val name: String,
    val assetType: String, // STOCK, CRYPTO
    val qty: Double,
    val avgPrice: Double
)

@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val symbol: String,
    val name: String,
    val exchange: String,
    val assetType: String,
    val side: String, // BUY, SELL
    val qty: Double,
    val price: Double,
    val gross: Double,
    val charges: Double,
    val net: Double,
    val realisedPl: Double,
    val note: String,
    val priceSource: String,
    val timestamp: Long
)

@Entity(tableName = "watchlist", primaryKeys = ["symbol", "exchange"])
data class WatchlistEntity(
    val symbol: String,
    val exchange: String,
    val name: String,
    val assetType: String
)

@Entity(tableName = "meta", primaryKeys = ["key"])
data class MetaEntity(val key: String, val value: String)

@Dao
interface TradingDao {
    @Query("SELECT * FROM holdings ORDER BY symbol")
    fun holdings(): Flow<List<HoldingEntity>>

    @Query("SELECT * FROM holdings WHERE symbol = :symbol AND exchange = :exchange LIMIT 1")
    suspend fun holding(symbol: String, exchange: String): HoldingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHolding(h: HoldingEntity)

    @Query("DELETE FROM holdings WHERE symbol = :symbol AND exchange = :exchange")
    suspend fun deleteHolding(symbol: String, exchange: String)

    @Query("SELECT * FROM orders ORDER BY timestamp DESC")
    fun orders(): Flow<List<OrderEntity>>

    @Insert
    suspend fun insertOrder(o: OrderEntity)

    @Query("SELECT * FROM watchlist ORDER BY symbol")
    fun watchlist(): Flow<List<WatchlistEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addWatchlist(w: WatchlistEntity)

    @Query("DELETE FROM watchlist WHERE symbol = :symbol AND exchange = :exchange")
    suspend fun removeWatchlist(symbol: String, exchange: String)

    @Query("SELECT * FROM meta WHERE `key` = :key LIMIT 1")
    suspend fun meta(key: String): MetaEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putMeta(m: MetaEntity)

    @Query("DELETE FROM holdings")
    suspend fun clearHoldings()

    @Query("DELETE FROM orders")
    suspend fun clearOrders()

    @Query("DELETE FROM watchlist")
    suspend fun clearWatchlist()
}

@Database(
    entities = [HoldingEntity::class, OrderEntity::class, WatchlistEntity::class, MetaEntity::class],
    version = 1, exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tradingDao(): TradingDao

    companion object {
        suspend fun cash(dao: TradingDao): Double =
            dao.meta("cash")?.value?.toDoubleOrNull() ?: TradingEngine.STARTING_CASH

        suspend fun setCash(dao: TradingDao, cash: Double) {
            dao.putMeta(MetaEntity("cash", cash.toString()))
        }
    }
}
