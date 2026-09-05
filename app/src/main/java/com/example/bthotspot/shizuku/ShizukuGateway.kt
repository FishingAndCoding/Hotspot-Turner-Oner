package com.example.bthotspot.shizuku

import android.content.pm.PackageManager
import rikka.shizuku.Shizuku

interface ShizukuGateway {
    fun pingBinder(): Boolean
    fun checkSelfPermission(): Int
    fun requestPermission(requestCode: Int)
    fun getSystemService(name: String): android.os.IBinder
}

object DefaultShizukuGateway : ShizukuGateway {
    override fun pingBinder(): Boolean = Shizuku.pingBinder()

    override fun checkSelfPermission(): Int = Shizuku.checkSelfPermission()

    override fun requestPermission(requestCode: Int) = Shizuku.requestPermission(requestCode)

    override fun getSystemService(name: String): android.os.IBinder =
        rikka.shizuku.SystemServiceHelper.getSystemService(name)
}