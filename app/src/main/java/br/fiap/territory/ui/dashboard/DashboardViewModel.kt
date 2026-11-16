package br.fiap.territory.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.fiap.territory.domain.format.PtBr
import br.fiap.territory.domain.model.IndicatorState
import br.fiap.territory.domain.model.NotificationPermission
import br.fiap.territory.domain.model.SeedStatus
import br.fiap.territory.domain.model.Territory
import br.fiap.territory.domain.model.TerritoryEvaluation
import br.fiap.territory.domain.notification.AlertNotifier
import br.fiap.territory.domain.repository.DataSeeder
import br.fiap.territory.domain.repository.PreferencesRepository
import br.fiap.territory.domain.repository.TerritoryRepository
import br.fiap.territory.domain.repository.TimeProvider
import br.fiap.territory.domain.usecase.NotifyCriticalAlertsUseCase
import br.fiap.territory.domain.usecase.ObserveTerritoryEvaluationUseCase
import br.fiap.territory.ui.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DashboardData(val territories: List<Territory>, val evaluation: TerritoryEvaluation)

private data class Inputs(
    val seed: SeedStatus,
    val territories: List<Territory>,
    val selectedId: String?,
    val demoMode: Boolean,
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val territories: TerritoryRepository,
    private val preferences: PreferencesRepository,
    private val seeder: DataSeeder,
    private val observeEvaluation: ObserveTerritoryEvaluationUseCase,
    private val notifyCritical: NotifyCriticalAlertsUseCase,
    notifier: AlertNotifier,
    private val time: TimeProvider,
) : ViewModel() {

    /** Muda a cada recarga (retry ou volta ao primeiro plano) e reinicia a coleta dos dados. */
    private val reloadTick = MutableStateFlow(0)
    private var lastRefreshAt = 0L
    private var retrying = false

    val permission: StateFlow<NotificationPermission> = notifier.permission

    val notificationsEnabled: StateFlow<Boolean> = preferences.notificationsEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    /**
     * Um único coletor ativo por vez: `flatMapLatest` cancela o anterior (sem carregamentos duplicados).
     * Qualquer falha da coleta vira `UiState.Error`; `retry()` reassina tudo a partir do `reloadTick`.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<UiState<DashboardData>> = reloadTick
        .flatMapLatest {
            combine(
                seeder.status,
                territories.observeAll(),
                preferences.selectedTerritoryId,
                preferences.demoMode,
            ) { seed, list, selected, demo -> Inputs(seed, list, selected, demo) }
                .flatMapLatest { stateFor(it) }
                .catch { emit(UiState.Error("Falha ao ler os dados locais: ${it.message}")) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)

    private fun stateFor(input: Inputs): Flow<UiState<DashboardData>> {
        val seed = input.seed
        return when {
            seed is SeedStatus.Running -> flowOf(UiState.Loading)
            seed is SeedStatus.Failed -> flowOf(UiState.Error(seed.message))
            input.territories.isEmpty() -> flowOf(UiState.Empty("Nenhum território encontrado nos dados locais."))
            else -> {
                val selected = input.territories.firstOrNull { it.id == input.selectedId } ?: input.territories.first()
                observeEvaluation(selected.id, input.demoMode)
                    .onEach { runCatching { notifyCritical(it.alerts) } }
                    .map { toState(input.territories, it) }
                    .catch { emit(UiState.Error("Falha ao ler os dados locais: ${it.message}")) }
            }
        }
    }

    private fun toState(territories: List<Territory>, evaluation: TerritoryEvaluation): UiState<DashboardData> {
        val data = DashboardData(territories, evaluation)
        val statuses = evaluation.statuses
        return when {
            evaluation.territory == null || statuses.isEmpty() || statuses.all { it.state == IndicatorState.NO_DATA } ->
                UiState.Empty("Sem leituras para este território.")
            statuses.all { it.state == IndicatorState.STALE } -> {
                val last = statuses.mapNotNull { it.forecast.lastTimestamp }.maxOrNull()
                UiState.Stale(
                    data,
                    "Dados desatualizados: a última leitura é de ${last?.let { PtBr.dateTime(it) } ?: "data desconhecida"}. " +
                        "Nenhum alerta é emitido com dados vencidos.",
                )
            }
            else -> UiState.Success(data)
        }
    }

    fun selectTerritory(id: String) {
        viewModelScope.launch { preferences.setSelectedTerritory(id) }
    }

    fun setDemoMode(enabled: Boolean) {
        viewModelScope.launch { preferences.setDemoMode(enabled) }
    }

    /**
     * Tentar novamente de verdade: se o carregamento inicial dos dados falhou, executa o seed outra vez;
     * em seguida reinicia a coleta. Um toque por vez (sem laços): enquanto roda, novos toques são ignorados.
     */
    fun retry() {
        if (retrying) return
        retrying = true
        viewModelScope.launch {
            try {
                if (seeder.status.value is SeedStatus.Failed) seeder.seedIfNeeded()
                reloadTick.value += 1
            } finally {
                retrying = false
            }
        }
    }

    /**
     * Reavalia os dados (validade e alertas dependem do relógio). Chamado ao voltar ao primeiro plano.
     * Limitado a uma vez por minuto para não gastar bateria à toa.
     */
    fun refresh() {
        val now = time.now()
        if (now - lastRefreshAt < MIN_REFRESH_INTERVAL_MILLIS) return
        lastRefreshAt = now
        reloadTick.value += 1
    }

    private companion object {
        const val MIN_REFRESH_INTERVAL_MILLIS = 60_000L
    }
}
