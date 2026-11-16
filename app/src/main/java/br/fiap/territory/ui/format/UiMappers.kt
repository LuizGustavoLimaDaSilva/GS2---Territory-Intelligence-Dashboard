package br.fiap.territory.ui.format

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.ui.graphics.vector.ImageVector
import br.fiap.territory.domain.format.PtBr
import br.fiap.territory.domain.model.Alert
import br.fiap.territory.domain.model.AlertLevel
import br.fiap.territory.domain.model.IndicatorState
import br.fiap.territory.domain.model.IndicatorStatus
import br.fiap.territory.domain.model.IndicatorType
import br.fiap.territory.domain.model.Trend
import br.fiap.territory.ui.components.StatusKind

fun AlertLevel?.toKind(): StatusKind = when (this) {
    null -> StatusKind.NORMAL
    AlertLevel.INFORMATIVO -> StatusKind.INFO
    AlertLevel.ATENCAO -> StatusKind.ATTENTION
    AlertLevel.CRITICO -> StatusKind.CRITICAL
}

fun IndicatorType.icon(): ImageVector = when (this) {
    IndicatorType.ENERGY -> Icons.Default.Bolt
    IndicatorType.WATER -> Icons.Default.WaterDrop
    IndicatorType.CLIMATE -> Icons.Default.Thermostat
}

fun IndicatorStatus.decimals(): Int = if (indicator.type == IndicatorType.WATER) 1 else 2

fun IndicatorStatus.kind(): StatusKind =
    if (state == IndicatorState.OK) level.toKind() else StatusKind.NEUTRAL

fun IndicatorStatus.label(): String = when (state) {
    IndicatorState.OK -> level?.label ?: "Normal"
    IndicatorState.LOW_CONFIDENCE -> "Confiança baixa"
    IndicatorState.INSUFFICIENT -> "Dados insuficientes"
    IndicatorState.STALE -> "Desatualizado"
    IndicatorState.NO_DATA -> "Sem dados"
}

fun IndicatorStatus.valueText(): String =
    forecast.lastValue?.let { PtBr.number(it, decimals()) } ?: "—"

fun Trend.text(): String = when (this) {
    Trend.STABLE -> "estável"
    Trend.INCREASING -> "em alta"
    Trend.DECREASING -> "em queda"
}

/** Linhas de detalhe do cartão do indicador: previsão (se houver), fonte e data da última leitura. */
fun IndicatorStatus.details(referenceNow: Long): List<String> {
    val unit = indicator.unit
    val d = decimals()
    val lines = mutableListOf<String>()
    val f = forecast
    if (state == IndicatorState.OK || state == IndicatorState.LOW_CONFIDENCE) {
        lines += "Tendência ${f.trend.text()} (${PtBr.signed(f.slopePerDay ?: 0.0, d)} $unit/dia); média móvel ${PtBr.number(f.movingAverage ?: 0.0, d)} $unit."
        lines += "Projeção em ${f.horizonDays} dias: ${PtBr.number(f.projectedValue ?: 0.0, d)} $unit (índice de confiança ${PtBr.percent(f.confidence)})."
    }
    message?.let { lines += it }
    lines += "Fonte: ${indicator.sourceNote}."
    f.lastTimestamp?.let { lines += "Última leitura: ${PtBr.dateTime(it)} (${PtBr.ago(it, referenceNow)})." }
    return lines
}

fun Alert.metaLine(referenceNow: Long): String =
    "Índice de confiança ${PtBr.percent(confidence)} (${PtBr.confidenceLabel(confidence)}) • " +
        "gerado ${PtBr.ago(createdAt, referenceNow)} • origem: ${origin.label}"
