package br.fiap.territory.domain.usecase

import br.fiap.territory.domain.format.PtBr
import br.fiap.territory.domain.model.Alert
import br.fiap.territory.domain.model.AlertJustification
import br.fiap.territory.domain.model.AlertLevel
import br.fiap.territory.domain.model.DataSource
import br.fiap.territory.domain.model.Forecast
import br.fiap.territory.domain.model.ForecastStatus
import br.fiap.territory.domain.model.Indicator
import br.fiap.territory.domain.model.IndicatorState
import br.fiap.territory.domain.model.IndicatorStatus
import br.fiap.territory.domain.model.IndicatorType
import br.fiap.territory.domain.model.JustificationItem
import br.fiap.territory.domain.model.Reading
import br.fiap.territory.domain.model.Territory
import br.fiap.territory.domain.model.TerritoryEvaluation
import br.fiap.territory.domain.model.Trend
import br.fiap.territory.domain.prediction.PredictionEngine
import java.time.Instant
import javax.inject.Inject
import kotlin.math.max

/**
 * Regra de negócio pura: transforma leituras em indicadores e alertas.
 * Não emite alerta quando faltam dados, os dados estão vencidos ou a confiança é baixa.
 */
class TerritoryEvaluator @Inject constructor(private val engine: PredictionEngine) {

    private val monitored = listOf(IndicatorType.ENERGY, IndicatorType.WATER)

    fun evaluate(
        territory: Territory?,
        indicators: List<Indicator>,
        readings: Map<IndicatorType, List<Reading>>,
        nowMillis: Long,
        demoMode: Boolean,
    ): TerritoryEvaluation {
        val config = engine.config
        val climate = readings[IndicatorType.CLIMATE].orEmpty()
        val horizonMillis = config.horizonDays * 24L * 60 * 60 * 1000
        val forecastTemp = climate
            .filter { it.timestamp > nowMillis && it.timestamp <= nowMillis + horizonMillis }
            .maxOfOrNull { it.value }
        val currentTemp = climate.filter { it.timestamp <= nowMillis }.maxByOrNull { it.timestamp }?.value

        val statuses = mutableListOf<IndicatorStatus>()
        val alerts = mutableListOf<Alert>()

        if (territory != null) {
            for (type in monitored) {
                val indicator = indicators.firstOrNull { it.type == type } ?: continue
                val rs = readings[type].orEmpty()
                val forecast = engine.forecast(type, rs, forecastTemp, nowMillis)
                val origin = rs.maxByOrNull { it.timestamp }?.origin
                val state = stateOf(forecast, config.minConfidence)
                val alert = if (state == IndicatorState.OK && origin != null) {
                    buildAlert(territory, indicator, forecast, origin, nowMillis)
                } else {
                    null
                }
                alert?.let { alerts += it }
                statuses += IndicatorStatus(
                    indicator = indicator,
                    forecast = forecast,
                    state = state,
                    level = alert?.level,
                    origin = origin,
                    message = messageOf(state, forecast, config.minConfidence),
                )
            }
        }

        return TerritoryEvaluation(
            territory = territory,
            statuses = statuses,
            alerts = alerts.sortedByDescending { it.level.ordinal },
            referenceNow = nowMillis,
            demoMode = demoMode,
            currentTemperatureC = currentTemp,
            forecastTemperatureC = forecastTemp,
        )
    }

    private fun stateOf(forecast: Forecast, minConfidence: Double): IndicatorState = when (forecast.status) {
        ForecastStatus.NO_DATA -> IndicatorState.NO_DATA
        ForecastStatus.STALE -> IndicatorState.STALE
        ForecastStatus.INSUFFICIENT -> IndicatorState.INSUFFICIENT
        ForecastStatus.OK ->
            if (forecast.confidence < minConfidence) IndicatorState.LOW_CONFIDENCE else IndicatorState.OK
    }

    private fun messageOf(state: IndicatorState, forecast: Forecast, minConfidence: Double): String? = when (state) {
        IndicatorState.OK -> null
        IndicatorState.LOW_CONFIDENCE ->
            "Índice de confiança baixo (${PtBr.percent(forecast.confidence)}; mínimo ${PtBr.percent(minConfidence)}): nenhum alerta foi emitido."
        else -> forecast.reason
    }

    private fun levelFor(forecast: Forecast, indicator: Indicator): AlertLevel? {
        val projected = forecast.projectedValue ?: return null
        val average = forecast.movingAverage ?: return null
        val peak = max(average, projected)
        return when {
            peak >= indicator.criticalThreshold -> AlertLevel.CRITICO
            peak >= indicator.warningThreshold -> AlertLevel.ATENCAO
            forecast.trend == Trend.INCREASING && peak >= indicator.warningThreshold * 0.9 -> AlertLevel.INFORMATIVO
            else -> null
        }
    }

    private fun buildAlert(
        territory: Territory,
        indicator: Indicator,
        forecast: Forecast,
        origin: DataSource,
        nowMillis: Long,
    ): Alert? {
        val level = levelFor(forecast, indicator) ?: return null
        val unit = indicator.unit
        val projected = forecast.projectedValue!!
        val average = forecast.movingAverage!!
        val slope = forecast.slopePerDay ?: 0.0
        val horizon = forecast.horizonDays
        val decimals = if (indicator.type == IndicatorType.WATER) 1 else 2
        val config = engine.config

        val subject = when (indicator.type) {
            IndicatorType.ENERGY -> "risco de sobrecarga de energia"
            IndicatorType.WATER -> "pressão sobre o abastecimento de água"
            IndicatorType.CLIMATE -> "variação climática"
        }
        val title = when (level) {
            AlertLevel.CRITICO -> "Crítico: $subject em ${territory.name}"
            AlertLevel.ATENCAO -> "Atenção: $subject em ${territory.name}"
            AlertLevel.INFORMATIVO -> "Informativo: consumo em alta em ${territory.name} (${indicator.type.label})"
        }
        val relativeToCritical = projected / indicator.criticalThreshold
        val summary = "Projeção de ${PtBr.number(projected, decimals)} $unit em $horizon dias " +
            "(${PtBr.percent(relativeToCritical)} do limiar crítico de ${PtBr.number(indicator.criticalThreshold, decimals)} $unit)."

        val temp = forecast.temperatureC
        val tempText = when {
            temp == null -> "sem previsão de temperatura disponível (ajuste não aplicado)"
            forecast.temperatureAdjustment > 0.0 ->
                "${PtBr.number(temp, 1)} °C acima de ${PtBr.number(config.temperatureThresholdC, 0)} °C: " +
                    "+${PtBr.number(forecast.temperatureAdjustment, decimals)} $unit"
            else -> "${PtBr.number(temp, 1)} °C (não passa de ${PtBr.number(config.temperatureThresholdC, 0)} °C: sem ajuste)"
        }
        val variables = listOf(
            JustificationItem("Última leitura diária", "${PtBr.number(forecast.lastValue ?: 0.0, decimals)} $unit"),
            JustificationItem("Média móvel (${config.movingAverageDays} dias)", "${PtBr.number(average, decimals)} $unit"),
            JustificationItem("Tendência (regressão linear)", "${PtBr.signed(slope, decimals)} $unit por dia"),
            JustificationItem("Projeção em $horizon dias", "${PtBr.number(projected, decimals)} $unit"),
            JustificationItem("Temperatura prevista", tempText),
            JustificationItem(
                "Limiares",
                "atenção ${PtBr.number(indicator.warningThreshold, decimals)} / crítico ${PtBr.number(indicator.criticalThreshold, decimals)} $unit",
            ),
            JustificationItem("Amostras", "${forecast.samples} de ${forecast.windowDays} dias"),
        )
        val rule = "Projeção = média móvel de ${config.movingAverageDays} dias + tendência (regressão linear dos últimos " +
            "${config.windowDays} dias) x $horizon dias, ajustada pela temperatura prevista. " +
            "Atenção: projeção ou média móvel >= limiar de atenção. Crítico: >= limiar crítico. " +
            "Informativo: tendência de alta com projeção >= 90% do limiar de atenção."

        val lastDate = PtBr.localDate(forecast.lastTimestamp ?: nowMillis)
        val windowStart = PtBr.localDate(forecast.windowStartMillis ?: nowMillis)
        val period = "${PtBr.shortDate(windowStart)} a ${PtBr.date(forecast.lastTimestamp ?: nowMillis)} (${forecast.samples} dias)"
        val forecastEnd = lastDate.plusDays(horizon.toLong())
        val forecastPeriod = "próximos $horizon dias (até ${PtBr.shortDate(forecastEnd)})"
        val dayKey = Instant.ofEpochMilli(nowMillis).atZone(PtBr.zone).toLocalDate().toString().replace("-", "")

        return Alert(
            id = "${indicator.type.name}_${territory.id}_${level.name}_$dayKey",
            territoryId = territory.id,
            territoryName = territory.name,
            indicator = indicator.type,
            level = level,
            title = title,
            summary = summary,
            confidence = forecast.confidence,
            justification = AlertJustification(
                variables = variables,
                rule = rule,
                period = period,
                source = "${indicator.sourceNote} (origem da última leitura: ${origin.label})",
            ),
            forecastPeriod = forecastPeriod,
            createdAt = nowMillis,
            validUntil = nowMillis + horizon * 24L * 60 * 60 * 1000,
            origin = origin,
        )
    }
}
