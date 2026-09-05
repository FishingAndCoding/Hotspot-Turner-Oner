package com.example.bthotspot.boot

import android.content.Context
import android.content.ComponentName
import android.content.ContextWrapper
import android.content.Intent
import com.example.bthotspot.data.AutomationPreferences
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test

class BootReceiverTest {

    private val preferences = mockk<AutomationPreferences>()

    @Test
    fun `starts monitoring service when automation is active`() {
        every { preferences.isAutomationEnabled() } returns true
        val context = RecordingContext()

        BootReceiver(preferences).onReceive(
            context,
            bootIntent(),
        )

        assert(context.foregroundServiceStarts == 1)
    }

    @Test
    fun `does not start monitoring service when automation is inactive`() {
        every { preferences.isAutomationEnabled() } returns false
        val context = RecordingContext()

        BootReceiver(preferences).onReceive(
            context,
            bootIntent(),
        )

        assert(context.foregroundServiceStarts == 0)
    }

    private class RecordingContext : ContextWrapper(null) {
        var foregroundServiceStarts = 0

        override fun getPackageName(): String = "com.example.bthotspot"

        override fun startForegroundService(service: Intent): ComponentName? {
            foregroundServiceStarts++
            return null
        }
    }

    private fun bootIntent(): Intent = mockk<Intent> {
        every { action } returns Intent.ACTION_BOOT_COMPLETED
    }
}