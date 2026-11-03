package bo.edu.ucb.encuentra.core.data

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import bo.edu.ucb.encuentra.core.domain.model.AppNotification
import bo.edu.ucb.encuentra.core.domain.model.NotificationType
import bo.edu.ucb.encuentra.core.domain.repository.LocalNotifier

/**
 * Canales de notificación (Android 8+), uno por [NotificationType], como `NotificationChannels` de ucbp1. El
 * usuario puede silenciar cada uno por separado en Ajustes (p. ej. dejar Reclamos y apagar Retención).
 */
object NotificationChannels {
    const val MATCHES = "coincidencias"
    const val CLAIMS = "reclamos"
    const val RETENTION = "retencion"
    const val GENERAL = "general"

    fun idFor(type: NotificationType): String = when (type) {
        NotificationType.NEW_MATCH -> MATCHES
        NotificationType.CLAIM_UPDATE -> CLAIMS
        NotificationType.RETENTION -> RETENTION
        NotificationType.GENERAL -> GENERAL
    }

    fun create(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        listOf(
            NotificationChannel(MATCHES, "Coincidencias", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Cuando aparece un objeto parecido a lo que reportaste."
            },
            NotificationChannel(CLAIMS, "Reclamos", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Cambios de estado de tus reclamos y fecha de entrega."
            },
            NotificationChannel(RETENTION, "Retención", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Objetos en custodia que están por vencer."
            },
            NotificationChannel(GENERAL, "General", NotificationManager.IMPORTANCE_DEFAULT)
        ).forEach(manager::createNotificationChannel)
    }
}

/**
 * Notificación local de Android (N01, el `actual` de [LocalNotifier] que entrega `platformModule()`).
 * Tocarla abre el deep link de F05 (`ucbencuentra://match/{id}`): MainActivity lo recibe por su intent-filter y la
 * navegación abre la pantalla correcta. Sin deep link, abre la app.
 */
class AndroidLocalNotifier(private val context: Context) : LocalNotifier {

    init {
        NotificationChannels.create(context)
    }

    override fun show(notification: AppNotification) {
        // Android 13+: sin el permiso POST_NOTIFICATIONS no se muestra (queda en la bandeja de la app).
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val intent = notification.deepLink
            ?.let { Intent(Intent.ACTION_VIEW, Uri.parse(it)).setPackage(context.packageName) }
            ?: context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?: return
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pending = PendingIntent.getActivity(
            context,
            notification.id.value.hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val built = NotificationCompat.Builder(context, NotificationChannels.idFor(notification.type))
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(notification.title)
            .setContentText(notification.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(notification.body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()
        // El mismo id de notificación reemplaza a la anterior (p. ej. una entrega reprogramada).
        runCatching { NotificationManagerCompat.from(context).notify(notification.id.value.hashCode(), built) }
    }
}
