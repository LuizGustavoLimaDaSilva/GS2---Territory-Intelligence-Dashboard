package br.fiap.territory.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TerritoryDao {
    @Query("SELECT * FROM territories ORDER BY name")
    fun observeAll(): Flow<List<TerritoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<TerritoryEntity>)

    @Query("DELETE FROM territories")
    suspend fun clear()
}

@Dao
interface IndicatorDao {
    @Query("SELECT * FROM indicators")
    fun observeAll(): Flow<List<IndicatorEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<IndicatorEntity>)

    @Query("DELETE FROM indicators")
    suspend fun clear()
}

@Dao
interface ReadingDao {
    /** As [limit] leituras mais recentes, devolvidas em ordem crescente de data. */
    @Query(
        "SELECT * FROM (SELECT * FROM readings WHERE territoryId = :territoryId AND indicator = :indicator " +
            "ORDER BY timestamp DESC LIMIT :limit) ORDER BY timestamp ASC",
    )
    fun observe(territoryId: String, indicator: String, limit: Int): Flow<List<ReadingEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(items: List<ReadingEntity>)

    @Query("SELECT COUNT(*) FROM readings")
    suspend fun count(): Int

    @Query("DELETE FROM readings")
    suspend fun clear()
}

@Dao
interface AlertDao {
    @Query("SELECT * FROM alerts ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<AlertEntity>>

    @Query("SELECT * FROM alerts WHERE id = :id")
    fun observeById(id: String): Flow<AlertEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<AlertEntity>)
}

@Dao
interface DecisionDao {
    @Query("SELECT * FROM decisions ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<DecisionEntity>>

    @Query("SELECT * FROM decisions WHERE alertId = :alertId ORDER BY timestamp DESC")
    fun observeForAlert(alertId: String): Flow<List<DecisionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: DecisionEntity)

    @Query("DELETE FROM decisions")
    suspend fun clear()
}

@Dao
interface NotifiedAlertDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun mark(item: NotifiedAlertEntity)

    @Query("SELECT COUNT(*) FROM notified_alerts WHERE alertId = :alertId")
    suspend fun count(alertId: String): Int
}
