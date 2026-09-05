package com.example.bthotspot.boot

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.bthotspot.data.AutomationPreferences
import com.example.bthotspot.data.AutomationPreferencesImpl
import com.example.bthotspot.service.BluetoothMonitorService

/**
 * Manifest-declared [BroadcastReceiver] that listens for [Intent.ACTION_BOOT_COMPLETED].
 *
 * [Intent.ACTION_BOOT_COMPLETED] is exempt from Android's implicit broadcast restriction
 * (API 26+) and can therefore be declared in the manifest without requiring a running
 * foreground service.
 *
 * Responsibilities:
 * - Read [AutomationPreferencesImpl.isAutomationEnabled].
 * - If true, start [BluetoothMonitorService] via [Context.startForegroundService] so that
 *   Bluetooth monitoring resumes automatically after a device reboot.
 *
 * Note: Shizuku does not survive reboot on non-rooted devices. If Shizuku is not yet
 * re-activated when CHEVROLET6572 connects post-boot, [com.example.bthotspot.hotspot.HotspotControllerImpl]
 * will post a notification prompting the user to re-activate Shizuku.
 *
 * Requirements: 2.6, 5.5
 */
class BootReceiver(
    private val automationPreferences: AutomationPreferences? = null,
) : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val preferences = automationPreferences ?: AutomationPreferencesImpl(context)
        if (preferences.isAutomationEnabled()) {
            val serviceIntent = Intent(context, BluetoothMonitorService::class.java)
            context.startForegroundService(serviceIntent)
        }
    }
}
