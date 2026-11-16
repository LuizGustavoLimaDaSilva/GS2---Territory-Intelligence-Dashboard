package br.fiap.territory.data.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import br.fiap.territory.MainActivity
import br.fiap.territory.R
import br.fiap.territory.domain.model.NotificationPermission
import br.fiap.territory.domain.notification.AlertNotifier
import br.fiap.territory.domain.notification.NotificationPermissionResolver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Adaptador do recurso nativo "notificação local".
 * Três estados: concedida, negada (dá para pedir de novo) e negada para sempre
 * (só pelas configurações do sistema).
 */
class AndroidAlertNotifier(private val context: Context) : AlertNotifier {

    private var askedBefore = false
    private var canAskAgain = true

    private val _permission = MutableStateFlow(resolve())
    override val permission: StateFlow<NotificationPermission> = _permission.asStateFlow()

    init {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply { description = context.getString(R.string.notification_channel_description) }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    override fun refresh() {
        _permission.value = resolve()
    }

    override fun onPermissionResult(granted: Boolean, canAskAgain: Boolean) {
        askedBefore = true
        this.canAskAgain = canAskAgain
        refresh()
    }

    override fun notifyAlert(alertId: String, title: String, body: String): Boolean {
        refresh()
        if (_permission.value != NotificationPermission.GRANTED) return false
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(
            context,
            alertId.hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        return try {
            @Suppress("MissingPermission") // a permissão foi verificada acima
            NotificationManagerCompat.from(context).notify(alertId.hashCode(), notification)
            true
        } catch (e: SecurityException) {
            false
        }
    }

    private fun resolve(): NotificationPermission {
        val enabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
        val runtimeGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        return NotificationPermissionResolver.resolve(enabled, runtimeGranted, askedBefore, canAskAgain)
    }

    private companion object {
        const val CHANNEL_ID = "critical_alerts"
    }
}
