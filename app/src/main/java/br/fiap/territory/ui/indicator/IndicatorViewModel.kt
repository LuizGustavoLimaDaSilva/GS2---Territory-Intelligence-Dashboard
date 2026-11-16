package br.fiap.territory.ui.indicator

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.fiap.territory.domain.model.Alert
import br.fiap.territory.domain.model.IndicatorStatus
import br.fiap.territory.domain.model.IndicatorType
import br.fiap.territory.domain.model.Territory
import br.fiap.territory.domain.repository.PreferencesRepository
import br.fiap.territory.domain.usecase.ObserveTerritoryEvaluationUseCase
import br.fiap.territory.ui.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class IndicatorDetail(
    val territory: Territory,
    val status: IndicatorStatus,
    val alert: Alert?,
    val referenceNow: Long,
)

/** O identificador do território e o tipo vêm na rota (nunca o objeto inteiro). */
@HiltViewModel
class IndicatorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    preferences: PreferencesRepository,
    observeEvaluation: ObserveTerritoryEvaluationUseCase,
) : ViewModel() {

    private val territoryId: String = checkNotNull(savedStateHandle.get<String>("territoryId"))
    private val type: IndicatorType = IndicatorType.valueOf(checkNotNull(savedStateHandle.get<String>("type")))

    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<UiState<IndicatorDetail>> = preferences.demoMode
        .flatMapLatest { demo -> observeEvaluation(territoryId, demo) }
        .map { evaluation ->
            val territory = evaluation.territory
            val status = evaluation.statuses.firstOrNull { it.indicator.type == type }
            if (territory == null || status == null) {
                UiState.Empty("Indicador não encontrado para este território.")
            } else {
                val detail = IndicatorDetail(territory, status, evaluation.alerts.firstOrNull { it.indicator == type }, evaluation.referenceNow)
                UiState.Success(detail)
            }
        }
        .catch { emit(UiState.Error("Falha ao ler os dados locais: ${it.message}")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)
}
