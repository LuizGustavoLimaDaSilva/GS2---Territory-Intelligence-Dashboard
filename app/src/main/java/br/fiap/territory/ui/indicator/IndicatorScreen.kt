package br.fiap.territory.ui.indicator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.fiap.territory.domain.format.PtBr
import br.fiap.territory.domain.model.IndicatorState
import br.fiap.territory.ui.UiState
import br.fiap.territory.ui.components.AppCard
import br.fiap.territory.ui.components.AppScaffold
import br.fiap.territory.ui.components.ScreenColumn
import br.fiap.territory.ui.components.SectionTitle
import br.fiap.territory.ui.components.StateKind
import br.fiap.territory.ui.components.StateView
import br.fiap.territory.ui.components.AlertCard
import br.fiap.territory.ui.components.KpiCard
import br.fiap.territory.ui.format.IndicatorTrendChart
import br.fiap.territory.ui.format.decimals
import br.fiap.territory.ui.format.icon
import br.fiap.territory.ui.format.metaLine
import br.fiap.territory.ui.format.toKind
import br.fiap.territory.ui.format.valueText
import br.fiap.territory.ui.format.kind
import br.fiap.territory.ui.format.label
import br.fiap.territory.ui.format.text
import br.fiap.territory.ui.theme.Spacing

@Composable
fun IndicatorRoute(
    onBack: () -> Unit,
    onOpenAlert: (String) -> Unit,
    viewModel: IndicatorViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    IndicatorScreen(state, onBack, onOpenAlert)
}

@Composable
fun IndicatorScreen(state: UiState<IndicatorDetail>, onBack: () -> Unit, onOpenAlert: (String) -> Unit) {
    val title = (state as? UiState.Success)?.data?.let { "${it.status.indicator.type.label} · ${it.territory.name}" } ?: "Indicador"
    AppScaffold(title = title, onBack = onBack) { padding ->
        ScreenColumn(padding) {
            when (state) {
                UiState.Loading -> item { StateView(StateKind.LOADING, "Carregando indicador…") }
                is UiState.Empty -> item { StateView(StateKind.EMPTY, state.message) }
                is UiState.Error -> item { StateView(StateKind.ERROR, state.message) }
                is UiState.Stale -> item { StateView(StateKind.STALE, state.message) }
                is UiState.Success -> indicatorContent(state.data, onOpenAlert)
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.indicatorContent(
    detail: IndicatorDetail,
    onOpenAlert: (String) -> Unit,
) {
    val status = detail.status
    val indicator = status.indicator
    val forecast = status.forecast
    val unit = indicator.unit
    val d = status.decimals()

    item {
        KpiCard(
            title = "${indicator.type.label} · ${detail.territory.name}",
            value = status.valueText(),
            unit = unit,
            statusLabel = status.label(),
            status = status.kind(),
            details = listOfNotNull(
                forecast.lastTimestamp?.let { "Última leitura: ${PtBr.dateTime(it)} (${PtBr.ago(it, detail.referenceNow)})." },
            ),
            leadingIcon = indicator.type.icon(),
        )
    }
    status.message?.let { message ->
        item {
            StateView(
                if (status.state == IndicatorState.STALE) StateKind.STALE else StateKind.EMPTY,
                message,
            )
        }
    }

    item {
        AppCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                SectionTitle("Histórico e previsão")
                if (forecast.history.size >= 2) {
                    IndicatorTrendChart(status, compact = false)
                } else {
                    Text(
                        "Sem pontos suficientes para desenhar o gráfico.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }

    item {
        AppCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                SectionTitle("Resumo da previsão")
                SummaryLine("Último valor diário", forecast.lastValue?.let { "${PtBr.number(it, d)} $unit" } ?: "—")
                if (forecast.status == br.fiap.territory.domain.model.ForecastStatus.OK) {
                    SummaryLine("Média móvel", "${PtBr.number(forecast.movingAverage ?: 0.0, d)} $unit")
                    SummaryLine("Tendência", "${forecast.trend.text()} (${PtBr.signed(forecast.slopePerDay ?: 0.0, d)} $unit/dia)")
                    SummaryLine("Projeção em ${forecast.horizonDays} dias", "${PtBr.number(forecast.projectedValue ?: 0.0, d)} $unit")
                    SummaryLine("Índice de confiança (heurístico)", "${PtBr.percent(forecast.confidence)} (${PtBr.confidenceLabel(forecast.confidence)})")
                    SummaryLine("Amostras", "${forecast.samples} de ${forecast.windowDays} dias")
                    SummaryLine(
                        "Temperatura prevista",
                        forecast.temperatureC?.let { "${PtBr.number(it, 1)} °C" } ?: "indisponível",
                    )
                }
                SummaryLine("Limiares", "atenção ${PtBr.number(indicator.warningThreshold, d)} / crítico ${PtBr.number(indicator.criticalThreshold, d)} $unit")
                SummaryLine("Fonte", indicator.sourceNote)
                SummaryLine("Origem da última leitura", status.origin?.label ?: "—")
                forecast.lastTimestamp?.let {
                    SummaryLine("Última leitura", "${PtBr.dateTime(it)} (${PtBr.ago(it, detail.referenceNow)})")
                }
            }
        }
    }

    detail.alert?.let { alert ->
        item { SectionTitle("Alerta deste indicador") }
        item {
            AlertCard(
                levelLabel = alert.level.label,
                level = alert.level.toKind(),
                title = alert.title,
                summary = alert.summary,
                metaLine = alert.metaLine(detail.referenceNow),
                onClick = { onOpenAlert(alert.id) },
            )
        }
    }

    item {
        Text(
            "Como funciona: a série diária (pico do dia na energia, média do dia na água) passa por uma média móvel de 3 dias e " +
                "uma regressão linear dos últimos 14 dias. A projeção é ajustada pela temperatura prevista e comparada aos limiares. " +
                "O índice de confiança é uma heurística (cobertura dos dados e ruído), não uma probabilidade de o evento ocorrer. " +
                "O app recomenda; a decisão é sempre humana.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SummaryLine(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
    }
}
