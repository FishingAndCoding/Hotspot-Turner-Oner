package com.example.bthotspot

import android.content.Context
import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BootReceiverIntegrationTest {

    @Test
    fun bootBroadcastIsAcceptedByApplicationReceiver() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context.sendBroadcast(Intent(Intent.ACTION_BOOT_COMPLETED).setPackage(context.packageName))
    }
}