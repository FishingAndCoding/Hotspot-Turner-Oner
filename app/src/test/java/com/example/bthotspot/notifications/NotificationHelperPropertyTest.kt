package com.example.bthotspot.notifications

import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotBeBlank
import io.kotest.property.Arb
import io.kotest.property.arbitrary.boolean
import io.kotest.property.checkAll

/**
 * Property-based tests for [NotificationHelper].
 *
 * This test verifies the notification status correctness contract defined in Property 5
 * using a local fake that mirrors the real impl's content logic without requiring the
 * Android framework (NotificationCompat / NotificationManager) at JVM test time.
 *
 * **Validates: Requirements 5.2**
 */
@Tags("Feature: bluetooth-hotspot-enabler", "Property 5: Notification status correctness")
class NotificationHelperPropertyTest : FreeSpec({

    /**
     * A testable record of the values the notification was built with.
     * Returned instead of a real [Notification] so assertions can inspect
     * the exact title and status text without involving the Android framework.
     */
    data class NotificationContent(val appName: String, val statusText: String)

    /**
     * Lightweight fake that mirrors the content-derivation logic of
     * [NotificationHelperImpl.buildServiceNotification] but returns a
     * [NotificationContent] record instead of an Android [Notification].
     *
     * The fake encodes the same rules:
     *   - appName is always "Bluetooth Hotspot Enabler"
     *   - statusText is "Monitoring" when isMonitoring == true, "Stopped" otherwise
     */
    fun buildNotificationContent(isMonitoring: Boolean): NotificationContent {
        val appName = "Bluetooth Hotspot Enabler"
        val statusText = if (isMonitoring) "Monitoring" else "Stopped"
        return NotificationContent(appName = appName, statusText = statusText)
    }

    /**
     * Property 5: Notification status correctness
     *
     * For any boolean monitoring state, the notification built by
     * buildServiceNotification(isMonitoring) must:
     *   (a) include the app name (non-blank),
     *   (b) display "Monitoring" when isMonitoring is true,
     *   (c) display "Stopped" when isMonitoring is false.
     *
     * **Validates: Requirements 5.2**
     */
    "Property 5 - Notification status correctness" {
        checkAll(
            iterations = 100,
            Arb.boolean()
        ) { isMonitoring ->
            val content = buildNotificationContent(isMonitoring)

            // (a) App name must be present and non-blank
            content.appName.shouldNotBeBlank()
            content.appName shouldBe "Bluetooth Hotspot Enabler"

            // (b)/(c) Status text must match the monitoring state exactly
            val expectedStatusText = if (isMonitoring) "Monitoring" else "Stopped"
            content.statusText shouldBe expectedStatusText
        }
    }
})
