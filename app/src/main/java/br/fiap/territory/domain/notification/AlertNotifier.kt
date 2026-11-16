package br.fiap.territory.domain.notification

import br.fiap.territory.domain.model.NotificationPermission
import kotlinx.coroutines.flow.StateFlow

/**
 * Contrato do recurso nativo "notificação local". A tela nunca chama a API do Android:
 * ela observa [permission] e pede ações por este contrato.
 */
interface AlertNotifier {
    val permission: StateFlow<NotificationPermission>

    /** Recalcula o estado da permissão (chamar ao voltar para o app). */
    fun refresh()

    /** Chamado pela Activity com o resultado do diálogo de permissão do sistema. */
    fun onPermissionResult(granted: Boolean, canAskAgain: Boolean)

    /** Retorna true somente se a notificação foi de fato exibida. */
    fun notifyAlert(alertId: String, title: String, body: String): Boolean
}
