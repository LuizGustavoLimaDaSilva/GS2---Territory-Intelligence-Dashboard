package br.fiap.territory.domain.prediction

import br.fiap.territory.domain.model.Forecast
import br.fiap.territory.domain.model.ForecastStatus
import br.fiap.territory.domain.model.IndicatorType
import br.fiap.territory.domain.model.Reading
import br.fiap.territory.domain.model.Trend
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

data class PredictionConfig(
    val windowDays: Int = 14,
    val minSamples: Int = 7,
    val movingAverageDays: Int = 3,
    val horizonDays: Int = 2,
    val chartDays: Int = 21,
    val temperatureThresholdC: Double = 30.0,
    val minConfidence: Double = 0.5,
    val maxAgeMillis: Long = 7L * 24 * 60 * 60 * 1000,
    /** Variação diária relativa abaixo da qual a tendência é considerada estável. */
    val stableRelativeSlope: Double = 0.005,
    /** Ruído relativo (desvio dos resíduos / média) em que a confiança chega a zero. */
    val noiseForZeroConfidence: Double = 0.25,
)

/**
 * Motor de previsão explicável, em Kotlin puro:
 * 1) série diária (pico diário na energia, média diária na água);
 * 2) média móvel de 3 dias para suavizar o nível atual;
 * 3) regressão linear nos últimos 14 dias para estimar a tendência por dia;
 * 4) projeção = média móvel + tendência x horizonte;
 * 5) ajuste por temperatura prevista (regra de limiar);
 * 6) índice de confiança = cobertura da janela x (1 - ruído relativo / ruído máximo).
 *
 * LIMITES: o índice de confiança é HEURÍSTICO. Não é uma probabilidade estatística de o evento ocorrer
 * nem um intervalo de confiança. A regressão é linear, não modela sazonalidade semanal nem eventos raros,
 * e a regra de temperatura é uma aproximação linear (parâmetros em IndicatorType e PredictionConfig).
 */
class PredictionEngine @Inject constructor() {
    var config: PredictionConfig = PredictionConfig()
        private set

    constructor(config: PredictionConfig) : this() {
        this.config = config
    }

    fun forecast(
        type: IndicatorType,
        readings: List<Reading>,
        temperatureC: Double?,
        nowMillis: Long,
    ): Forecast {
        // Só observações até o horário de referência: leituras futuras (previsões) nunca entram no histórico.
        val usable = readings
            .filter { it.value.isFinite() && it.value >= 0.0 && it.timestamp <= nowMillis }
            .distinctBy { it.id }
        if (usable.isEmpty()) {
            val onlyFuture = readings.isNotEmpty() && readings.all { it.timestamp > nowMillis }
            return Forecast(
                ForecastStatus.NO_DATA,
                reason = if (onlyFuture) {
                    "Todas as leituras são posteriores ao horário de referência (são previsões, não observações)."
                } else {
                    "Nenhuma leitura válida disponível para este indicador."
                },
            )
        }
        val valid = usable

        val allDaily = DailyAggregator.aggregate(valid, type.aggregation)
        val daily = allDaily.takeLast(config.windowDays)
        val last = daily.last()
        val history = allDaily.takeLast(config.chartDays).map { it.value }

        if (nowMillis - last.lastTimestamp > config.maxAgeMillis) {
            return Forecast(
                status = ForecastStatus.STALE,
                reason = "Dados desatualizados: a última leitura é mais antiga que a validade de " +
                    "${config.maxAgeMillis / (24 * 60 * 60 * 1000)} dias.",
                samples = daily.size,
                windowDays = config.windowDays,
                lastValue = last.value,
                lastTimestamp = last.lastTimestamp,
                history = history,
            )
        }
        if (daily.size < config.minSamples) {
            return Forecast(
                status = ForecastStatus.INSUFFICIENT,
                reason = "Dados insuficientes: ${daily.size} dia(s) de leituras; são necessários ao menos ${config.minSamples}.",
                samples = daily.size,
                windowDays = config.windowDays,
                lastValue = last.value,
                lastTimestamp = last.lastTimestamp,
                history = history,
            )
        }

        val ys = daily.map { it.value }
        val n = ys.size
        val meanY = ys.average()
        val meanX = (n - 1) / 2.0
        var sxx = 0.0
        var sxy = 0.0
        ys.forEachIndexed { i, y ->
            sxx += (i - meanX) * (i - meanX)
            sxy += (i - meanX) * (y - meanY)
        }
        val slope = if (sxx == 0.0) 0.0 else sxy / sxx
        val intercept = meanY - slope * meanX
        val residualSquares = ys.indices.sumOf { i ->
            val r = ys[i] - (intercept + slope * i)
            r * r
        }
        val residualStd = sqrt(residualSquares / max(1, n - 2))

        val movingAverage = ys.takeLast(config.movingAverageDays).average()
        val base = movingAverage + slope * config.horizonDays
        val adjustment = if (temperatureC != null && temperatureC > config.temperatureThresholdC) {
            base * type.temperatureSensitivity * (temperatureC - config.temperatureThresholdC)
        } else {
            0.0
        }
        val projected = max(0.0, base + adjustment)

        if (!movingAverage.isFinite() || !slope.isFinite() || !projected.isFinite() || !residualStd.isFinite()) {
            return Forecast(
                status = ForecastStatus.INSUFFICIENT,
                reason = "Cálculo inválido para esta série (valores não finitos): nenhum alerta foi emitido.",
                samples = n,
                windowDays = config.windowDays,
                lastValue = last.value,
                lastTimestamp = last.lastTimestamp,
                history = history,
            )
        }

        val relativeSlope = slope / max(abs(meanY), 1e-9)
        val trend = when {
            abs(relativeSlope) < config.stableRelativeSlope -> Trend.STABLE
            relativeSlope > 0 -> Trend.INCREASING
            else -> Trend.DECREASING
        }

        val coverage = min(1.0, n.toDouble() / config.windowDays)
        val relativeNoise = residualStd / max(abs(meanY), 1e-9)
        val confidence = (coverage * (1.0 - min(1.0, relativeNoise / config.noiseForZeroConfidence)))
            .coerceIn(0.0, 1.0)

        val projection = (1..config.horizonDays).map { k ->
            movingAverage + (projected - movingAverage) * k / config.horizonDays
        }

        return Forecast(
            status = ForecastStatus.OK,
            samples = n,
            windowDays = config.windowDays,
            lastValue = last.value,
            lastTimestamp = last.lastTimestamp,
            movingAverage = movingAverage,
            slopePerDay = slope,
            projectedBase = base,
            temperatureC = temperatureC,
            temperatureAdjustment = adjustment,
            projectedValue = projected,
            horizonDays = config.horizonDays,
            trend = trend,
            confidence = confidence,
            history = history,
            projection = projection,
            windowStartMillis = daily.first().lastTimestamp,
        )
    }
}
