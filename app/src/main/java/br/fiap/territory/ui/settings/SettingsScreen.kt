package br.fiap.territory.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.fiap.territory.domain.format.PtBr
import br.fiap.territory.domain.model.IndicatorType
import br.fiap.territory.domain.model.NotificationPermission
import br.fiap.territory.ui.UiState
import br.fiap.territory.ui.components.AppCard
import br.fiap.territory.ui.components.AppScaffold
import br.fiap.territory.ui.components.ScreenColumn
import br.fiap.territory.ui.components.SectionTitle
import br.fiap.territory.ui.components.SelectorOption
import br.fiap.territory.ui.components.StateKind
import br.fiap.territory.ui.components.StateView
import br.fiap.territory.ui.components.TerritorySelector
import br.fiap.territory.ui.theme.Spacing

@Composable
fun SettingsRoute(
    onBack: () -> Unit,
    onRequestNotification: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val permission by viewModel.permission.collectAsStateWithLifecycle()
    SettingsScreen(
        state = state,
        permission = permission,
        onBack = onBack,
        onSelectTerritory = viewModel::selectTerritory,
        onDemoMode = viewModel::setDemoMode,
        onNotifications = viewModel::setNotificationsEnabled,
        onRequestNotification = onRequestNotification,
        onOpenNotificationSettings = onOpenNotificationSettings,
        onDeleteDecisions = viewModel::deleteDecisions,
        onRetry = viewModel::retry,
    )
}

@Composable
fun SettingsScreen(
    state: UiState<SettingsData>,
    permission: NotificationPermission,
    onBack: () -> Unit,
    onSelectTerritory: (String) -> Unit,
    onDemoMode: (Boolean) -> Unit,
    onNotifications: (Boolean) -> Unit,
    onRequestNotification: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onDeleteDecisions: () -> Unit,
    onRetry: () -> Unit,
) {
    var confirmDelete by remember { mutableStateOf(false) }

    AppScaffold(title = "Configurações", onBack = onBack) { padding ->
        ScreenColumn(padding) {
            when (state) {
                UiState.Loading -> item { StateView(StateKind.LOADING, "Carregando configurações…") }
                is UiState.Empty -> item { StateView(StateKind.EMPTY, state.message) }
                is UiState.Error -> item {
                    StateView(StateKind.ERROR, state.message, actionLabel = "Tentar novamente", onAction = onRetry)
                }
                is UiState.Stale -> item { StateView(StateKind.STALE, state.message) }
                is UiState.Success -> {
                    val data = state.data
                    item { SectionTitle("Território") }
                    item {
                        TerritorySelector(
                            options = data.territories.map { SelectorOption(it.id, it.name) },
                            selectedId = data.selectedId,
                            onSelect = onSelectTerritory,
                        )
                    }

                    item { SectionTitle("Modo demonstração") }
                    item {
                        SwitchCard(
                            title = "Usar a última leitura como data de referência",
                            description = "Os dados empacotados no app têm validade de 7 dias. Com este modo ligado, o app trata a última " +
                                "leitura como \"agora\" e continua gerando alertas para demonstração. Desligado, vale o relógio do aparelho.",
                            checked = data.demoMode,
                            onCheckedChange = onDemoMode,
                        )
                    }

                    item { SectionTitle("Notificações") }
                    item {
                        SwitchCard(
                            title = "Avisar alertas críticos",
                            description = "O app só avisa; nenhuma ação é executada automaticamente. Os avisos são gerados quando o app é aberto " +
                                "ou volta ao primeiro plano: não há monitoramento contínuo em segundo plano.",
                            checked = data.notificationsEnabled,
                            onCheckedChange = onNotifications,
                        )
                    }
                    item {
                        PermissionCard(permission, onRequestNotification, onOpenNotificationSettings)
                    }

                    item { SectionTitle("Limiares de alerta") }
                    items(data.indicators.size) { index ->
                        val indicator = data.indicators[index]
                        val d = if (indicator.type == IndicatorType.WATER) 1 else 2
                        InfoCard(
                            title = indicator.type.label,
                            body = "Atenção a partir de ${PtBr.number(indicator.warningThreshold, d)} ${indicator.unit}; " +
                                "crítico a partir de ${PtBr.number(indicator.criticalThreshold, d)} ${indicator.unit}. " +
                                "Definidos pelo curador de dados (administrador).",
                        )
                    }

                    item { SectionTitle("Fontes de dados") }
                    items(data.indicators.size) { index ->
                        val indicator = data.indicators[index]
                        InfoCard(title = indicator.type.label, body = indicator.sourceNote)
                    }
                    item {
                        InfoCard(
                            title = "Privacidade (LGPD)",
                            body = "O app usa apenas dados agregados, públicos ou sintéticos. Não coleta dados pessoais. " +
                                "Os registros de decisão ficam somente neste aparelho e você pode apagá-los abaixo.",
                        )
                    }
                    item {
                        OutlinedButton(
                            onClick = { confirmDelete = true },
                            modifier = Modifier.fillMaxWidth().heightIn(min = Spacing.minTouch),
                        ) { Text("Apagar histórico de decisões") }
                    }
                }
            }
        }
        if (confirmDelete) {
            AlertDialog(
                onDismissRequest = { confirmDelete = false },
                title = { Text("Apagar histórico?") },
                text = { Text("Todas as decisões registradas neste aparelho serão removidas. Esta ação não pode ser desfeita.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            confirmDelete = false
                            onDeleteDecisions()
                        },
                        modifier = Modifier.heightIn(min = Spacing.minTouch),
                    ) { Text("Apagar") }
                },
                dismissButton = {
                    TextButton(onClick = { confirmDelete = false }, modifier = Modifier.heightIn(min = Spacing.minTouch)) {
                        Text("Cancelar")
                    }
                },
            )
        }
    }
}

@Composable
private fun SwitchCard(title: String, description: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = Spacing.minTouch)
                .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
                .padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = checked, onCheckedChange = null)
        }
    }
}

@Composable
private fun PermissionCard(
    permission: NotificationPermission,
    onRequest: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Text("Permissão de notificações", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            when (permission) {
                NotificationPermission.GRANTED -> Text(
                    "Concedida: alertas críticos serão avisados por notificação.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                NotificationPermission.DENIED -> {
                    Text(
                        "Negada ou ainda não pedida. Precisamos dela só para avisar alertas críticos mesmo com o app fechado. " +
                            "Sem ela, o alerta continua visível no painel.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(onClick = onRequest, modifier = Modifier.heightIn(min = Spacing.minTouch)) { Text("Permitir notificações") }
                }
                NotificationPermission.DENIED_FOREVER -> {
                    Text(
                        "Bloqueada nas configurações do sistema. Para ativar, abra as configurações do app. " +
                            "Sem a permissão, o alerta continua visível no painel.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(onClick = onOpenSettings, modifier = Modifier.heightIn(min = Spacing.minTouch)) {
                        Text("Abrir configurações do sistema")
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoCard(title: String, body: String) {
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
