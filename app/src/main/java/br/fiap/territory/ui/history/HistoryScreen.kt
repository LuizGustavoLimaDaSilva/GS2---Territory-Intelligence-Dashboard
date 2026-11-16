package br.fiap.territory.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.fiap.territory.domain.format.PtBr
import br.fiap.territory.domain.model.DecisionAction
import br.fiap.territory.domain.model.HistoryItem
import br.fiap.territory.ui.UiState
import br.fiap.territory.ui.components.AppCard
import br.fiap.territory.ui.components.AppScaffold
import br.fiap.territory.ui.components.ScreenColumn
import br.fiap.territory.ui.components.StateKind
import br.fiap.territory.ui.components.StateView
import br.fiap.territory.ui.components.StatusKind
import br.fiap.territory.ui.theme.Spacing

@Composable
fun HistoryRoute(onBack: () -> Unit, onOpenAlert: (String) -> Unit, viewModel: HistoryViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    HistoryScreen(state, onBack, onOpenAlert)
}

@Composable
fun HistoryScreen(state: UiState<List<HistoryItem>>, onBack: () -> Unit, onOpenAlert: (String) -> Unit) {
    AppScaffold(title = "Histórico de decisões", onBack = onBack) { padding ->
        ScreenColumn(padding) {
            when (state) {
                UiState.Loading -> item { StateView(StateKind.LOADING, "Carregando histórico…") }
                is UiState.Empty -> item { StateView(StateKind.EMPTY, state.message) }
                is UiState.Error -> item { StateView(StateKind.ERROR, state.message) }
                is UiState.Stale -> item { StateView(StateKind.STALE, state.message) }
                is UiState.Success -> items(state.data.size) { index ->
                    HistoryRow(state.data[index], onOpenAlert)
                }
            }
        }
    }
}

@Composable
private fun HistoryRow(item: HistoryItem, onOpenAlert: (String) -> Unit) {
    val decision = item.decision
    val kind = when (decision.action) {
        DecisionAction.ACATAR -> StatusKind.NORMAL
        DecisionAction.ADIAR -> StatusKind.ATTENTION
        DecisionAction.DESCARTAR -> StatusKind.NEUTRAL
    }
    AppCard(modifier = Modifier.fillMaxWidth(), onClick = { onOpenAlert(decision.alertId) }) {
        Column(
            modifier = Modifier.padding(Spacing.md).semantics(mergeDescendants = true) {},
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Icon(kind.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Text(decision.action.label, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            }
            Text(
                item.alertTitle ?: "Alerta não está mais disponível",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            item.alertLevel?.let {
                Text("Nível do alerta: ${it.label}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("Responsável: ${decision.responsible}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Registrada em ${PtBr.dateTime(decision.timestamp)}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (decision.note.isNotBlank()) {
                Text("Observação: ${decision.note}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}
