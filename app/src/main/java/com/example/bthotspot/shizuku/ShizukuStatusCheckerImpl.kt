package com.example.bthotspot.shizuku

import android.content.Context
import android.content.pm.PackageManager
import rikka.shizuku.Shizuku

class ShizukuStatusCheckerImpl(
    private val context: Context,
    private val shizukuGateway: ShizukuGateway = DefaultShizukuGateway,
) : ShizukuStatusChecker {

    private companion object {
        const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"
    }

    override fun isInstalled(): Boolean {
        return try {
            context.packageManager.getPackageInfo(SHIZUKU_PACKAGE, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    override fun isRunning(): Boolean {
        return try {
            shizukuGateway.pingBinder()
        } catch (e: Exception) {
            false
        }
    }

    override fun hasPermission(): Boolean {
        return try {
            shizukuGateway.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (e: Exception) {
            false
        }
    }

    override fun requestPermission(requestCode: Int) {
        shizukuGateway.requestPermission(requestCode)
    }
}
