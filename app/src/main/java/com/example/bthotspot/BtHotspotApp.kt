package com.example.bthotspot

import android.app.Application
import android.os.Build
import android.util.Log
import org.lsposed.hiddenapibypass.HiddenApiBypass

class BtHotspotApp : Application() {
    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                HiddenApiBypass.addHiddenApiExemptions("")
                Log.d("BtHotspot", "HiddenApiBypass exemptions added successfully")
            } catch (e: Throwable) {
                Log.e("BtHotspot", "Failed to add HiddenApiBypass exemptions", e)
            }
        }
    }
}
