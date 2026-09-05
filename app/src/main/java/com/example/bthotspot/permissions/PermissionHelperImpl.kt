package com.example.bthotspot.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

open class PermissionHelperImpl : PermissionHelper {

    /**
     * Returns [Manifest.permission.BLUETOOTH_CONNECT] on API 31 (Android 12) and above,
     * or [Manifest.permission.BLUETOOTH] on older versions.
     *
     * Validates: Requirements 4.1, 4.2
     */
    open override fun requiredBluetoothPermission(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Manifest.permission.BLUETOOTH_CONNECT
        } else {
            Manifest.permission.BLUETOOTH
        }
    }

    /**
     * Returns true if all required runtime permissions are granted.
     *
     * Required permissions:
     * - [Manifest.permission.BLUETOOTH_CONNECT] on API 31+, [Manifest.permission.BLUETOOTH] otherwise
     * - [Manifest.permission.POST_NOTIFICATIONS] on API 33+
     *
     * Validates: Requirements 4.1, 4.2
     */
    override fun hasAllPermissions(context: Context): Boolean {
        val permissions = buildList {
            add(requiredBluetoothPermission())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        return permissions.all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Builds a denial message for the given [permission] that contains:
     * (a) the permission name, (b) a functionality description, (c) the word "Settings".
     *
     * Validates: Requirements 4.3, 4.4
     */
    override fun buildDenialMessage(permission: String): String {
        return "The $permission permission is required to detect Bluetooth device connections. " +
            "To grant it, go to Settings > Apps > Bluetooth Hotspot Enabler > Permissions."
    }
}
