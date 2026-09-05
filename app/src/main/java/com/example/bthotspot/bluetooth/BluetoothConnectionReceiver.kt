package com.example.bthotspot.bluetooth

import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
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

        val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
        val deviceName = try {
            device?.name
        } catch (e: SecurityException) {
            // BLUETOOTH_CONNECT permission may not be granted; treat as unrecognised device.
            null
        }

        // Requirement 2.4: non-target device name → no action
        if (deviceName != TARGET_DEVICE_NAME) return

        // Requirement 2.5: automation must be active
        if (!automationPreferences.isAutomationEnabled()) return

        // Requirement 2.3: target device + automation active → enable hotspot
        hotspotController.enableHotspotIfNeeded()
    }

    companion object {
        /** The hardcoded Bluetooth device name that triggers hotspot activation. */
        const val TARGET_DEVICE_NAME = "CHEVROLET6572"
    }
}
