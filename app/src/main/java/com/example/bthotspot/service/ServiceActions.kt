package com.example.bthotspot.service

data class ServiceNotificationContent(
    val appName: String,    // e.g. "Bluetooth Hotspot Enabler"
    val statusText: String  // "Monitoring" | "Stopped"
)

object ServiceActions {
    const val ACTION_START = "com.example.bthotspot.START"
    const val ACTION_STOP  = "com.example.bthotspot.STOP"
}
