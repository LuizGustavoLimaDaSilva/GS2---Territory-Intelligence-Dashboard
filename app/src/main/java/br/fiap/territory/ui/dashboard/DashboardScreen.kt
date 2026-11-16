package br.fiap.territory.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.fiap.territory.domain.format.PtBr
import br.fiap.territory.domain.model.AlertLevel
import br.fiap.territory.domain.model.DataSource
import br.fiap.territory.domain.model.IndicatorStatus
import br.fiap.territory.domain.model.IndicatorType
import br.fiap.territory.domain.model.NotificationPermission
import br.fiap.territory.ui.UiState
import br.fiap.territory.ui.components.AlertCard
import br.fiap.territory.ui.components.AppScaffold
import br.fiap.territory.ui.components.KpiCard
import br.fiap.territory.ui.components.ScreenColumn
import br.fiap.territory.ui.components.SectionTitle
import br.fiap.territory.ui.components.SelectorOption
import br.fiap.territory.ui.components.StateKind
import br.fiap.territory.ui.components.StateView
import br.fiap.territory.ui.components.TerritorySelector
import br.fiap.territory.ui.format.IndicatorTrendChart
import br.fiap.territory.ui.format.details
import br.fiap.territory.ui.format.icon
import br.fiap.territory.ui.format.kind
import br.fiap.territory.ui.format.label
import br.fiap.territory.ui.format.metaLine
import br.fiap.territory.ui.format.toKind
import br.fiap.territory.ui.format.valueText
import br.fiap.territory.ui.theme.Spacing

@Composable
fun DashboardRoute(
    onOpenIndicator: (String, IndicatorType) -> Unit,
    onOpenAlert: (String) -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
    onRequestNotification: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val permission by viewModel.permission.collectAsStateWithLifecycle()
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsStateWithLifecycle()
    // Ao voltar ao primeiro plano, reavalia validade dos dados e alertas (no máximo 1 vez por minuto).
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }
    DashboardScreen(
        state = state,
        permission = permission,
        notificationsEnabled = notificationsEnabled,
        onSelectTerritory = viewModel::selectTerritory,
        onOpenIndicator = onOpenIndicator,
        onOpenAlert = onOpenAlert,
        onOpenHistory = onOpenHistory,
        onOpenSettings = onOpenSettings,
        onRetry = viewModel::retry,
        onEnableDemo = { viewModel.setDemoMode(true) },
        onRequestNotification = onRequestNotification,
        onOpenNotificationSettings = onOpenNotificationSettings,
    )
}

@Composable
fun DashboardScreen(
    state: UiState<DashboardData>,
    permission: NotificationPermission,
    notificationsEnabled: Boolean,
    onSelectTerritory: (String) -> Unit,
    onOpenIndicator: (String, IndicatorType) -> Unit,
    onOpenAlert: (String) -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
    onRetry: () -> Unit,
    onEnableDemo: () -> Unit,
    onRequestNotification: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
) {
    AppScaffold(
        title = "Painel territorial",
        actions = {
            IconButton(onClick = onOpenHistory) { Icon(Icons.Default.History, contentDescription = "Histórico de decisões") }
            IconButton(onClick = onOpenSettings) { Icon(Icons.Default.Settings, contentDescription = "Configurações") }
        },
    ) { padding ->
        when (state) {
            UiState.Loading -> ScreenColumn(padding) {
                item { StateView(StateKind.LOADING, "Carregando dados locais…") }
            }
            is UiState.Empty -> ScreenColumn(padding) {
                item { StateView(StateKind.EMPTY, state.message) }
            }
            is UiState.Error -> ScreenColumn(padding) {
                item { StateView(StateKind.ERROR, state.message, actionLabel = "Tentar novamente", onAction = onRetry) }
            }
            is UiState.Success -> DashboardContent(
                padding = padding,
                data = state.data,
                staleMessage = null,
                permission = permission,
                notificationsEnabled = notificationsEnabled,
                onSelectTerritory = onSelectTerritory,
                onOpenIndicator = onOpenIndicator,
                onOpenAlert = onOpenAlert,
                onEnableDemo = onEnableDemo,
                onRequestNotification = onRequestNotification,
                onOpenNotificationSettings = onOpenNotificationSettings,
            )
            is UiState.Stale -> DashboardContent(
                padding = padding,
                data = state.data,
                staleMessage = state.message,
                permission = permission,
                notificationsEnabled = notificationsEnabled,
                onSelectTerritory = onSelectTerritory,
                onOpenIndicator = onOpenIndicator,
                onOpenAlert = onOpenAlert,
                onEnableDemo = onEnableDemo,
                onRequestNotification = onRequestNotification,
                onOpenNotificationSettings = onOpenNotificationSettings,
            )
        }
    }
}

@Composable
private fun DashboardContent(
    padding: androidx.compose.foundation.layout.PaddingValues,
    data: DashboardData,
    staleMessage: String?,
    permission: NotificationPermission,
    notificationsEnabled: Boolean,
    onSelectTerritory: (String) -> Unit,
    onOpenIndicator: (String, IndicatorType) -> Unit,
    onOpenAlert: (String) -> Unit,
    onEnableDemo: () -> Unit,
    onRequestNotification: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
) {
    val evaluation = data.evaluation
    val territory = evaluation.territory
    val now = evaluation.referenceNow
    val hasSynthetic = evaluation.statuses.any { it.origin == DataSource.SINTETICA }
    val hasCritical = evaluation.alerts.any { it.level == AlertLevel.CRITICO }

    ScreenColumn(padding) {
        item {
            TerritorySelector(
                options = data.territories.map { SelectorOption(it.id, it.name) },
                selectedId = territory?.id,
                onSelect = onSelectTerritory,
            )
        }
        if (hasSynthetic) {
            item {
                StateView(
                    StateKind.EMPTY,
                    "Parte dos dados é sintética (fictícia), usada para demonstração. A origem de cada dado aparece nos cartões.",
                )
            }
        }
        if (evaluation.demoMode) {
            item {
                StateView(
                    StateKind.EMPTY,
                    "Modo demonstração: a data de referência é a última leitura (${PtBr.dateTime(now)}), não o relógio do aparelho.",
                )
            }
        }
        if (staleMessage != null) {
            item {
                StateView(
                    StateKind.STALE,
                    staleMessage,
                    actionLabel = if (evaluation.demoMode) null else "Usar modo demonstração",
                    onAction = if (evaluation.demoMode) null else onEnableDemo,
                )
            }
        }
        if (hasCritical && notificationsEnabled && permission != NotificationPermission.GRANTED) {
            item {
                NotificationPrompt(permission, onRequestNotification, onOpenNotificationSettings)
            }
        }

        item { SectionTitle("Alertas ativos") }
        if (evaluation.alerts.isEmpty()) {
            item {
                val reasons = evaluation.statuses.mapNotNull { it.message }
                StateView(
                    StateKind.EMPTY,
                    if (reasons.isEmpty()) "Nenhum alerta ativo: os indicadores estão dentro dos limiares."
                    else "Nenhum alerta emitido. " + reasons.joinToString(" "),
                )
            }
        } else {
            items(evaluation.alerts.size) { index ->
                val alert = evaluation.alerts[index]
                AlertCard(
                    levelLabel = alert.level.label,
                    level = alert.level.toKind(),
                    title = alert.title,
                    summary = alert.summary,
                    metaLine = alert.metaLine(now),
                    onClick = { onOpenAlert(alert.id) },
                )
            }
        }

        item { SectionTitle("Indicadores") }
        item {
            BoxWithConstraints {
                val wide = maxWidth >= 600.dp
                if (wide) {
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                        evaluation.statuses.forEach { status ->
                            IndicatorCard(status, now, territory?.id, onOpenIndicator, Modifier.weight(1f))
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                        evaluation.statuses.forEach { status ->
                            IndicatorCard(status, now, territory?.id, onOpenIndicator)
                        }
                    }
                }
            }
        }
        item {
            val forecastTemp = evaluation.forecastTemperatureC
            Text(
                text = if (forecastTemp != null) {
                    "Temperatura prevista (sintética): ${PtBr.number(forecastTemp, 1)} °C nos próximos dias."
                } else {
                    "Sem previsão de temperatura disponível: o ajuste por calor não foi aplicado."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun IndicatorCard(
    status: IndicatorStatus,
    now: Long,
    territoryId: String?,
    onOpenIndicator: (String, IndicatorType) -> Unit,
    modifier: Modifier = Modifier,
) {
    KpiCard(
        title = status.indicator.type.label,
        value = status.valueText(),
        unit = status.indicator.unit,
        statusLabel = status.label(),
        status = status.kind(),
        details = status.details(now),
        leadingIcon = status.indicator.type.icon(),
        onClick = territoryId?.let { id -> { onOpenIndicator(id, status.indicator.type) } },
        modifier = modifier,
        extraContent = if (status.forecast.history.size >= 2) {
            { IndicatorTrendChart(status, compact = true) }
        } else {
            null
        },
    )
}

@Composable
private fun NotificationPrompt(
    permission: NotificationPermission,
    onRequest: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm), modifier = Modifier.fillMaxWidth()) {
        StateView(
            StateKind.STALE,
            "Há um alerta crítico, mas as notificações estão desligadas. Ative para ser avisado mesmo fora do app. " +
                "Enquanto isso, o alerta aparece aqui no painel.",
        )
        if (permission == NotificationPermission.DENIED_FOREVER) {
            Button(onClick = onOpenSettings, modifier = Modifier.heightIn(min = Spacing.minTouch).padding(top = Spacing.xs)) {
                Text("Abrir configurações do sistema")
            }
        } else {
            Button(onClick = onRequest, modifier = Modifier.heightIn(min = Spacing.minTouch).padding(top = Spacing.xs)) {
                Text("Permitir notificações")
            }
        }
    }
}
