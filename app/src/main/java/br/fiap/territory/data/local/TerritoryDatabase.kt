package br.fiap.territory.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        TerritoryEntity::class,
        IndicatorEntity::class,
        ReadingEntity::class,
        AlertEntity::class,
        DecisionEntity::class,
        NotifiedAlertEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class TerritoryDatabase : RoomDatabase() {
    abstract fun territories(): TerritoryDao
    abstract fun indicators(): IndicatorDao
    abstract fun readings(): ReadingDao
    abstract fun alerts(): AlertDao
    abstract fun decisions(): DecisionDao
    abstract fun notified(): NotifiedAlertDao
}
