package br.fiap.territory.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "territories")
data class TerritoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val type: String,
)

@Entity(tableName = "indicators")
data class IndicatorEntity(
    @PrimaryKey val type: String,
    val unit: String,
    val warningThreshold: Double,
    val criticalThreshold: Double,
    val sourceNote: String,
)

@Entity(
    tableName = "readings",
    indices = [Index(value = ["territoryId", "indicator", "timestamp"])],
)
data class ReadingEntity(
    @PrimaryKey val id: String,
    val territoryId: String,
    val indicator: String,
    val timestamp: Long,
    val value: Double,
    val origin: String,
)

@Entity(tableName = "alerts")
data class AlertEntity(
    @PrimaryKey val id: String,
    val territoryId: String,
    val territoryName: String,
    val indicator: String,
    val level: String,
    val title: String,
    val summary: String,
    val confidence: Double,
    val variablesText: String,
    val rule: String,
    val period: String,
    val source: String,
    val forecastPeriod: String,
    val createdAt: Long,
    val validUntil: Long,
    val origin: String,
)

@Entity(tableName = "decisions", indices = [Index("alertId")])
data class DecisionEntity(
    @PrimaryKey val id: String,
    val alertId: String,
    val responsible: String,
    val action: String,
    val note: String,
    val timestamp: Long,
)

/** Registro de alertas já notificados (evita notificação duplicada, mesmo após reiniciar o app). */
@Entity(tableName = "notified_alerts")
data class NotifiedAlertEntity(@PrimaryKey val alertId: String)
