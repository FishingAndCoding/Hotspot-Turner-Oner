package com.example.bthotspot.data

interface AutomationPreferences {
    /** Returns persisted automation state. Returns false on read failure. */
    fun isAutomationEnabled(): Boolean

    /** Persists automation state. Throws AutomationPrefsException on failure. */
    fun setAutomationEnabled(enabled: Boolean)
}

class AutomationPrefsException(message: String, cause: Throwable?) : Exception(message, cause)
