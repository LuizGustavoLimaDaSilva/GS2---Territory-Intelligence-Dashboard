package br.fiap.territory.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.fiap.territory.domain.model.HistoryItem
import br.fiap.territory.domain.usecase.ObserveHistoryUseCase
import br.fiap.territory.ui.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class HistoryViewModel @Inject constructor(observeHistory: ObserveHistoryUseCase) : ViewModel() {

    val state: StateFlow<UiState<List<HistoryItem>>> = observeHistory()
        .map<List<HistoryItem>, UiState<List<HistoryItem>>> { items ->
            if (items.isEmpty()) {
                UiState.Empty("Nenhuma decisão registrada ainda. Abra um alerta para registrar a primeira.")
            } else {
                UiState.Success(items)
            }
        }
        .catch { emit(UiState.Error("Falha ao ler o histórico: ${it.message}")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)
}
