package com.example.bthotspot.bluetooth

import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.bthotspot.data.AutomationPreferences
import com.example.bthotspot.hotspot.HotspotController

/**
 * A [BroadcastReceiver] that listens for [BluetoothDevice.ACTION_ACL_CONNECTED] events
 * and triggers hotspot activation when the connecting device is the hardcoded target.
 *
 * This receiver is registered **dynamically** by [BluetoothMonitorService] (not in the
 * manifest) because ACTION_ACL_CONNECTED is not on Android's implicit broadcast exception
 * list and would not be delivered to a statically-declared receiver on API 26+.
 *
 * Responsibilities:
 * - Extract the [BluetoothDevice] from the intent extra and read its name.
 * - If the name equals [TARGET_DEVICE_NAME] AND automation is currently active,
 *   call [HotspotController.enableHotspotIfNeeded].
 * - Otherwise take no action.
 *
 * Requirements: 2.3, 2.4, 2.5
 */
class BluetoothConnectionReceiver(
    private val automationPreferences: AutomationPreferences,
    private val hotspotController: HotspotController,
) : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != BluetoothDevice.ACTION_ACL_CONNECTED) return

        val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
        }
        val deviceName = try {
            device?.name
        } catch (e: SecurityException) {
            // BLUETOOTH_CONNECT permission may not be granted; treat as unrecognised device.
            Log.w(TAG, "SecurityException reading device name", e)
            null
        }

        Log.d(TAG, "ACL_CONNECTED — device name: '$deviceName' (target: '$TARGET_DEVICE_NAME')")

        // Requirement 2.4: non-target device name → no action
        if (deviceName != TARGET_DEVICE_NAME) {
            Log.d(TAG, "Device name mismatch — ignoring")
            return
        }

        val automationEnabled = automationPreferences.isAutomationEnabled()
        Log.d(TAG, "Target device matched! automationEnabled=$automationEnabled")

        // Requirement 2.5: automation must be active
        if (!automationEnabled) return

        // Requirement 2.3: target device + automation active → enable hotspot
        Log.i(TAG, "Triggering hotspot enable")
        hotspotController.enableHotspotIfNeeded()
    }

    companion object {
        private const val TAG = "BtHotspot"

        /** The hardcoded Bluetooth device name that triggers hotspot activation. */
        const val TARGET_DEVICE_NAME = "CHEVROLET6572"
    }
}
