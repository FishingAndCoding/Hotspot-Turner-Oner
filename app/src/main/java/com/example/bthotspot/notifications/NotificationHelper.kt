package com.example.bthotspot.notifications

import android.app.Notification

interface NotificationHelper {
    /** Builds the foreground service persistent notification. */
    fun buildServiceNotification(isMonitoring: Boolean): Notification

    /** Posts a one-shot error/status notification. */
    fun postErrorNotification(title: String, body: String)
}
