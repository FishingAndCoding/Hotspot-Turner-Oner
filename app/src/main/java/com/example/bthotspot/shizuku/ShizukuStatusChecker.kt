package com.example.bthotspot.shizuku

interface ShizukuStatusChecker {
    /** Returns true if Shizuku is installed on the device. */
    fun isInstalled(): Boolean

    /** Returns true if the Shizuku service is running and reachable. */
    fun isRunning(): Boolean

    /** Returns true if this app has been granted Shizuku permission. */
    fun hasPermission(): Boolean

    /** Requests Shizuku permission from the user (shows Shizuku's permission dialog). */
    fun requestPermission(requestCode: Int)
}
