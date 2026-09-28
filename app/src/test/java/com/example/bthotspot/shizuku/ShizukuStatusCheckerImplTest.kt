package com.example.bthotspot.shizuku

import android.content.Context
import android.content.pm.PackageManager
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue

/**
 * Unit tests for [ShizukuStatusCheckerImpl].
 *
 * Validates: Requirements 3.3
 */
class ShizukuStatusCheckerImplTest {

    private val mockPackageManager = mockk<PackageManager>()
    private val mockContext = mockk<Context>()
    private val shizukuGateway = mockk<ShizukuGateway>()

    @BeforeEach
    fun setUp() {
        every { mockContext.packageManager } returns mockPackageManager
    }

    // --- isInstalled() ---

    @Test
    fun `isInstalled returns false when Shizuku package is absent`() {
        every {
            mockPackageManager.getPackageInfo("moe.shizuku.privileged.api", 0)
        } throws PackageManager.NameNotFoundException()
        // Fallback: getLaunchIntentForPackage also returns null when not installed
        every {
            mockPackageManager.getLaunchIntentForPackage("moe.shizuku.privileged.api")
        } returns null

        val checker = ShizukuStatusCheckerImpl(mockContext, shizukuGateway)

        assertFalse(checker.isInstalled()) {
            "Expected isInstalled() to return false when the Shizuku package is not found"
        }
    }

    @Test
    fun `isInstalled returns true when Shizuku package is present`() {
        every {
            mockPackageManager.getPackageInfo("moe.shizuku.privileged.api", 0)
        } returns mockk() // non-null PackageInfo → package exists

        val checker = ShizukuStatusCheckerImpl(mockContext, shizukuGateway)

        assertTrue(checker.isInstalled()) {
            "Expected isInstalled() to return true when the Shizuku package is found"
        }
    }

    // --- isRunning() ---

    @Test
    fun `isRunning returns true when Shizuku pingBinder returns true`() {
        every { shizukuGateway.pingBinder() } returns true

        val checker = ShizukuStatusCheckerImpl(mockContext, shizukuGateway)

        assertTrue(checker.isRunning()) {
            "Expected isRunning() to return true when Shizuku.pingBinder() returns true"
        }
    }

    @Test
    fun `isRunning returns false when Shizuku pingBinder returns false`() {
        every { shizukuGateway.pingBinder() } returns false

        val checker = ShizukuStatusCheckerImpl(mockContext, shizukuGateway)

        assertFalse(checker.isRunning()) {
            "Expected isRunning() to return false when Shizuku.pingBinder() returns false"
        }
    }

    @Test
    fun `isRunning returns false when Shizuku pingBinder throws`() {
        every { shizukuGateway.pingBinder() } throws IllegalStateException("Shizuku not bound")

        val checker = ShizukuStatusCheckerImpl(mockContext, shizukuGateway)

        assertFalse(checker.isRunning()) {
            "Expected isRunning() to return false when Shizuku.pingBinder() throws"
        }
    }

    // --- hasPermission() ---

    @Test
    fun `hasPermission returns true when Shizuku checkSelfPermission returns PERMISSION_GRANTED`() {
        every { shizukuGateway.checkSelfPermission() } returns PackageManager.PERMISSION_GRANTED

        val checker = ShizukuStatusCheckerImpl(mockContext, shizukuGateway)

        assertTrue(checker.hasPermission()) {
            "Expected hasPermission() to return true when checkSelfPermission() == PERMISSION_GRANTED"
        }
    }

    @Test
    fun `hasPermission returns false when Shizuku checkSelfPermission returns PERMISSION_DENIED`() {
        every { shizukuGateway.checkSelfPermission() } returns PackageManager.PERMISSION_DENIED

        val checker = ShizukuStatusCheckerImpl(mockContext, shizukuGateway)

        assertFalse(checker.hasPermission()) {
            "Expected hasPermission() to return false when checkSelfPermission() == PERMISSION_DENIED"
        }
    }

    @Test
    fun `hasPermission returns false when Shizuku checkSelfPermission throws`() {
        every { shizukuGateway.checkSelfPermission() } throws IllegalStateException("Shizuku not bound")

        val checker = ShizukuStatusCheckerImpl(mockContext, shizukuGateway)

        assertFalse(checker.hasPermission()) {
            "Expected hasPermission() to return false when checkSelfPermission() throws"
        }
    }
}
