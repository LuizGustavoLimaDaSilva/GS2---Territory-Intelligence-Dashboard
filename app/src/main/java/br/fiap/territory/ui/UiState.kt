package br.fiap.territory.ui

/** Estados de tela: carregando, sucesso, vazio, erro e dados desatualizados. */
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Empty(val message: String) : UiState<Nothing>
    data class Error(val message: String) : UiState<Nothing>
    data class Stale<T>(val data: T, val message: String) : UiState<T>
}
