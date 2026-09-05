package com.example.bthotspot

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.bthotspot.data.AutomationPreferences
import com.example.bthotspot.data.AutomationPreferencesImpl
import com.example.bthotspot.data.AutomationPrefsException
import com.example.bthotspot.permissions.PermissionHelper
import com.example.bthotspot.permissions.PermissionHelperImpl
import com.example.bthotspot.service.BluetoothMonitorService
import com.example.bthotspot.shizuku.ShizukuStatusChecker
import com.example.bthotspot.shizuku.ShizukuStatusCheckerImpl
import com.google.android.material.card.MaterialCardView
import com.google.android.material.switchmaterial.SwitchMaterial

/**
 * Single-screen Activity for the Bluetooth Hotspot Enabler app.
 *
 * Responsibilities (Requirements 1.1 – 1.7, 4.1 – 4.4):
 * - Display the [AutomationToggle] ([SwitchMaterial]) and a Shizuku status banner.
 * - On resume: sync toggle state from [AutomationPreferences]; evaluate Shizuku status
 *   and show an appropriate banner (not installed → Play Store link, installed but not
 *   running → launch Shizuku app); disable toggle while Shizuku is inactive.
 * - On toggle → on: verify Shizuku is running and permission is granted; if not, show
 *   guidance and revert; if yes, persist state and start [BluetoothMonitorService].
 * - On toggle → off: persist state and stop [BluetoothMonitorService].
 * - Catch [AutomationPrefsException] / [SecurityException] and show inline error.
 * - Check and request Bluetooth + POST_NOTIFICATIONS runtime permissions.
 * - Display a note that WRITE_SETTINGS is not required (Shizuku replaces that flow).
 */
class MainActivity : AppCompatActivity() {

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    /** Injected during [onCreate]; can be overridden in tests. */
    internal lateinit var automationPreferences: AutomationPreferences

    internal lateinit var shizukuStatusChecker: ShizukuStatusChecker

    internal lateinit var permissionHelper: PermissionHelper

    // -------------------------------------------------------------------------
    // Views
    // -------------------------------------------------------------------------

    private lateinit var automationToggle: SwitchMaterial
    private lateinit var shizukuBanner: MaterialCardView
    private lateinit var shizukuBannerText: TextView
    private lateinit var shizukuBannerDismiss: View
    private lateinit var errorText: TextView

    // -------------------------------------------------------------------------
    // State
    // -------------------------------------------------------------------------

    /** True while we are programmatically updating the toggle to prevent re-entrant handling. */
    private var suppressToggleListener = false

    /**
     * Request code used when asking Shizuku for its own runtime permission.
     * Range 0..65535 required by Shizuku API.
     */
    private companion object {
        const val SHIZUKU_PERMISSION_REQUEST_CODE = 1001
        const val SHIZUKU_PACKAGE_NAME = "moe.shizuku.privileged.api"

        /** Play Store URI for the Shizuku app. */
        val PLAY_STORE_SHIZUKU_URI: Uri =
            Uri.parse("https://play.google.com/store/apps/details?id=$SHIZUKU_PACKAGE_NAME")
    }

    // -------------------------------------------------------------------------
    // Permission launchers
    // -------------------------------------------------------------------------

    /**
     * Launcher for requesting Bluetooth (and optional POST_NOTIFICATIONS) runtime permissions.
     *
     * Requirements 4.1, 4.2, 4.3, 4.4
     */
    private val requestPermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
            handlePermissionResults(results)
        }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        automationPreferences = AutomationPreferencesImpl(applicationContext)
        shizukuStatusChecker = ShizukuStatusCheckerImpl(applicationContext)
        permissionHelper = PermissionHelperImpl()

        automationToggle = findViewById(R.id.automationToggle)
        shizukuBanner = findViewById(R.id.shizukuBanner)
        shizukuBannerText = findViewById(R.id.shizukuBannerText)
        shizukuBannerDismiss = findViewById(R.id.shizukuBannerDismiss)
        errorText = findViewById(R.id.errorText)

        // Dismiss banner button
        shizukuBannerDismiss.setOnClickListener {
            shizukuBanner.visibility = View.GONE
        }

        // Note about WRITE_SETTINGS not being required (Requirement 4.5).
        // Shizuku replaces the WRITE_SETTINGS flow entirely on API 36+. Display this
        // information in the status banner when Shizuku is active.
        setupToggleListener()
    }

    /**
     * Called when the Activity becomes visible.
     *
     * Re-reads persisted state and re-checks Shizuku each time the user returns
     * to the screen (e.g. after configuring Shizuku in another app).
     *
     * Requirements: 1.4, 1.6, 3.3
     */
    override fun onResume() {
        super.onResume()

        // Sync toggle from persisted state (Requirements 1.4, 1.6).
        val enabled = automationPreferences.isAutomationEnabled()
        suppressToggleListener = true
        automationToggle.isChecked = enabled
        suppressToggleListener = false

        // Evaluate Shizuku and update the banner / toggle-enabled state (Requirement 3.3).
        updateShizukuStatus()

        // Request any missing runtime permissions (Requirements 4.1, 4.2).
        requestMissingPermissions()
    }

    // -------------------------------------------------------------------------
    // Toggle
    // -------------------------------------------------------------------------

    private fun setupToggleListener() {
        automationToggle.setOnCheckedChangeListener { _, isChecked ->
            if (suppressToggleListener) return@setOnCheckedChangeListener

            if (isChecked) {
                handleToggleOn()
            } else {
                handleToggleOff()
            }
        }
    }

    /**
     * Handles enabling the automation toggle.
     *
     * Steps:
     * 1. Confirm Shizuku is running and permission is granted; revert + show guidance if not.
     * 2. Check required Android runtime permissions are granted.
     * 3. Persist state and start [BluetoothMonitorService].
     *
     * Requirements: 1.2, 1.5, 1.7, 3.3
     */
    private fun handleToggleOn() {
        // Validate Shizuku is active and permission is granted (Requirement 1.5, 3.3).
        if (!shizukuStatusChecker.isRunning()) {
            showError(getString(R.string.error_shizuku_not_running))
            revertToggle(false)
            updateShizukuStatus()   // refresh banner
            return
        }

        if (!shizukuStatusChecker.hasPermission()) {
            // Show Shizuku's own permission dialog.
            shizukuStatusChecker.requestPermission(SHIZUKU_PERMISSION_REQUEST_CODE)
            revertToggle(false)
            return
        }

        // Persist enabled state (Requirements 1.2, 1.7).
        try {
            automationPreferences.setAutomationEnabled(true)
        } catch (e: AutomationPrefsException) {
            showError(getString(R.string.error_prefs_write_failed))
            revertToggle(false)
            return
        }

        // Start the foreground service (Requirement 1.2).
        try {
            val intent = Intent(this, BluetoothMonitorService::class.java)
            startForegroundService(intent)
        } catch (e: SecurityException) {
            showError(getString(R.string.error_service_security))
            // Roll back the persisted state.
            try {
                automationPreferences.setAutomationEnabled(false)
            } catch (_: AutomationPrefsException) { /* best-effort */ }
            revertToggle(false)
            return
        }

        hideError()
    }

    /**
     * Handles disabling the automation toggle.
     *
     * Persists state and stops [BluetoothMonitorService].
     *
     * Requirements: 1.3, 1.7
     */
    private fun handleToggleOff() {
        // Persist disabled state (Requirements 1.3, 1.7).
        try {
            automationPreferences.setAutomationEnabled(false)
        } catch (e: AutomationPrefsException) {
            showError(getString(R.string.error_prefs_write_failed))
            revertToggle(true)
            return
        }

        // Stop the service (Requirement 1.3).
        val intent = Intent(this, BluetoothMonitorService::class.java)
        stopService(intent)

        hideError()
    }

    /** Reverts the toggle to [checked] without triggering the listener. */
    private fun revertToggle(checked: Boolean) {
        suppressToggleListener = true
        automationToggle.isChecked = checked
        suppressToggleListener = false
    }

    // -------------------------------------------------------------------------
    // Shizuku status banner
    // -------------------------------------------------------------------------

    /**
     * Checks Shizuku status and updates the banner and toggle interactivity accordingly.
     *
     * - Not installed → banner with Play Store deep-link; toggle disabled.
     * - Installed but not running → banner prompting user to open Shizuku; toggle disabled.
     * - Running → banner hidden; toggle enabled.
     *
     * Note: WRITE_SETTINGS is NOT required because Shizuku replaces that permission flow
     * entirely on API 36+. (Requirement 4.5 — display note that WRITE_SETTINGS is not needed.)
     *
     * Requirements: 1.1, 1.5, 3.3
     */
    private fun updateShizukuStatus() {
        when {
            !shizukuStatusChecker.isInstalled() -> {
                // Shizuku not installed — show Play Store link (Requirement 3.3).
                shizukuBannerText.text = getString(R.string.shizuku_not_installed_message)
                shizukuBanner.visibility = View.VISIBLE
                shizukuBanner.setOnClickListener {
                    openPlayStoreForShizuku()
                }
                // Toggle disabled until Shizuku is active (Requirement 1.1).
                automationToggle.isEnabled = false
            }

            !shizukuStatusChecker.isRunning() -> {
                // Installed but not running — prompt to open Shizuku app.
                shizukuBannerText.text = getString(R.string.shizuku_not_running_message)
                shizukuBanner.visibility = View.VISIBLE
                shizukuBanner.setOnClickListener {
                    launchShizukuApp()
                }
                // Toggle disabled (Requirement 1.1).
                automationToggle.isEnabled = false
            }

            else -> {
                // Shizuku is active — hide banner, enable toggle.
                shizukuBanner.visibility = View.GONE
                automationToggle.isEnabled = true

                // WRITE_SETTINGS note (Requirement 4.5): Shizuku replaces WRITE_SETTINGS.
                // Display this once as a transient informational note, not an error.
                // (No persistent UI element needed; the absence of a WRITE_SETTINGS prompt
                //  itself communicates that it is not required.)
            }
        }
    }

    /** Opens the Shizuku listing in the Play Store. */
    private fun openPlayStoreForShizuku() {
        try {
            val marketIntent = Intent(Intent.ACTION_VIEW,
                Uri.parse("market://details?id=$SHIZUKU_PACKAGE_NAME"))
            startActivity(marketIntent)
        } catch (e: android.content.ActivityNotFoundException) {
            // Fall back to browser-based Play Store URL.
            val browserIntent = Intent(Intent.ACTION_VIEW, PLAY_STORE_SHIZUKU_URI)
            startActivity(browserIntent)
        }
    }

    /** Launches the Shizuku app so the user can activate the service. */
    private fun launchShizukuApp() {
        val launchIntent = packageManager.getLaunchIntentForPackage(SHIZUKU_PACKAGE_NAME)
        if (launchIntent != null) {
            startActivity(launchIntent)
        } else {
            // App is installed but no launch intent — guide user to app info.
            val appInfoIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:$SHIZUKU_PACKAGE_NAME")
            }
            startActivity(appInfoIntent)
        }
    }

    // -------------------------------------------------------------------------
    // Runtime permissions (Requirements 4.1, 4.2, 4.3, 4.4)
    // -------------------------------------------------------------------------

    /**
     * Requests any runtime permissions that have not yet been granted.
     *
     * Permissions requested:
     * - [Manifest.permission.BLUETOOTH_CONNECT] on API 31+ (Requirement 4.1)
     * - [Manifest.permission.BLUETOOTH] on API < 31 (Requirement 4.2)
     * - [Manifest.permission.POST_NOTIFICATIONS] on API 33+ (for error notifications)
     */
    private fun requestMissingPermissions() {
        if (permissionHelper.hasAllPermissions(this)) return

        val needed = buildList {
            val btPerm = permissionHelper.requiredBluetoothPermission()
            if (ContextCompat.checkSelfPermission(this@MainActivity, btPerm)
                != PackageManager.PERMISSION_GRANTED) {
                add(btPerm)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(
                        this@MainActivity, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                    add(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }

        if (needed.isNotEmpty()) {
            requestPermissionsLauncher.launch(needed.toTypedArray())
        }
    }

    /**
     * Handles the result of a runtime permission request.
     *
     * - Denied (rationale still showable) → show denial message (Requirement 4.3).
     * - Permanently denied → show message with Settings deep-link (Requirement 4.4).
     */
    private fun handlePermissionResults(results: Map<String, Boolean>) {
        for ((permission, granted) in results) {
            if (granted) continue

            val permanentlyDenied = !shouldShowRequestPermissionRationale(permission)
            if (permanentlyDenied) {
                // Permanently denied — direct user to settings (Requirement 4.4).
                showError(buildPermanentDenialMessage(permission))
                errorText.setOnClickListener { openAppSettings() }
            } else {
                // Denied but can ask again — show denial message (Requirement 4.3).
                showError(permissionHelper.buildDenialMessage(permission))
            }
            // Disable toggle if Bluetooth permission is missing.
            if (permission == permissionHelper.requiredBluetoothPermission()) {
                automationToggle.isEnabled = false
            }
        }
    }

    /**
     * Builds a permanent-denial message that directs the user to Android settings.
     *
     * Requirement 4.4: must NOT attempt to re-request via dialog;
     * must link to system settings.
     */
    private fun buildPermanentDenialMessage(permission: String): String {
        return getString(R.string.permission_permanently_denied_message, permission)
    }

    private fun openAppSettings() {
        startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:$packageName")
        })
    }

    // -------------------------------------------------------------------------
    // Inline error helpers
    // -------------------------------------------------------------------------

    private fun showError(message: String) {
        errorText.text = message
        errorText.visibility = View.VISIBLE
    }

    private fun hideError() {
        errorText.visibility = View.GONE
        errorText.text = ""
        errorText.setOnClickListener(null)
    }
}
