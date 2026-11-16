package br.fiap.territory.ui.format

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import br.fiap.territory.domain.format.PtBr
import br.fiap.territory.domain.model.IndicatorStatus
import br.fiap.territory.ui.components.ChartThreshold
import br.fiap.territory.ui.components.TrendChart
import br.fiap.territory.ui.components.criticalThresholdColor
import br.fiap.territory.ui.components.warningThresholdColor

/**
 * Adaptador entre o domínio e o `TrendChart`: monta séries, limiares, incerteza e descrição de acessibilidade.
 * Usado no Painel (modo compacto) e no Detalhe do Indicador (modo completo).
 */
@Composable
fun IndicatorTrendChart(status: IndicatorStatus, compact: Boolean, modifier: Modifier = Modifier) {
    val forecast = status.forecast
    if (forecast.history.size < 2) return
    val indicator = status.indicator
    val unit = indicator.unit
    val d = status.decimals()
    val history = forecast.history.map { it.toFloat() }
    val projection = forecast.projection.map { it.toFloat() }
    val description = "Gráfico de ${indicator.type.label}: ${history.size} dias, de " +
        "${PtBr.number(history.first().toDouble(), d)} a ${PtBr.number(history.last().toDouble(), d)} $unit" +
        (if (projection.isNotEmpty()) "; projeção de ${PtBr.number(projection.last().toDouble(), d)} $unit" else "") +
        "; limiar de atenção ${PtBr.number(indicator.warningThreshold, d)} e crítico ${PtBr.number(indicator.criticalThreshold, d)} $unit."
    TrendChart(
        history = history,
        forecast = projection,
        thresholds = listOf(
            ChartThreshold(indicator.warningThreshold.toFloat(), "limiar de atenção", warningThresholdColor()),
            ChartThreshold(indicator.criticalThreshold.toFloat(), "limiar crítico", criticalThresholdColor()),
        ),
        uncertainty = (0.05 + (1.0 - forecast.confidence) * 0.2).toFloat(),
        description = description,
        valueFormatter = { PtBr.number(it.toDouble(), d) },
        historyLabel = "${history.size} dias atrás",
        forecastLabel = if (projection.isNotEmpty()) "+${projection.size} dias (previsão)" else "hoje",
        modifier = modifier,
        compact = compact,
    )
}
