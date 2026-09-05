package com.example.bthotspot.hotspot

interface HotspotController {
    /**
     * Enables WiFi hotspot if not already active.
     * Posts a notification on any failure rather than throwing.
     */
    fun enableHotspotIfNeeded()

    /** Returns true if the WiFi hotspot tethering is currently active. */
    fun isHotspotEnabled(): Boolean
}
