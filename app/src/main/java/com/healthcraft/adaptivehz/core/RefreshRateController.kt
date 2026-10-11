package com.healthcraft.adaptivehz.core

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.display.DisplayManager
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Display
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.roundToInt

class RefreshRateController private constructor(private val appContext: Context) : DisplayManager.DisplayListener {

    private val contentResolver = appContext.contentResolver
    private val displayManager = appContext.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _currentRefreshRateHz = MutableStateFlow(getHardwareRefreshRateHz())
    val currentRefreshRateHz: StateFlow<Int> = _currentRefreshRateHz.asStateFlow()

    @Volatile
    private var lastAppliedTargetHz: Int = -1
    @Volatile
    private var lastAppliedMinHz: Int = -1

    init {
        // Register display listener to track real hardware SurfaceFlinger mode changes
        displayManager?.registerDisplayListener(this, mainHandler)
    }

    override fun onDisplayAdded(displayId: Int) {}
    override fun onDisplayRemoved(displayId: Int) {}

    override fun onDisplayChanged(displayId: Int) {
        if (displayId == Display.DEFAULT_DISPLAY) {
            val hwRate = getHardwareRefreshRateHz()
            Log.d(TAG, "Hardware DisplayListener changed: $hwRate Hz")
            _currentRefreshRateHz.value = hwRate
            if (hwRate != lastAppliedTargetHz && lastAppliedTargetHz != -1) {
                // Keep cache aligned with external or system power saving changes
                lastAppliedTargetHz = hwRate
            }
        }
    }

    /**
     * Checks if WRITE_SECURE_SETTINGS permission is granted.
     */
    fun hasWriteSecureSettings(): Boolean {
        return appContext.checkCallingOrSelfPermission(android.Manifest.permission.WRITE_SECURE_SETTINGS) ==
                PackageManager.PERMISSION_GRANTED
    }

    /**
     * Queries display modes from the device's default display to discover available refresh rates.
     */
    fun getSupportedModes(): List<DisplayModeInfo> {
        val display = displayManager?.getDisplay(Display.DEFAULT_DISPLAY) ?: return listOf(
            DisplayModeInfo(1, 1080, 2400, 60, 60.0f),
            DisplayModeInfo(2, 1080, 2400, 120, 120.0f)
        )

        return display.supportedModes
            .map { mode ->
                DisplayModeInfo(
                    modeId = mode.modeId,
                    width = mode.physicalWidth,
                    height = mode.physicalHeight,
                    refreshRateHz = mode.refreshRate.roundToInt(),
                    exactRefreshRate = mode.refreshRate
                )
            }
            .distinctBy { it.refreshRateHz }
            .sortedBy { it.refreshRateHz }
    }

    /**
     * Retrieves unique available refresh rates in Hz (e.g., [60, 120]).
     */
    fun getSupportedRefreshRates(): List<Int> {
        val modes = getSupportedModes().map { it.refreshRateHz }
        return if (modes.isNotEmpty()) modes else listOf(60, 120)
    }

    /**
     * Invalidate cached settings state to force re-application when needed.
     */
    fun invalidateCache() {
        lastAppliedTargetHz = -1
        lastAppliedMinHz = -1
    }

    /**
     * Applies target refresh rate to Samsung One UI Secure table and Android System table.
     * Includes caching to eliminate redundant IPC and SQLite writes during active touch streams.
     */
    @Synchronized
    fun setRefreshRate(targetHz: Int, minHz: Int = 60, force: Boolean = false): Boolean {
        if (!hasWriteSecureSettings()) {
            Log.w(TAG, "Cannot set refresh rate: WRITE_SECURE_SETTINGS not granted.")
            return false
        }

        if (!force && targetHz == lastAppliedTargetHz && minHz == lastAppliedMinHz) {
            // Display is already configured at this refresh rate.
            // Avoid redundant binder IPC and SettingsProvider disk transactions.
            return true
        }

        val peakFloat = targetHz.toFloat()
        val minFloat = minHz.toFloat()
        var success = true

        // 1. Samsung One UI refresh_rate_mode in Settings.Secure (0 = 60Hz, 1 = 120Hz)
        try {
            val samsungMode = RefreshRateModeResolver.resolveSamsungMode(targetHz)
            Settings.Secure.putInt(contentResolver, KEY_SAMSUNG_REFRESH_RATE_MODE, samsungMode)
            Log.d(TAG, "Successfully set Settings.Secure refresh_rate_mode to $samsungMode")

            // 1b. Samsung Galaxy Fold / Flip cover screen support
            if (isSamsungFoldable()) {
                Settings.Secure.putInt(contentResolver, KEY_SAMSUNG_SUB_REFRESH_RATE_MODE, samsungMode)
                Log.d(TAG, "Successfully set Settings.Secure sub_refresh_rate_mode to $samsungMode")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting Samsung refresh_rate_mode in Secure: ${e.message}")
            success = false
        }

        // 2. Android peak and min refresh rates in Settings.Secure
        try {
            Settings.Secure.putFloat(contentResolver, KEY_PEAK_REFRESH_RATE, peakFloat)
            Settings.Secure.putFloat(contentResolver, KEY_MIN_REFRESH_RATE, minFloat)
            Log.d(TAG, "Successfully set Settings.Secure peak=$peakFloat, min=$minFloat")
        } catch (e: Exception) {
            Log.w(TAG, "Error writing Settings.Secure: ${e.message}")
        }

        // 3. Android peak and min refresh rates in Settings.Global
        try {
            Settings.Global.putFloat(contentResolver, KEY_PEAK_REFRESH_RATE, peakFloat)
            Settings.Global.putFloat(contentResolver, KEY_MIN_REFRESH_RATE, minFloat)
            Log.d(TAG, "Successfully set Settings.Global peak=$peakFloat, min=$minFloat")
        } catch (e: Exception) {
            Log.w(TAG, "Error writing Settings.Global: ${e.message}")
        }

        if (success) {
            lastAppliedTargetHz = targetHz
            lastAppliedMinHz = minHz
            Log.d(TAG, "Applied refresh rate request: $targetHz Hz (min: $minHz Hz)")
            _currentRefreshRateHz.value = targetHz
        } else {
            Log.w(TAG, "Failed to apply refresh rate request: $targetHz Hz")
        }
        return success
    }

    /**
     * Unregisters the hardware display listener to prevent resource retention.
     */
    fun release() {
        try {
            displayManager?.unregisterDisplayListener(this)
            Log.d(TAG, "DisplayListener unregistered in release()")
        } catch (e: Exception) {
            Log.w(TAG, "Error unregistering DisplayListener in release()", e)
        }
    }

    /**
     * Reads current active system/hardware refresh rate in Hz.
     */
    fun getHardwareRefreshRateHz(): Int {
        try {
            val display = displayManager?.getDisplay(Display.DEFAULT_DISPLAY)
            val rate = display?.refreshRate?.roundToInt()
            if (rate != null && rate > 0) return rate

            val samsungMode = Settings.Secure.getInt(contentResolver, KEY_SAMSUNG_REFRESH_RATE_MODE, -1)
            return RefreshRateModeResolver.resolveHzFromSamsungMode(samsungMode, 120)
        } catch (e: Exception) {
            Log.w(TAG, "Error reading hardware refresh rate: ${e.message}")
        }
        return 120
    }

    fun setFixed120(force: Boolean = true): Boolean {
        return setRefreshRate(targetHz = 120, minHz = 120, force = force)
    }

    fun setFixed60(force: Boolean = true): Boolean {
        return setRefreshRate(targetHz = 60, minHz = 60, force = force)
    }

    fun boostToHigh(highHz: Int = 120, force: Boolean = false): Boolean {
        return setRefreshRate(targetHz = highHz, minHz = highHz, force = force)
    }

    fun dropToLow(lowHz: Int = 60, force: Boolean = false): Boolean {
        return setRefreshRate(targetHz = lowHz, minHz = lowHz, force = force)
    }

    fun bypassPowerSaving120Hz(targetHz: Int = 120): Boolean {
        val (peak, min) = RefreshRateModeResolver.calculatePsmBypassParameters(targetHz)
        return setRefreshRate(targetHz = peak.toInt(), minHz = min.toInt(), force = true)
    }

    fun isSamsungFoldable(): Boolean {
        val hasHinge = try {
            appContext.packageManager.hasSystemFeature("android.hardware.sensor.hinge_angle")
        } catch (e: Exception) {
            false
        }
        return RefreshRateModeResolver.isFoldableDevice(android.os.Build.MODEL, hasHinge)
    }

    companion object {
        private const val TAG = "RefreshRateController"

        const val KEY_PEAK_REFRESH_RATE = "peak_refresh_rate"
        const val KEY_MIN_REFRESH_RATE = "min_refresh_rate"
        const val KEY_SAMSUNG_REFRESH_RATE_MODE = "refresh_rate_mode"
        const val KEY_SAMSUNG_SUB_REFRESH_RATE_MODE = "sub_refresh_rate_mode"

        const val SAMSUNG_MODE_STANDARD = RefreshRateModeResolver.SAMSUNG_MODE_STANDARD // 0 (60Hz)
        const val SAMSUNG_MODE_HIGH = RefreshRateModeResolver.SAMSUNG_MODE_HIGH         // 1 (120Hz)

        @Volatile
        private var instance: RefreshRateController? = null

        fun getInstance(context: Context): RefreshRateController {
            return instance ?: synchronized(this) {
                instance ?: RefreshRateController(context.applicationContext).also { instance = it }
            }
        }
    }
}
