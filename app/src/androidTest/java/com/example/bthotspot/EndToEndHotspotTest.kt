package com.example.bthotspot

import android.bluetooth.BluetoothDevice
import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EndToEndHotspotTest {

    @Test
    fun targetBluetoothConnectionBroadcastCanBeDispatched() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val shizukuInstalled = try {
            context.packageManager.getPackageInfo("moe.shizuku.privileged.api", 0)
            true
        } catch (_: Exception) {
            false
        }
        assumeTrue("Shizuku must be installed for this end-to-end scenario", shizukuInstalled)

        val intent = Intent(BluetoothDevice.ACTION_ACL_CONNECTED)
            .setPackage(context.packageName)
        context.sendBroadcast(intent)
    }
}