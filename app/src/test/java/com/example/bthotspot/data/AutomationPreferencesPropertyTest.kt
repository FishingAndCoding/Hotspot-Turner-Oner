package com.example.bthotspot.data

import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.boolean
import io.kotest.property.checkAll

@Tags("Feature: bluetooth-hotspot-enabler", "Property 1: Persistence round-trip")
class AutomationPreferencesPropertyTest : FreeSpec({
    "Property 1 - automation state persistence round-trip" {
        checkAll(iterations = 100, Arb.boolean()) { enabled ->
            val preferences = InMemoryAutomationPreferences()

            preferences.setAutomationEnabled(enabled)

            preferences.isAutomationEnabled() shouldBe enabled
        }
    }
}) {
    private class InMemoryAutomationPreferences : AutomationPreferences {
        private var enabled = false

        override fun isAutomationEnabled(): Boolean = enabled

        override fun setAutomationEnabled(enabled: Boolean) {
            this.enabled = enabled
        }
    }
}