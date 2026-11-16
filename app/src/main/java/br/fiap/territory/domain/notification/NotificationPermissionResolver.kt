package br.fiap.territory.domain.notification

import br.fiap.territory.domain.model.NotificationPermission

/**
 * Regra pura que decide o estado da permissão de notificações (testável sem Android).
 *
 * @param notificationsEnabled notificações do app ligadas no sistema (areNotificationsEnabled)
 * @param runtimePermissionGranted permissão POST_NOTIFICATIONS concedida (sempre true antes do Android 13)
 * @param askedBefore o app já mostrou o diálogo do sistema nesta execução
 * @param canAskAgain o sistema ainda mostra o diálogo (shouldShowRequestPermissionRationale)
 */
object NotificationPermissionResolver {
    fun resolve(
        notificationsEnabled: Boolean,
        runtimePermissionGranted: Boolean,
        askedBefore: Boolean,
        canAskAgain: Boolean,
    ): NotificationPermission = when {
        notificationsEnabled && runtimePermissionGranted -> NotificationPermission.GRANTED
        // Permissão concedida (ou Android < 13), mas as notificações estão desligadas nas configurações do sistema.
        runtimePermissionGranted -> NotificationPermission.DENIED_FOREVER
        askedBefore && !canAskAgain -> NotificationPermission.DENIED_FOREVER
        else -> NotificationPermission.DENIED
    }
}
