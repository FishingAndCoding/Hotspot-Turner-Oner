package com.example.bthotspot.bluetooth

import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import com.example.bthotspot.data.AutomationPreferences
import com.example.bthotspot.hotspot.HotspotController
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FreeSpec
import io.kotest.property.Arb
import io.kotest.property.arbitrary.filter
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

/**
 * Property-based tests for [BluetoothConnectionReceiver].
 *
 * **Validates: Requirements 2.4**
 */
@Tags(
    "Feature: bluetooth-hotspot-enabler",
    "Property 2: Non-target device name triggers no action",
)
class BluetoothConnectionReceiverPropertyTest : FreeSpec({

    /**
     * Property 2: Non-target device name triggers no action
     *
     * For any Bluetooth device name that is NOT equal to "CHEVROLET6572",
     * with automation active, calling [BluetoothConnectionReceiver.onReceive]
     * must never invoke [HotspotController.enableHotspotIfNeeded].
     *
     * **Validates: Requirements 2.4**
     */
    "Property 2 - Non-target device name triggers no action" {
        checkAll(
            iterations = 100,
            Arb.string().filter { it !in BluetoothConnectionReceiver.TARGET_DEVICE_NAMES },
        ) { nonTargetName ->
            // --- arrange ---
            val automationPreferences = mockk<AutomationPreferences>()
            val hotspotController = mockk<HotspotController>(relaxed = true)
            val context = mockk<Context>()
            val intent = mockk<Intent>()
            val bluetoothDevice = mockk<BluetoothDevice>()

            // Automation is active
            every { automationPreferences.isAutomationEnabled() } returns true

            // Intent carries the correct action
            every { intent.action } returns BluetoothDevice.ACTION_ACL_CONNECTED

            // Intent extra returns a BluetoothDevice whose name is the non-target value
            every {
                intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
            } returns bluetoothDevice
            every { bluetoothDevice.name } returns nonTargetName

            val receiver = BluetoothConnectionReceiver(
                automationPreferences = automationPreferences,
                hotspotController = hotspotController,
            )

            // --- act ---
            receiver.onReceive(context, intent)

            // --- assert ---
            // enableHotspotIfNeeded() must NOT be called for any non-target device name
            verify(exactly = 0) { hotspotController.enableHotspotIfNeeded() }
        }
    }
})
