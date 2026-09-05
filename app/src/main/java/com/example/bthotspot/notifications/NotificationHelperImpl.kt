package com.example.bthotspot.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat

/**
 * Concrete implementation of [NotificationHelper].
 *
 * Two notification channels are created in the [init] block:
 * - [CHANNEL_SERVICE] (low importance) — persistent foreground-service status.
 * - [CHANNEL_ERRORS] (default importance) — one-shot error / Shizuku alerts.
 *
 * Satisfies Requirements 3.4, 5.1, 5.2.
 */
class NotificationHelperImpl(private val context: Context) : NotificationHelper {

    companion object {
        /** Channel id for the persistent foreground-service status notification. */
        const val CHANNEL_SERVICE = "service_status"

        /** Channel id for error and alert notifications. */
        const val CHANNEL_ERRORS = "errors"

        /** Stable notification id used for the foreground-service notification. */
        const val NOTIFICATION_ID = 1001
    }

    private val notificationManager: NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannels()
    }

    // -------------------------------------------------------------------------
    // Channel creation
    // -------------------------------------------------------------------------

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_SERVICE,
                "Service Status",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows whether Bluetooth monitoring is active."
            }

            val errorsChannel = NotificationChannel(
                CHANNEL_ERRORS,
                "Errors & Alerts",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Alerts when hotspot activation fails or Shizuku needs attention."
            }

            notificationManager.createNotificationChannels(
                listOf(serviceChannel, errorsChannel)
            )
        }
    }

    // -------------------------------------------------------------------------
    // NotificationHelper implementation
    // -------------------------------------------------------------------------

    /**
     * Builds the persistent foreground-service notification.
     *
     * Content title  → app name (from string resource, falling back to the
     *                  hard-coded value if the resource is unavailable).
     * Content text   → "Monitoring" when [isMonitoring] is true, "Stopped" otherwise.
     *
     * Requirement 5.1: the notification keeps the service in the foreground.
     * Requirement 5.2: displays app name and monitoring status.
     */
    override fun buildServiceNotification(isMonitoring: Boolean): Notification {
        val appName = runCatching {
            val resId = context.resources.getIdentifier("app_name", "string", context.packageName)
            if (resId != 0) context.getString(resId) else null
        }.getOrNull() ?: "Bluetooth Hotspot Enabler"

        val statusText = if (isMonitoring) "Monitoring" else "Stopped"

        return NotificationCompat.Builder(context, CHANNEL_SERVICE)
            .setContentTitle(appName)
            .setContentText(statusText)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    /**
     * Posts a one-shot notification on [CHANNEL_ERRORS].
     *
     * A timestamp-based id ensures each call creates a distinct notification
     * rather than replacing an existing one.
     *
     * Requirement 3.4: surfacing hotspot-activation failures to the user.
     */
    override fun postErrorNotification(title: String, body: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ERRORS)
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_ERROR)
            .build()

        // Use a timestamp-derived id so multiple errors are shown independently.
        val notificationId = (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
        notificationManager.notify(notificationId, notification)
    }
}
