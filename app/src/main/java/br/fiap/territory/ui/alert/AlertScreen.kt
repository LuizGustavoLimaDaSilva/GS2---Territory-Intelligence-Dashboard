package br.fiap.territory.ui.alert

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.fiap.territory.domain.format.PtBr
import br.fiap.territory.domain.model.AlertLevel
import br.fiap.territory.domain.model.DecisionAction
import br.fiap.territory.ui.UiState
import br.fiap.territory.ui.components.AlertCard
import br.fiap.territory.ui.components.AppCard
import br.fiap.territory.ui.components.AppScaffold
import br.fiap.territory.ui.components.DecisionSheet
import br.fiap.territory.ui.components.JustificationPanel
import br.fiap.territory.ui.components.ScreenColumn
import br.fiap.territory.ui.components.SectionTitle
import br.fiap.territory.ui.components.StateKind
import br.fiap.territory.ui.components.StateView
import br.fiap.territory.ui.format.toKind
import br.fiap.territory.ui.theme.Spacing

@Composable
fun AlertRoute(
    onBack: () -> Unit,
    onOpenHistory: () -> Unit,
    viewModel: AlertViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    AlertScreen(
        state = state,
        onBack = onBack,
        onOpenHistory = onOpenHistory,
        onRegister = viewModel::register,
        onDismissSheet = viewModel::clearError,
    )
}

@Composable
fun AlertScreen(
    state: UiState<AlertScreenData>,
    onBack: () -> Unit,
    onOpenHistory: () -> Unit,
    onRegister: (DecisionAction, String, String, () -> Unit) -> Unit,
    onDismissSheet: () -> Unit,
) {
    var showSheet by rememberSaveable { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    AppScaffold(
        title = "Detalhe do alerta",
        onBack = onBack,
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        ScreenColumn(padding) {
            when (state) {
                UiState.Loading -> item { StateView(StateKind.LOADING, "Carregando alerta…") }
                is UiState.Empty -> item { StateView(StateKind.EMPTY, state.message) }
                is UiState.Error -> item { StateView(StateKind.ERROR, state.message) }
                is UiState.Stale -> item { StateView(StateKind.STALE, state.message) }
                is UiState.Success -> {
                    val alert = state.data.detail.alert
                    val decisions = state.data.detail.decisions
                    item {
                        AlertCard(
                            levelLabel = alert.level.label,
                            level = alert.level.toKind(),
                            title = alert.title,
                            summary = alert.summary,
                            metaLine = "Previsão: ${alert.forecastPeriod}",
                        )
                    }
                    item {
                        JustificationPanel(
                            variables = alert.justification.variables.map { it.label to it.value },
                            rule = alert.justification.rule,
                            period = alert.justification.period,
                            source = alert.justification.source,
                            confidence = alert.confidence.toFloat(),
                            confidenceText = "${PtBr.percent(alert.confidence)} (${PtBr.confidenceLabel(alert.confidence)}), heurístico: mede a cobertura e o ruído dos dados, não a probabilidade de o evento ocorrer",
                            updatedText = "gerado em ${PtBr.dateTime(alert.createdAt)}; válido até ${PtBr.dateTime(alert.validUntil)}",
                        )
                    }
                    item {
                        AppCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                                SectionTitle("Recomendação do app")
                                Text(
                                    recommendation(alert.level),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    "Decisões de alto impacto (como racionamento ou interrupção de serviço) nunca são executadas pelo app: " +
                                        "elas dependem de uma pessoa responsável.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                    decisions.firstOrNull()?.let { last ->
                        item {
                            StateView(
                                StateKind.EMPTY,
                                "Decisão registrada: ${last.action.label} por ${last.responsible} em ${PtBr.dateTime(last.timestamp)}" +
                                    (if (last.note.isNotBlank()) ". Observação: ${last.note}" else "."),
                            )
                        }
                    }
                    item {
                        Button(
                            onClick = { showSheet = true },
                            modifier = Modifier.fillMaxWidth().heightIn(min = Spacing.minTouch),
                        ) { Text(if (decisions.isEmpty()) "Registrar decisão" else "Registrar nova decisão") }
                    }
                    item {
                        OutlinedButton(
                            onClick = onOpenHistory,
                            modifier = Modifier.fillMaxWidth().heightIn(min = Spacing.minTouch),
                        ) { Text("Ver histórico de decisões") }
                    }
                }
            }
        }
        val success = state as? UiState.Success
        if (showSheet && success != null) {
            DecisionSheet(
                alertTitle = success.data.detail.alert.title,
                initialResponsible = success.data.lastResponsible,
                errorMessage = success.data.error,
                isSubmitting = success.data.submitting,
                onConfirm = { action, responsible, note ->
                    onRegister(action, responsible, note) {
                        showSheet = false
                        scope.launch { snackbar.showSnackbar("Decisão registrada com sucesso.") }
                    }
                },
                onDismiss = {
                    showSheet = false
                    onDismissSheet()
                },
            )
        }
    }
}

private fun recommendation(level: AlertLevel): String = when (level) {
    AlertLevel.CRITICO -> "Priorizar a análise agora: confirmar os dados em campo e avaliar medidas preventivas antes que o limiar crítico seja atingido."
    AlertLevel.ATENCAO -> "Reforçar o monitoramento e preparar medidas de contenção para os próximos dias."
    AlertLevel.INFORMATIVO -> "Acompanhar a tendência: o consumo está em alta, mas ainda abaixo do limiar de atenção."
}
