package com.example.bthotspot.service

import android.app.Service
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import com.example.bthotspot.bluetooth.BluetoothConnectionReceiver
import com.example.bthotspot.data.AutomationPreferencesImpl
import com.example.bthotspot.hotspot.HotspotControllerImpl
import com.example.bthotspot.notifications.NotificationHelperImpl

/**
 * Foreground service that monitors Bluetooth connection events and triggers hotspot
 * activation when the target device ("CHEVROLET6572") connects.
 *
 * Lifecycle:
 * - [onStartCommand]: promotes to foreground with a "Monitoring" notification, then
 *   dynamically registers [BluetoothConnectionReceiver] for [BluetoothDevice.ACTION_ACL_CONNECTED].
 * - [onDestroy]: unregisters the receiver and updates the notification to "Stopped".
 *
 * Returns [START_STICKY] so the system auto-restarts this service after a process kill
 * while the automation is active (Requirement 5.4).
 *
 * Requirements: 2.1, 2.2, 5.1, 5.2, 5.3, 5.4
 */
class BluetoothMonitorService : Service() {

    private lateinit var notificationHelper: NotificationHelperImpl
    private lateinit var bluetoothConnectionReceiver: BluetoothConnectionReceiver

    // -------------------------------------------------------------------------
    // Service lifecycle
    // -------------------------------------------------------------------------

    override fun onCreate() {
        super.onCreate()
        notificationHelper = NotificationHelperImpl(this)

        val automationPreferences = AutomationPreferencesImpl(this)
        val hotspotController = HotspotControllerImpl(this, notificationHelper)

        bluetoothConnectionReceiver = BluetoothConnectionReceiver(
            automationPreferences = automationPreferences,
            hotspotController = hotspotController,
        )
    }

    /**
     * Starts the foreground service and registers the Bluetooth receiver.
     *
     * Calling [startForeground] immediately upon entry satisfies Android's requirement
     * that a foreground service call startForeground() within 5 seconds of
     * [onStartCommand] being invoked (Requirement 5.1).
     *
     * [BluetoothDevice.ACTION_ACL_CONNECTED] is not in the implicit broadcast exception
     * list, so the receiver must be registered dynamically here (Requirement 2.1).
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Promote to foreground with a "Monitoring" notification (Requirements 5.1, 5.2).
        val notification = notificationHelper.buildServiceNotification(isMonitoring = true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NotificationHelperImpl.NOTIFICATION_ID,
                notification,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE,
            )
        } else {
            startForeground(NotificationHelperImpl.NOTIFICATION_ID, notification)
        }

        // Dynamically register the Bluetooth receiver (Requirement 2.1).
        val filter = IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(bluetoothConnectionReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(bluetoothConnectionReceiver, filter)
        }

        // START_STICKY ensures auto-restart if the process is killed (Requirement 5.4).
        return START_STICKY
    }

    /**
     * Unregisters the Bluetooth receiver and updates the notification to "Stopped"
     * so the user knows monitoring has ceased (Requirement 5.2).
     */
    override fun onDestroy() {
        super.onDestroy()

        // Unregister receiver — safe to call even if registration failed.
        try {
            unregisterReceiver(bluetoothConnectionReceiver)
        } catch (e: IllegalArgumentException) {
            // Receiver was never successfully registered; ignore.
        }

        // Update the persistent notification to reflect "Stopped" status (Requirement 5.2).
        val stoppedNotification = notificationHelper.buildServiceNotification(isMonitoring = false)
        val notificationManager =
            getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
        notificationManager.notify(NotificationHelperImpl.NOTIFICATION_ID, stoppedNotification)
    }

    /** This service does not support binding. */
    override fun onBind(intent: Intent?): IBinder? = null
}
