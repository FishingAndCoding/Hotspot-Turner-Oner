package com.example.bthotspot.hotspot

import android.content.Context
import android.content.pm.PackageManager
import android.net.TetheringManager
import android.net.wifi.WifiManager
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
        // Guard 1: no-op if hotspot is already on (Requirement 3.2)
        if (isHotspotEnabled()) return

        // Guard 2: Shizuku process must be alive (Requirement 3.3)
        val shizukuAlive = try {
            shizukuGateway.pingBinder()
        } catch (e: Exception) {
            false
        }
        if (!shizukuAlive) {
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
            false
        }
        if (!hasShizukuPermission) {
            notificationHelper.postErrorNotification(
                title = "Shizuku permission required",
                body  = "Shizuku is not active. Open Shizuku to re-activate.",
            )
            return
        }

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
            // 1. Wrap the raw "tethering" IBinder with Shizuku so all calls are
            //    executed with ADB-level identity.
            val wrappedBinder = ShizukuBinderWrapper(
                shizukuGateway.getSystemService("tethering")
            )

            // 2. Obtain the hidden ITetheringConnector proxy from the wrapped binder.
            val iTetheringConnectorStub =
                Class.forName("android.net.ITetheringConnector\$Stub")
            val asInterface =
                iTetheringConnectorStub.getMethod("asInterface", android.os.IBinder::class.java)
            val connector = asInterface.invoke(null, wrappedBinder)

            // 3. Build a TetheringRequest for TETHERING_WIFI via the builder.
            val requestBuilderClass =
                Class.forName("android.net.TetheringManager\$TetheringRequest\$Builder")
            val requestBuilder =
                requestBuilderClass.getConstructor(Int::class.java)
                    .newInstance(TetheringManager.TETHERING_WIFI)
            val request =
                requestBuilderClass.getMethod("build").invoke(requestBuilder)

            // 4. Build a dynamic proxy for the hidden StartTetheringCallback interface.
            val callbackClass =
                Class.forName("android.net.TetheringManager\$StartTetheringCallback")
            val callbackProxy = Proxy.newProxyInstance(
                callbackClass.classLoader,
                arrayOf(callbackClass),
            ) { _, method, args ->
                when (method.name) {
                    "onTetheringStarted" -> {
                        // Success — no user-visible action needed.
                    }
                    "onTetheringFailed" -> {
                        // Requirement 3.4: surface failure with error code to the user.
                        val errorCode = (args?.getOrNull(0) as? Int) ?: -1
                        notificationHelper.postErrorNotification(
                            title = "Hotspot could not be enabled",
                            body  = "Error code: $errorCode",
                        )
                    }
                }
                null
            }

            // 5. Invoke startTethering(TetheringRequest, Executor, StartTetheringCallback).
            val requestClass =
                Class.forName("android.net.TetheringManager\$TetheringRequest")
            connector.javaClass.getMethod(
                "startTethering",
                requestClass,
                java.util.concurrent.Executor::class.java,
                callbackClass,
            ).invoke(connector, request, Executors.newSingleThreadExecutor(), callbackProxy)

        } catch (e: Exception) {
            // Any reflection or IPC failure posts a notification rather than crashing.
            notificationHelper.postErrorNotification(
                title = "Hotspot could not be enabled",
                body  = "Could not invoke tethering API: ${e.message}",
            )
        }
    }
}
