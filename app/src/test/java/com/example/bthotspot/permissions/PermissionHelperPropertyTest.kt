package com.example.bthotspot.permissions

import android.Manifest
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotBeEmpty
import io.kotest.property.Arb
import io.kotest.property.arbitrary.element
import io.kotest.property.checkAll

/**
 * Property-based tests for [PermissionHelper].
 *
 * **Validates: Requirements 4.3**
 */
@Tags("Feature: bluetooth-hotspot-enabler", "Property 4: Permission denial message completeness")
class PermissionHelperPropertyTest : FreeSpec({

    val permissionHelper = PermissionHelperImpl()

    /**
     * Property 4: Permission denial message completeness
     *
     * For any permission in {BLUETOOTH_CONNECT, BLUETOOTH},
     * buildDenialMessage() must return a string containing:
     *   (a) the permission name,
     *   (b) a non-empty functionality description,
     *   (c) the word "Settings" or a settings URI.
     *
     * **Validates: Requirements 4.3**
     */
    "Property 4 - Permission denial message completeness" {
        checkAll(
            iterations = 100,
            Arb.element(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH
            )
        ) { permission ->
            val message = permissionHelper.buildDenialMessage(permission)

            // (a) message must contain the permission name
            message shouldContain permission

            // (b) message must be non-empty (functionality description present)
            message.shouldNotBeEmpty()

            // (c) message must contain "Settings" (or a settings URI)
            val containsSettingsHint = message.contains("Settings") ||
                message.contains("settings") ||
                message.contains("android.settings")
            assert(containsSettingsHint) {
                "Expected denial message to contain 'Settings' or a settings URI, but got: $message"
            }
        }
    }
})
