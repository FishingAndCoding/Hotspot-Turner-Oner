package com.example.bthotspot.permissions

import android.content.Context

interface PermissionHelper {
    /** Returns BLUETOOTH_CONNECT on API 31+, BLUETOOTH below. */
    fun requiredBluetoothPermission(): String

    /** Returns true if all required Android runtime permissions are granted. */
    fun hasAllPermissions(context: Context): Boolean

    /**
     * Builds a denial message containing:
     * (a) permission name, (b) functionality description, (c) settings navigation hint.
     */
    fun buildDenialMessage(permission: String): String
}
