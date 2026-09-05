package com.example.bthotspot

import android.content.Context
import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.BufferedReader
import java.io.InputStreamReader
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BootReceiverIntegrationTest {

    @Test
    fun bootBroadcastIsAcceptedByApplicationReceiver() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val descriptor = InstrumentationRegistry.getInstrumentation().uiAutomation
            .executeShellCommand(
                "am broadcast -a ${Intent.ACTION_BOOT_COMPLETED} -p ${context.packageName}",
            )
        BufferedReader(InputStreamReader(
            android.os.ParcelFileDescriptor.AutoCloseInputStream(descriptor),
        )).use { reader ->
            reader.readText()
        }
    }
}