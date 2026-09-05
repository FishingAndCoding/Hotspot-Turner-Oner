package com.example.bthotspot

import android.content.Context
import android.content.Intent
import android.view.View
import com.example.bthotspot.data.AutomationPreferences
import com.example.bthotspot.permissions.PermissionHelper
import com.example.bthotspot.shizuku.ShizukuStatusChecker
import com.google.android.material.switchmaterial.SwitchMaterial
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MainActivityTest {

    @Test
    fun `toggle defaults to off when preferences are empty`() {
        val preferences = FakePreferences(false)
        val activity = activityWith(preferences, FakeShizuku(running = true, permission = true))

        assertFalse(activity.findViewById<SwitchMaterial>(R.id.automationToggle).isChecked)
    }

    @Test
    fun `preference write failure shows inline error`() {
        val preferences = FakePreferences(false, failWrites = true)
        val activity = activityWith(preferences, FakeShizuku(running = true, permission = true))
        val toggle = activity.findViewById<SwitchMaterial>(R.id.automationToggle)

        toggle.performClick()

        assertTrue(activity.findViewById<View>(R.id.errorText).visibility == View.VISIBLE)
    }

    @Test
    fun `toggle is disabled when Shizuku is not running`() {
        val activity = activityWith(FakePreferences(false), FakeShizuku(running = false, permission = false))

        assertFalse(activity.findViewById<SwitchMaterial>(R.id.automationToggle).isEnabled)
    }

    @Test
    fun `enabling toggle persists automation without hotspot controller access`() {
        val preferences = FakePreferences(false)
        val activity = activityWith(preferences, FakeShizuku(running = true, permission = true))
        val toggle = activity.findViewById<SwitchMaterial>(R.id.automationToggle)

        toggle.performClick()

        assertTrue(preferences.enabled)
    }

    private fun activityWith(
        preferences: FakePreferences,
        shizuku: FakeShizuku,
    ): MainActivity {
        val controller = Robolectric.buildActivity(MainActivity::class.java).create()
        val activity = controller.get()
        activity.automationPreferences = preferences
        activity.shizukuStatusChecker = shizuku
        activity.permissionHelper = AlwaysGrantedPermissions()
        return controller.start().resume().get()
    }

    private class FakePreferences(
        var enabled: Boolean,
        private val failWrites: Boolean = false,
    ) : AutomationPreferences {
        override fun isAutomationEnabled(): Boolean = enabled

        override fun setAutomationEnabled(enabled: Boolean) {
            if (failWrites) {
                throw com.example.bthotspot.data.AutomationPrefsException("write failed", null)
            }
            this.enabled = enabled
        }
    }

    private class FakeShizuku(
        private val running: Boolean,
        private val permission: Boolean,
    ) : ShizukuStatusChecker {
        override fun isInstalled(): Boolean = true
        override fun isRunning(): Boolean = running
        override fun hasPermission(): Boolean = permission
        override fun requestPermission(requestCode: Int) = Unit
    }

    private class AlwaysGrantedPermissions : PermissionHelper {
        override fun requiredBluetoothPermission(): String = android.Manifest.permission.BLUETOOTH_CONNECT
        override fun hasAllPermissions(context: Context): Boolean = true
        override fun buildDenialMessage(permission: String): String = permission
    }
}
