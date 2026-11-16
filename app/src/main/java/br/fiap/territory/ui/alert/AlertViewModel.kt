package br.fiap.territory.ui.alert

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.fiap.territory.domain.model.AlertDetail
import br.fiap.territory.domain.model.DecisionAction
import br.fiap.territory.domain.repository.PreferencesRepository
import br.fiap.territory.domain.usecase.ObserveAlertDetailUseCase
import br.fiap.territory.domain.usecase.RegisterDecisionUseCase
import br.fiap.territory.ui.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AlertScreenData(
    val detail: AlertDetail,
    val lastResponsible: String,
    val error: String?,
    val submitting: Boolean,
)

@HiltViewModel
class AlertViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeDetail: ObserveAlertDetailUseCase,
    preferences: PreferencesRepository,
    private val registerDecision: RegisterDecisionUseCase,
) : ViewModel() {

    private val alertId: String = checkNotNull(savedStateHandle.get<String>("alertId"))
    private val error = MutableStateFlow<String?>(null)
    private val submitting = MutableStateFlow(false)

    val state: StateFlow<UiState<AlertScreenData>> = combine(
        observeDetail(alertId),
        preferences.lastResponsible,
        error,
        submitting,
    ) { detail, responsible, err, busy ->
        if (detail == null) {
            UiState.Empty("Alerta não encontrado. Ele pode ter sido gerado com dados que não existem mais.")
        } else {
            UiState.Success(AlertScreenData(detail, responsible, err, busy))
        }
    }
        .catch { emit(UiState.Error("Falha ao ler o alerta: ${it.message}")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)

    /**
     * Valida e grava a decisão. Enquanto uma gravação está em andamento, novos envios são ignorados
     * (evita duplicidade por toque duplo). A tela só fecha o formulário em caso de sucesso.
     */
    fun register(action: DecisionAction, responsible: String, note: String, onSuccess: () -> Unit) {
        if (!submitting.compareAndSet(expect = false, update = true)) return
        viewModelScope.launch {
            try {
                registerDecision(alertId, action, responsible, note)
                    .onSuccess {
                        error.value = null
                        onSuccess()
                    }
                    .onFailure { error.value = it.message ?: "Não foi possível registrar a decisão." }
            } finally {
                submitting.value = false
            }
        }
    }

    fun clearError() {
        error.value = null
    }
}
