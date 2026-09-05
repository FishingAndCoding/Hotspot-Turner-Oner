package com.example.bthotspot.bluetooth

import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import com.example.bthotspot.data.AutomationPreferences
import com.example.bthotspot.hotspot.HotspotController
import io.mockk.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Unit tests for [BluetoothConnectionReceiver].
 *
 * Validates: Requirements 2.3, 2.4, 2.5
 *
 * Strategy
 * --------
 * - [AutomationPreferences] and [HotspotController] are constructor-injected and
 *   mocked via MockK.
 * - [Intent] is mocked to return [BluetoothDevice.ACTION_ACL_CONNECTED] as its
 *   action and a mocked [BluetoothDevice] as the [BluetoothDevice.EXTRA_DEVICE]
 *   parcelable extra.
 * - [BluetoothDevice] is a final Android class; MockK's inline mock-maker handles
 *   this without any additional configuration.
 * - [Context] is unused by the receiver's logic but is required by the
 *   [BroadcastReceiver.onReceive] signature; a relaxed mock is sufficient.
 */
class BluetoothConnectionReceiverTest {

    private val mockAutomationPreferences = mockk<AutomationPreferences>()
    private val mockHotspotController = mockk<HotspotController>(relaxed = true)
    private val mockContext = mockk<Context>(relaxed = true)

    private lateinit var receiver: BluetoothConnectionReceiver

    @BeforeEach
    fun setUp() {
        receiver = BluetoothConnectionReceiver(
            automationPreferences = mockAutomationPreferences,
            hotspotController = mockHotspotController,
        )
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /** Builds a mock Intent that delivers an ACL_CONNECTED broadcast. */
    private fun aclConnectedIntent(deviceName: String?): Intent {
        val mockDevice = mockk<BluetoothDevice>()
        every { mockDevice.name } returns deviceName

        val mockIntent = mockk<Intent>()
        every { mockIntent.action } returns BluetoothDevice.ACTION_ACL_CONNECTED
        every {
            mockIntent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
        } returns mockDevice

        return mockIntent
    }

    // -------------------------------------------------------------------------
    // Requirement 2.3 — target device + automation active → enableHotspotIfNeeded called once
    // -------------------------------------------------------------------------

    @Test
    fun `calls enableHotspotIfNeeded exactly once when name matches and automation is active`() {
        every { mockAutomationPreferences.isAutomationEnabled() } returns true
        val intent = aclConnectedIntent(BluetoothConnectionReceiver.TARGET_DEVICE_NAME)

        receiver.onReceive(mockContext, intent)

        verify(exactly = 1) { mockHotspotController.enableHotspotIfNeeded() }
    }

    // -------------------------------------------------------------------------
    // Requirement 2.5 — name matches but automation is inactive → no action
    // -------------------------------------------------------------------------

    @Test
    fun `takes no action when name matches but automation is inactive`() {
        every { mockAutomationPreferences.isAutomationEnabled() } returns false
        val intent = aclConnectedIntent(BluetoothConnectionReceiver.TARGET_DEVICE_NAME)

        receiver.onReceive(mockContext, intent)

        verify(exactly = 0) { mockHotspotController.enableHotspotIfNeeded() }
    }

    // -------------------------------------------------------------------------
    // Requirement 2.4 — name does not match and automation is active → no action
    // -------------------------------------------------------------------------

    @Test
    fun `takes no action when name does not match and automation is active`() {
        every { mockAutomationPreferences.isAutomationEnabled() } returns true
        val intent = aclConnectedIntent("SOME_OTHER_DEVICE")

        receiver.onReceive(mockContext, intent)

        verify(exactly = 0) { mockHotspotController.enableHotspotIfNeeded() }
        // isAutomationEnabled should not even be consulted for a non-matching name
        verify(exactly = 0) { mockAutomationPreferences.isAutomationEnabled() }
    }
}
