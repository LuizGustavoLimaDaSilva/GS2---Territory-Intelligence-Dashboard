package br.fiap.territory.domain.model

/** Como as leituras de um dia são resumidas em um único ponto da série diária. */
enum class Aggregation { DAILY_MAX, DAILY_MEAN }

enum class IndicatorType(
    val label: String,
    val aggregation: Aggregation,
    /** Leituras por dia na fonte; usado para limitar quantas linhas o banco devolve. */
    val samplesPerDay: Int,
    /** Aumento relativo do valor por °C acima do limiar de temperatura (regra explicável). */
    val temperatureSensitivity: Double,
) {
    ENERGY("Energia", Aggregation.DAILY_MAX, 24, 0.015),
    WATER("Água", Aggregation.DAILY_MEAN, 1, 0.010),
    CLIMATE("Clima", Aggregation.DAILY_MEAN, 1, 0.0),
}

enum class DataSource(val label: String) {
    PUBLICA("Pública"),
    SINTETICA("Sintética"),
    AUTORIZADA("Autorizada"),
}

enum class Trend { STABLE, INCREASING, DECREASING }

enum class ForecastStatus { OK, INSUFFICIENT, STALE, NO_DATA }

/** Estado de um indicador na tela (combina a previsão com a regra de confiança mínima). */
enum class IndicatorState { OK, LOW_CONFIDENCE, INSUFFICIENT, STALE, NO_DATA }

/** A ordem dos valores é a gravidade: INFORMATIVO < ATENCAO < CRITICO. */
enum class AlertLevel(val label: String) {
    INFORMATIVO("Informativo"),
    ATENCAO("Atenção"),
    CRITICO("Crítico"),
}

enum class DecisionAction(val label: String) {
    ACATAR("Acatar"),
    ADIAR("Adiar"),
    DESCARTAR("Descartar"),
}

enum class NotificationPermission { GRANTED, DENIED, DENIED_FOREVER }

data class Territory(val id: String, val name: String, val type: String)

data class Indicator(
    val type: IndicatorType,
    val unit: String,
    val warningThreshold: Double,
    val criticalThreshold: Double,
    val sourceNote: String,
)

data class Reading(
    val id: String,
    val territoryId: String,
    val indicator: IndicatorType,
    val timestamp: Long,
    val value: Double,
    val origin: DataSource,
)

data class JustificationItem(val label: String, val value: String)

data class AlertJustification(
    val variables: List<JustificationItem>,
    val rule: String,
    val period: String,
    val source: String,
)

data class Alert(
    val id: String,
    val territoryId: String,
    val territoryName: String,
    val indicator: IndicatorType,
    val level: AlertLevel,
    val title: String,
    val summary: String,
    /** 0.0 a 1.0 */
    val confidence: Double,
    val justification: AlertJustification,
    val forecastPeriod: String,
    val createdAt: Long,
    val validUntil: Long,
    val origin: DataSource,
)

data class Decision(
    val id: String,
    val alertId: String,
    val responsible: String,
    val action: DecisionAction,
    val note: String,
    val timestamp: Long,
)

/** Resultado da previsão para um indicador. Tudo o que a justificativa do alerta precisa está aqui. */
data class Forecast(
    val status: ForecastStatus,
    val reason: String? = null,
    val samples: Int = 0,
    val windowDays: Int = 0,
    val lastValue: Double? = null,
    val lastTimestamp: Long? = null,
    val movingAverage: Double? = null,
    val slopePerDay: Double? = null,
    val projectedBase: Double? = null,
    val temperatureC: Double? = null,
    val temperatureAdjustment: Double = 0.0,
    val projectedValue: Double? = null,
    val horizonDays: Int = 0,
    val trend: Trend = Trend.STABLE,
    val confidence: Double = 0.0,
    val history: List<Double> = emptyList(),
    val projection: List<Double> = emptyList(),
    val windowStartMillis: Long? = null,
)

data class IndicatorStatus(
    val indicator: Indicator,
    val forecast: Forecast,
    val state: IndicatorState,
    val level: AlertLevel?,
    val origin: DataSource?,
    val message: String?,
)

data class TerritoryEvaluation(
    val territory: Territory?,
    val statuses: List<IndicatorStatus>,
    val alerts: List<Alert>,
    val referenceNow: Long,
    val demoMode: Boolean,
    val currentTemperatureC: Double?,
    val forecastTemperatureC: Double?,
)

data class HistoryItem(
    val decision: Decision,
    val alertTitle: String?,
    val alertLevel: AlertLevel?,
)

data class AlertDetail(val alert: Alert, val decisions: List<Decision>)

sealed interface SeedStatus {
    data object Running : SeedStatus
    data object Done : SeedStatus
    data class Failed(val message: String) : SeedStatus
}
