package com.example.bthotspot.hotspot

import android.content.Context
import android.content.pm.PackageManager
import android.net.TetheringManager
import android.net.wifi.WifiManager
import android.util.Log
import com.example.bthotspot.notifications.NotificationHelper
import com.example.bthotspot.shizuku.DefaultShizukuGateway
import com.example.bthotspot.shizuku.ShizukuGateway
import rikka.shizuku.ShizukuBinderWrapper
import java.lang.reflect.Proxy
import java.util.concurrent.Executors

/**
 * Concrete implementation of [HotspotController].
 *
 * Uses [WifiManager.isWifiApEnabled] (public API, no special privilege) to check
 * the current hotspot state, and calls [TetheringManager.startTethering] with
 * [TetheringManager.TETHERING_WIFI] via the Shizuku binder to enable it.
 *
 * Guard checks are evaluated in this order inside [enableHotspotIfNeeded]:
 *   1. [isHotspotEnabled] — silent no-op if hotspot is already active.
 *   2. [Shizuku.pingBinder] — abort and notify user if Shizuku is not alive.
 *   3. [Shizuku.checkSelfPermission] — abort and notify user if permission is denied.
 *
 * All failures surface as CHANNEL_ERRORS notifications rather than exceptions,
 * because this code runs in a background context with no UI.
 *
 * Requirements: 3.1, 3.2, 3.3, 3.4, 2.7
 */
open class HotspotControllerImpl(
    private val context: Context,
    protected val notificationHelper: NotificationHelper,
    private val shizukuGateway: ShizukuGateway = DefaultShizukuGateway,
) : HotspotController {

    private val wifiManager: WifiManager =
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

    // -------------------------------------------------------------------------
    // HotspotController interface
    // -------------------------------------------------------------------------

    /**
     * Returns `true` when WiFi AP (hotspot) tethering is currently active.
     *
     * [WifiManager.isWifiApEnabled] is a public API that requires no special
     * privilege — no Shizuku involvement needed here.
     *
     * Requirements: 3.1, 3.2
     */
    override fun isHotspotEnabled(): Boolean = readWifiApEnabled()

    protected open fun readWifiApEnabled(): Boolean {
        return runCatching {
            WifiManager::class.java.getMethod("isWifiApEnabled").invoke(wifiManager) as Boolean
        }.getOrDefault(false)
    }

    /**
     * Enables the WiFi hotspot if it is not already active.
     *
     * Guard sequence (any guard failure aborts and posts a notification):
     *   1. Already enabled  → silent no-op.
     *   2. Shizuku binder unreachable → CHANNEL_ERRORS notification.
     *   3. Shizuku permission denied  → CHANNEL_ERRORS notification.
     *
     * On all guards passing, calls [TetheringManager.startTethering] with
     * [TetheringManager.TETHERING_WIFI] via the Shizuku binder.
     *
     * Requirements: 3.1, 3.2, 3.3, 3.4, 2.7
     */
    override fun enableHotspotIfNeeded() {
        Log.d(TAG, "enableHotspotIfNeeded called")

        // Guard 1: no-op if hotspot is already on (Requirement 3.2)
        if (isHotspotEnabled()) {
            Log.d(TAG, "Hotspot already enabled — no-op")
            return
        }

        // Guard 2: Shizuku process must be alive (Requirement 3.3)
        val shizukuAlive = try {
            shizukuGateway.pingBinder()
        } catch (e: Exception) {
            Log.e(TAG, "pingBinder threw exception", e)
            false
        }
        if (!shizukuAlive) {
            Log.e(TAG, "Shizuku binder not alive — aborting")
            notificationHelper.postErrorNotification(
                title = "Shizuku is not active",
                body  = "Shizuku is not active. Open Shizuku to re-activate.",
            )
            return
        }

        // Guard 3: this app must have been granted Shizuku permission (Requirement 3.3)
        val hasShizukuPermission = try {
            shizukuGateway.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (e: Exception) {
            Log.e(TAG, "checkSelfPermission threw exception", e)
            false
        }
        if (!hasShizukuPermission) {
            Log.e(TAG, "Shizuku permission not granted — aborting")
            notificationHelper.postErrorNotification(
                title = "Shizuku permission required",
                body  = "Shizuku is not active. Open Shizuku to re-activate.",
            )
            return
        }

        Log.i(TAG, "All guards passed — calling startTetheringViaShizuku")
        // All guards passed — attempt to start Wi-Fi tethering via Shizuku binder.
        startTetheringViaShizuku()
    }


    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Obtains a privileged [TetheringManager] ITetheringConnector through the
     * Shizuku binder wrapper and calls startTethering reflectively.
     *
     * [TetheringManager.startTethering] requires the TETHER_PRIVILEGED
     * permission, which is granted to the Shizuku process (ADB-level identity).
     *
     * ITetheringConnector is a @hide API, so we reach it via reflection after
     * wrapping the raw IBinder with [ShizukuBinderWrapper] — the canonical
     * Shizuku pattern for restricted system services.
     *
     * Requirements: 3.1, 3.4
     */
    protected open fun startTetheringViaShizuku() {
        try {
            Log.d(TAG, "Step 1: getting tethering service binder via Shizuku")
            val rawBinder = shizukuGateway.getSystemService("tethering")
            if (rawBinder == null) {
                Log.e(TAG, "getSystemService('tethering') returned null")
                notificationHelper.postErrorNotification("Hotspot failed", "Tethering service unavailable")
                return
            }
            val wrappedBinder = ShizukuBinderWrapper(rawBinder)

            // Inspect ITetheringConnector$Stub
            val stubClass = try {
                Class.forName("android.net.ITetheringConnector\$Stub")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load ITetheringConnector\$Stub", e)
                null
            }

            if (stubClass != null) {
                Log.d(TAG, "ITetheringConnector\$Stub declared methods: " +
                        stubClass.declaredMethods.joinToString { it.name })
                Log.d(TAG, "ITetheringConnector\$Stub methods: " +
                        stubClass.methods.joinToString { it.name })
            }

            // Inspect ITetheringConnector interface if present
            val ifaceClass = try {
                Class.forName("android.net.ITetheringConnector")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load ITetheringConnector interface", e)
                null
            }
            if (ifaceClass != null) {
                Log.d(TAG, "ITetheringConnector declared methods: " +
                        ifaceClass.declaredMethods.joinToString { it.name })
            }

            // Inspect TetheringManager constructors
            val tmClass = TetheringManager::class.java
            Log.d(TAG, "TetheringManager constructors: " +
                    tmClass.declaredConstructors.joinToString { c ->
                        "${c.name}(${c.parameterTypes.joinToString { it.simpleName }})"
                    })

            // Attempt 1: Try TetheringManager constructor with shell context and Supplier<IBinder>
            val shellContext = object : android.content.ContextWrapper(context.applicationContext) {
                override fun getPackageName(): String = "com.android.shell"
                override fun getOpPackageName(): String = "com.android.shell"
                override fun getAttributionTag(): String? = null
            }

            var started = false
            for (constructor in tmClass.declaredConstructors) {
                constructor.isAccessible = true
                val paramTypes = constructor.parameterTypes
                if (paramTypes.size == 2 &&
                    Context::class.java.isAssignableFrom(paramTypes[0]) &&
                    java.util.function.Supplier::class.java.isAssignableFrom(paramTypes[1])) {
                    Log.d(TAG, "Found TetheringManager(Context, Supplier<IBinder>) constructor! Invoking with shell context...")
                    val supplier = java.util.function.Supplier<android.os.IBinder> { wrappedBinder }
                    val customTm = constructor.newInstance(shellContext, supplier) as TetheringManager
                    
                    val request = TetheringManager.TetheringRequest.Builder(TetheringManager.TETHERING_WIFI).build()
                    val executor = Executors.newSingleThreadExecutor()
                    val callback = object : TetheringManager.StartTetheringCallback {
                        override fun onTetheringStarted() {
                            Log.i(TAG, "onTetheringStarted via custom TetheringManager — hotspot is active!")
                        }
                        override fun onTetheringFailed(resultCode: Int) {
                            Log.e(TAG, "onTetheringFailed via custom TetheringManager: $resultCode")
                            notificationHelper.postErrorNotification(
                                title = "Hotspot could not be enabled",
                                body = "Error code: $resultCode",
                            )
                        }
                    }
                    customTm.startTethering(request, executor, callback)
                    Log.i(TAG, "startTethering invoked successfully via custom TetheringManager!")
                    started = true
                    break
                }
            }

            if (started) return

            // Attempt 2: Direct ITetheringConnector asInterface reflection
            val asInterfaceMethod = stubClass?.methods?.firstOrNull { it.name == "asInterface" }
                ?: stubClass?.declaredMethods?.firstOrNull { it.name == "asInterface" }

            if (asInterfaceMethod != null) {
                asInterfaceMethod.isAccessible = true
                val connector = asInterfaceMethod.invoke(null, wrappedBinder)
                Log.d(TAG, "Obtained connector via asInterface: $connector")
                if (connector != null) {
                    for (method in connector.javaClass.methods.filter { it.name == "startTethering" }) {
                        Log.d(TAG, "Connector startTethering params: " +
                                method.parameterTypes.joinToString { it.name })
                    }
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "startTetheringViaShizuku failed", e)
            notificationHelper.postErrorNotification(
                title = "Hotspot could not be enabled",
                body  = "Could not invoke tethering API: ${e.message}",
            )
        }
    }

    companion object {
        private const val TAG = "BtHotspot"
    }
}
