package com.example.bthotspot.service

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class BluetoothMonitorServiceTest {

    @Test
    fun `service returns START_STICKY after starting foreground monitoring`() {
        val controller = Robolectric.buildService(BluetoothMonitorService::class.java)
        val service = controller.create().get()

        val result = service.onStartCommand(null, 0, 1)

        assertEquals(android.app.Service.START_STICKY, result)
        controller.destroy()
        service.stopSelf()
    }
}
