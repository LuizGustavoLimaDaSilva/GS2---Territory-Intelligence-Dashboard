package br.fiap.territory.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import br.fiap.territory.ui.theme.LocalStatusColors
import br.fiap.territory.ui.theme.Spacing

data class ChartThreshold(val value: Float, val label: String, val color: Color)

/**
 * Gráfico de série temporal: histórico (linha cheia), previsão (tracejada) com faixa de incerteza
 * e linhas de limiar. Recebe só números e textos; não conhece domínio nem Android.
 */
@Composable
fun TrendChart(
    history: List<Float>,
    forecast: List<Float>,
    thresholds: List<ChartThreshold>,
    uncertainty: Float,
    description: String,
    valueFormatter: (Float) -> String,
    historyLabel: String,
    forecastLabel: String,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val lineColor = MaterialTheme.colorScheme.primary
    val forecastColor = MaterialTheme.colorScheme.tertiary
    val gridColor = MaterialTheme.colorScheme.outlineVariant

    val allValues = history + forecast + thresholds.map { it.value }
    val rawMax = allValues.maxOrNull() ?: 1f
    val rawMin = allValues.minOrNull() ?: 0f
    val span = (rawMax - rawMin).takeIf { it > 0f } ?: 1f
    val maxV = rawMax + span * 0.08f
    val minV = rawMin - span * 0.08f

    Column(modifier = modifier.fillMaxWidth().semantics { contentDescription = description }) {
        Row(modifier = Modifier.fillMaxWidth().height(if (compact) 88.dp else 180.dp)) {
            if (!compact) {
                Column(
                    modifier = Modifier.fillMaxHeight().width(52.dp).padding(end = Spacing.xs),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(valueFormatter(maxV), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(valueFormatter(minV), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Canvas(modifier = Modifier.fillMaxHeight().weight(1f)) {
                val w = size.width
                val h = size.height
                val total = (history.size + forecast.size).coerceAtLeast(2)
                val stepX = w / (total - 1)
                fun x(i: Int) = i * stepX
                fun y(v: Float) = h - (v - minV) / (maxV - minV) * h

                // grade horizontal
                for (k in 0..3) {
                    val gy = h * k / 3f
                    drawLine(gridColor, Offset(0f, gy), Offset(w, gy), strokeWidth = 1.dp.toPx())
                }
                // limiares
                thresholds.forEach { t ->
                    drawLine(
                        color = t.color,
                        start = Offset(0f, y(t.value)),
                        end = Offset(w, y(t.value)),
                        strokeWidth = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10.dp.toPx(), 6.dp.toPx())),
                    )
                }
                // faixa de incerteza da previsão (cresce ao longe)
                if (history.isNotEmpty() && forecast.isNotEmpty()) {
                    val startIndex = history.size - 1
                    val start = history.last()
                    val upper = Path().apply { moveTo(x(startIndex), y(start)) }
                    forecast.forEachIndexed { k, v ->
                        val growth = uncertainty * (k + 1) / forecast.size
                        upper.lineTo(x(startIndex + k + 1), y(v * (1f + growth)))
                    }
                    for (k in forecast.indices.reversed()) {
                        val growth = uncertainty * (k + 1) / forecast.size
                        upper.lineTo(x(startIndex + k + 1), y(forecast[k] * (1f - growth)))
                    }
                    upper.close()
                    drawPath(upper, forecastColor.copy(alpha = 0.18f))
                }
                // histórico
                if (history.size >= 2) {
                    val path = Path().apply {
                        moveTo(x(0), y(history[0]))
                        for (i in 1 until history.size) lineTo(x(i), y(history[i]))
                    }
                    drawPath(path, lineColor, style = Stroke(width = 3.dp.toPx()))
                }
                // previsão
                if (history.isNotEmpty() && forecast.isNotEmpty()) {
                    val path = Path().apply {
                        moveTo(x(history.size - 1), y(history.last()))
                        forecast.forEachIndexed { k, v -> lineTo(x(history.size + k), y(v)) }
                    }
                    drawPath(
                        path,
                        forecastColor,
                        style = Stroke(width = 3.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(12.dp.toPx(), 8.dp.toPx()))),
                    )
                }
            }
        }
        if (compact) return@Column
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 52.dp, top = Spacing.xs),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(historyLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(forecastLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column(modifier = Modifier.padding(top = Spacing.sm), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            LegendRow("Linha cheia azul: histórico diário", lineColor)
            LegendRow("Linha tracejada com faixa: previsão e incerteza", forecastColor)
            thresholds.forEach { LegendRow("Tracejado: ${it.label} (${valueFormatter(it.value)})", it.color) }
        }
    }
}

@Composable
private fun LegendRow(text: String, color: Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Canvas(modifier = Modifier.width(24.dp).height(12.dp)) {
            drawLine(color, Offset(0f, size.height / 2), Offset(size.width, size.height / 2), strokeWidth = 3.dp.toPx())
        }
        Text(text, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Cores padrão de limiar vindas do tema semântico. */
@Composable
fun warningThresholdColor(): Color = LocalStatusColors.current.attention.content

@Composable
fun criticalThresholdColor(): Color = LocalStatusColors.current.critical.content
