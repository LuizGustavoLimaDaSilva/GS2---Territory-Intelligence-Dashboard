package br.fiap.territory.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.fiap.territory.domain.model.Indicator
import br.fiap.territory.domain.model.IndicatorType
import br.fiap.territory.domain.model.NotificationPermission
import br.fiap.territory.domain.model.Territory
import br.fiap.territory.domain.notification.AlertNotifier
import br.fiap.territory.domain.repository.IndicatorRepository
import br.fiap.territory.domain.repository.PreferencesRepository
import br.fiap.territory.domain.repository.TerritoryRepository
import br.fiap.territory.domain.usecase.ClearDecisionsUseCase
import br.fiap.territory.ui.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsData(
    val territories: List<Territory>,
    val selectedId: String?,
    val demoMode: Boolean,
    val notificationsEnabled: Boolean,
    val indicators: List<Indicator>,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    territories: TerritoryRepository,
    indicators: IndicatorRepository,
    private val preferences: PreferencesRepository,
    private val clearDecisions: ClearDecisionsUseCase,
    notifier: AlertNotifier,
) : ViewModel() {

    val permission: StateFlow<NotificationPermission> = notifier.permission

    private val retryTick = MutableStateFlow(0)

    /** Falha de leitura vira estado de erro (com "tentar novamente"); lista vazia legítima vira Empty. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<UiState<SettingsData>> = retryTick
        .flatMapLatest {
            combine(
                territories.observeAll(),
                indicators.observeAll(),
                preferences.selectedTerritoryId,
                preferences.demoMode,
                preferences.notificationsEnabled,
            ) { territoryList, indicatorList, selected, demo, notifications ->
                if (territoryList.isEmpty()) {
                    UiState.Empty("Nenhum território disponível nos dados locais.")
                } else {
                    UiState.Success(
                        SettingsData(
                            territories = territoryList,
                            selectedId = selected?.takeIf { id -> territoryList.any { it.id == id } }
                                ?: territoryList.first().id,
                            demoMode = demo,
                            notificationsEnabled = notifications,
                            indicators = indicatorList.filter { it.type != IndicatorType.CLIMATE },
                        ),
                    )
                }
            }.catch { emit(UiState.Error("Não foi possível ler as configurações: ${it.message}")) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)

    fun retry() {
        retryTick.value += 1
    }

    fun selectTerritory(id: String) {
        viewModelScope.launch { preferences.setSelectedTerritory(id) }
    }

    fun setDemoMode(enabled: Boolean) {
        viewModelScope.launch { preferences.setDemoMode(enabled) }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch { preferences.setNotificationsEnabled(enabled) }
    }

    /** LGPD: o usuário pode apagar os próprios registros de decisão. */
    fun deleteDecisions() {
        viewModelScope.launch { clearDecisions() }
    }
}
