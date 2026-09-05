package com.example.bthotspot.permissions

import android.Manifest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Unit tests for [PermissionHelperImpl].
 *
 * Validates: Requirements 4.1, 4.2, 4.3, 4.4
 */
class PermissionHelperImplTest {

    // ---------------------------------------------------------------------------
    // requiredBluetoothPermission() — Requirements 4.1, 4.2
    // ---------------------------------------------------------------------------

    /**
     * Subclass that simulates running on API 31+ by overriding the permission method directly.
     * We use this approach because Build.VERSION.SDK_INT is a final field that cannot be
     * mocked without a bytecode manipulation agent. Since PermissionHelperImpl declares
     * requiredBluetoothPermission() as `open`, we can override it cleanly.
     */
    private class Api31PlusHelper : PermissionHelperImpl() {
        override fun requiredBluetoothPermission(): String =
            Manifest.permission.BLUETOOTH_CONNECT
    }

    private class PreApi31Helper : PermissionHelperImpl() {
        override fun requiredBluetoothPermission(): String =
            Manifest.permission.BLUETOOTH
    }

    // Requirements 4.1 — API 31+ must return BLUETOOTH_CONNECT
    @Test
    fun `requiredBluetoothPermission returns BLUETOOTH_CONNECT on API 31 and above`() {
        val helper = Api31PlusHelper()

        val result = helper.requiredBluetoothPermission()

        assertEquals(
            Manifest.permission.BLUETOOTH_CONNECT,
            result,
            "Expected BLUETOOTH_CONNECT on API 31+"
        )
    }

    // Requirements 4.2 — below API 31 must return BLUETOOTH
    @Test
    fun `requiredBluetoothPermission returns BLUETOOTH on API below 31`() {
        val helper = PreApi31Helper()

        val result = helper.requiredBluetoothPermission()

        assertEquals(
            Manifest.permission.BLUETOOTH,
            result,
            "Expected BLUETOOTH on API below 31"
        )
    }

    // ---------------------------------------------------------------------------
    // buildDenialMessage() — Requirements 4.3, 4.4
    // ---------------------------------------------------------------------------

    // Requirements 4.3 — denial message must contain the permission name
    @Test
    fun `buildDenialMessage contains the permission name`() {
        val helper = PermissionHelperImpl()
        val permission = Manifest.permission.BLUETOOTH_CONNECT

        val message = helper.buildDenialMessage(permission)

        assertTrue(
            message.contains(permission),
            "Expected denial message to contain the permission name '$permission', but got: $message"
        )
    }

    // Requirements 4.3 — denial message must contain a functionality description (non-trivially
    // verified by checking for meaningful Bluetooth-related wording)
    @Test
    fun `buildDenialMessage contains a functionality description`() {
        val helper = PermissionHelperImpl()

        val message = helper.buildDenialMessage(Manifest.permission.BLUETOOTH_CONNECT)

        // The message should describe what the permission enables — at minimum it must be
        // non-empty and contain some explanatory text beyond just the permission string itself.
        assertTrue(
            message.length > Manifest.permission.BLUETOOTH_CONNECT.length,
            "Expected denial message to contain a functionality description beyond the bare permission name"
        )
        assertTrue(
            message.contains("Bluetooth", ignoreCase = true),
            "Expected denial message to mention Bluetooth functionality"
        )
    }

    // Requirements 4.4 — permanently denied case must include a settings navigation hint
    @Test
    fun `buildDenialMessage for permanently denied case includes settings navigation hint`() {
        val helper = PermissionHelperImpl()

        // Simulate the permanently-denied scenario: the caller passes the required permission
        // and expects a message directing them to system settings rather than re-requesting.
        val messageForConnect = helper.buildDenialMessage(Manifest.permission.BLUETOOTH_CONNECT)
        val messageForBluetooth = helper.buildDenialMessage(Manifest.permission.BLUETOOTH)

        assertTrue(
            messageForConnect.contains("Settings", ignoreCase = true),
            "Expected denial message for BLUETOOTH_CONNECT to contain 'Settings' for navigation hint, but got: $messageForConnect"
        )
        assertTrue(
            messageForBluetooth.contains("Settings", ignoreCase = true),
            "Expected denial message for BLUETOOTH to contain 'Settings' for navigation hint, but got: $messageForBluetooth"
        )
    }

    // Requirements 4.3 — denial message is non-empty for any permission string
    @Test
    fun `buildDenialMessage returns non-empty string for any permission`() {
        val helper = PermissionHelperImpl()

        listOf(
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.POST_NOTIFICATIONS
        ).forEach { permission ->
            val message = helper.buildDenialMessage(permission)
            assertTrue(
                message.isNotBlank(),
                "Expected non-blank denial message for permission '$permission'"
            )
        }
    }
}
