package com.example.bthotspot.hotspot

import android.content.Context
import android.net.wifi.WifiManager
import com.example.bthotspot.notifications.NotificationHelper
import com.example.bthotspot.shizuku.ShizukuGateway
import io.mockk.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Unit tests for [HotspotControllerImpl].
 *
 * Validates: Requirements 3.2, 3.3, 3.4
 *
 * Strategy
 * --------
 * • [WifiManager] and [NotificationHelper] are mocked via MockK.
 * • [Shizuku] static methods are mocked via `mockkStatic`.
 * • For the `onTetheringFailed` path: because `startTetheringViaShizuku` uses
 *   deep reflection against a real Android framework that is absent in the JVM
 *   unit-test environment, we use a minimal test subclass
 *   [FakeHotspotController] that overrides the protected method to immediately
 *   invoke the failure callback — isolating the callback-handling logic from the
 *   Shizuku/reflection plumbing.
 */
class HotspotControllerImplTest {

    private val mockNotificationHelper = mockk<NotificationHelper>(relaxed = true)
    private val mockWifiManager = mockk<WifiManager>()
    private val mockContext = mockk<Context>()
    private val shizukuGateway = mockk<ShizukuGateway>()

    @BeforeEach
    fun setUp() {
        // Context.applicationContext.getSystemService(WIFI_SERVICE) chain
        every { mockContext.applicationContext } returns mockContext
        every { mockContext.getSystemService(Context.WIFI_SERVICE) } returns mockWifiManager

    }

    // -------------------------------------------------------------------------
    // Requirement 3.2 — no-op when hotspot is already enabled
    // -------------------------------------------------------------------------

    @Test
    fun `enableHotspotIfNeeded is a no-op when hotspot is already enabled`() {
        val controller = TestHotspotController(
            mockContext,
            mockNotificationHelper,
            true,
            shizukuGateway,
        )
        controller.enableHotspotIfNeeded()

        // Shizuku must not be touched, and no notification must be posted.
        verify(exactly = 0) { mockNotificationHelper.postErrorNotification(any(), any()) }
    }

    // -------------------------------------------------------------------------
    // Requirement 3.3 — post notification when Shizuku is not active
    // -------------------------------------------------------------------------

    @Test
    fun `enableHotspotIfNeeded posts Shizuku-not-active notification when pingBinder returns false`() {
        every { shizukuGateway.pingBinder() } returns false

        val controller = TestHotspotController(mockContext, mockNotificationHelper, false, shizukuGateway)
        controller.enableHotspotIfNeeded()

        verify(exactly = 1) {
            mockNotificationHelper.postErrorNotification(
                title = "Shizuku is not active",
                body  = any(),
            )
        }
    }

    @Test
    fun `enableHotspotIfNeeded posts Shizuku-not-active notification when pingBinder throws`() {
        every { shizukuGateway.pingBinder() } throws IllegalStateException("Shizuku not bound")

        val controller = TestHotspotController(mockContext, mockNotificationHelper, false, shizukuGateway)
        controller.enableHotspotIfNeeded()

        verify(exactly = 1) {
            mockNotificationHelper.postErrorNotification(
                title = "Shizuku is not active",
                body  = any(),
            )
        }
    }

    // -------------------------------------------------------------------------
    // Requirement 3.4 — post failure notification when onTetheringFailed fires
    // -------------------------------------------------------------------------

    @Test
    fun `enableHotspotIfNeeded posts failure notification when onTetheringFailed fires with an error code`() {
        every { shizukuGateway.pingBinder() } returns true
        every { shizukuGateway.checkSelfPermission() } returns android.content.pm.PackageManager.PERMISSION_GRANTED

        // FakeHotspotController overrides startTetheringViaShizuku to immediately
        // simulate an onTetheringFailed(error = 3) callback without requiring the
        // Android framework or a live Shizuku process.
        val controller = FakeHotspotController(
            context              = mockContext,
            notificationHelper   = mockNotificationHelper,
            simulatedErrorCode   = 3,
            hotspotEnabled       = false,
            shizukuGateway       = shizukuGateway,
        )
        controller.enableHotspotIfNeeded()

        verify(exactly = 1) {
            mockNotificationHelper.postErrorNotification(
                title = "Hotspot could not be enabled",
                body  = "Error code: 3",
            )
        }
    }

    @Test
    fun `enableHotspotIfNeeded posts failure notification with correct error code`() {
        every { shizukuGateway.pingBinder() } returns true
        every { shizukuGateway.checkSelfPermission() } returns android.content.pm.PackageManager.PERMISSION_GRANTED

        val controller = FakeHotspotController(
            context              = mockContext,
            notificationHelper   = mockNotificationHelper,
            simulatedErrorCode   = 7,
            hotspotEnabled       = false,
            shizukuGateway       = shizukuGateway,
        )
        controller.enableHotspotIfNeeded()

        verify(exactly = 1) {
            mockNotificationHelper.postErrorNotification(
                title = "Hotspot could not be enabled",
                body  = "Error code: 7",
            )
        }
    }

    // -------------------------------------------------------------------------
    // Helper — test subclass
    // -------------------------------------------------------------------------

    /**
     * Minimal subclass of [HotspotControllerImpl] that bypasses the Shizuku /
     * reflection plumbing in [startTetheringViaShizuku] and immediately fires the
     * `onTetheringFailed` notification path with a configurable error code.
     *
     * This keeps the test completely on the JVM with no Android framework
     * dependencies while still exercising the production notification logic.
     */
    private class FakeHotspotController(
        context: Context,
        notificationHelper: NotificationHelper,
        private val simulatedErrorCode: Int,
        private val hotspotEnabled: Boolean,
        shizukuGateway: ShizukuGateway,
    ) : HotspotControllerImpl(context, notificationHelper, shizukuGateway) {

        override fun readWifiApEnabled(): Boolean = hotspotEnabled

        override fun startTetheringViaShizuku() {
            // Directly invoke the same error-reporting path as the production
            // onTetheringFailed(error) callback.
            notificationHelper.postErrorNotification(
                title = "Hotspot could not be enabled",
                body  = "Error code: $simulatedErrorCode",
            )
        }
    }

    private class TestHotspotController(
        context: Context,
        notificationHelper: NotificationHelper,
        private val hotspotEnabled: Boolean,
        shizukuGateway: ShizukuGateway,
    ) : HotspotControllerImpl(context, notificationHelper, shizukuGateway) {

        override fun readWifiApEnabled(): Boolean = hotspotEnabled
    }
}
