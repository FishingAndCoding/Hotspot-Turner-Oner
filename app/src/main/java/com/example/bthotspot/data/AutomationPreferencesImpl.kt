package com.example.bthotspot.data

import android.content.Context

class AutomationPreferencesImpl(context: Context) : AutomationPreferences {

    private val prefs = context.getSharedPreferences("bthotspot_prefs", Context.MODE_PRIVATE)

    override fun isAutomationEnabled(): Boolean {
        return try {
            prefs.getBoolean(KEY_AUTOMATION_ENABLED, DEFAULT_VALUE)
        } catch (e: Exception) {
            false
        }
    }

    override fun setAutomationEnabled(enabled: Boolean) {
        try {
            prefs.edit().putBoolean(KEY_AUTOMATION_ENABLED, enabled).apply()
        } catch (e: Exception) {
            throw AutomationPrefsException("Failed to persist automation state", e)
        }
    }

    companion object {
        private const val KEY_AUTOMATION_ENABLED = "automation_enabled"
        private const val DEFAULT_VALUE = false
    }
}
