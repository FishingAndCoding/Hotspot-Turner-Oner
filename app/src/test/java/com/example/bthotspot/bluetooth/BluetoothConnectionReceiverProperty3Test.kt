package com.example.bthotspot.bluetooth

import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import com.example.bthotspot.data.AutomationPreferences
import com.example.bthotspot.hotspot.HotspotController
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FreeSpec
import io.kotest.property.Arb
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

/**
 * Property-based test for [BluetoothConnectionReceiver] — Property 3.
 *
 * With automation **inactive**, [HotspotController.enableHotspotIfNeeded] must
 * never be called regardless of the connecting device name — including the
 * target name "CHEVROLET6572".
 *
 * **Validates: Requirements 2.5**
 */
@Tags("Feature: bluetooth-hotspot-enabler", "Property 3: Inactive automation suppresses all events")
class BluetoothConnectionReceiverProperty3Test : FreeSpec({

    /**
     * Property 3: Inactive automation suppresses all events
     *
     * For any Bluetooth device name (including "CHEVROLET6572"), when automation is
     * inactive, [HotspotController.enableHotspotIfNeeded] must not be called.
     *
     * **Validates: Requirements 2.5**
     */
    "Property 3 - Inactive automation suppresses all events" {
        checkAll(
            iterations = 100,
            Arb.string()
        ) { deviceName ->
            // Arrange — automation is inactive
            val mockPreferences = mockk<AutomationPreferences>()
            val mockHotspotController = mockk<HotspotController>(relaxed = true)
            val mockContext = mockk<Context>()

            every { mockPreferences.isAutomationEnabled() } returns false

            val mockDevice = mockk<BluetoothDevice>()
            every { mockDevice.name } returns deviceName

            val intent = mockk<Intent>()
            every { intent.action } returns BluetoothDevice.ACTION_ACL_CONNECTED
            every {
                intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
            } returns mockDevice

            val receiver = BluetoothConnectionReceiver(mockPreferences, mockHotspotController)

            // Act
            receiver.onReceive(mockContext, intent)

            // Assert — enableHotspotIfNeeded must never be called for any device name
            // when automation is inactive, including the target "CHEVROLET6572"
            verify(exactly = 0) { mockHotspotController.enableHotspotIfNeeded() }
        }
    }
})
