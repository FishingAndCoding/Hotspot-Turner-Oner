package com.example.bthotspot.data

import android.content.Context
import android.content.SharedPreferences
import io.mockk.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * Unit tests for [AutomationPreferencesImpl].
 *
 * Validates: Requirements 1.4, 1.6, 1.7
 */
class AutomationPreferencesImplTest {

    private val mockEditor = mockk<SharedPreferences.Editor>(relaxed = true)
    private val mockPrefs = mockk<SharedPreferences>()
    private val mockContext = mockk<Context>()

    init {
        every { mockContext.getSharedPreferences(any(), any()) } returns mockPrefs
        every { mockPrefs.edit() } returns mockEditor
        every { mockEditor.putBoolean(any(), any()) } returns mockEditor
        every { mockEditor.apply() } just Runs
    }

    // Requirements 1.4 — default value is false when no key is stored
    @Test
    fun `isAutomationEnabled returns false when key not stored`() {
        every { mockPrefs.getBoolean("automation_enabled", false) } returns false

        val impl = AutomationPreferencesImpl(mockContext)

        assert(!impl.isAutomationEnabled()) {
            "Expected isAutomationEnabled() to return false when the key has never been written"
        }
    }

    // Requirements 1.6 — write failure surfaces as AutomationPrefsException
    @Test
    fun `setAutomationEnabled throws AutomationPrefsException on write failure`() {
        every { mockPrefs.getBoolean(any(), any()) } returns false
        every { mockEditor.apply() } throws RuntimeException("write error")

        val impl = AutomationPreferencesImpl(mockContext)

        assertThrows<AutomationPrefsException> {
            impl.setAutomationEnabled(true)
        }
    }

    // Requirements 1.7 — read failure is swallowed and returns false
    @Test
    fun `isAutomationEnabled returns false on read exception`() {
        every { mockPrefs.getBoolean(any(), any()) } throws RuntimeException("read error")

        val impl = AutomationPreferencesImpl(mockContext)

        assert(!impl.isAutomationEnabled()) {
            "Expected isAutomationEnabled() to return false when SharedPreferences throws on read"
        }
    }
}
