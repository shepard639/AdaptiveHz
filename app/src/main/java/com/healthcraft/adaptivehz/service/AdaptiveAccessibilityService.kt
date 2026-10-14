package com.healthcraft.adaptivehz.service

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.healthcraft.adaptivehz.core.RefreshRateController
import com.healthcraft.adaptivehz.data.OperationMode
import com.healthcraft.adaptivehz.data.ProfileMode
import com.healthcraft.adaptivehz.data.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

enum class AdaptiveState {
    ACTIVE_TOUCH,  // 120Hz boost due to touch or scrolling
    IDLE_TIMEOUT,  // Dropped to 60Hz due to inactivity
    LOCKED_APP,    // Locked to 60Hz or 120Hz by app profile or fixed mode
    DISABLED       // Switched off
}

class AdaptiveAccessibilityService : AccessibilityService() {

    private lateinit var controller: RefreshRateController
    private lateinit var repository: SettingsRepository

    private val handler = Handler(Looper.getMainLooper())
    private var idleRunnable: Runnable? = null

    private var currentForegroundPackage: String = ""
    private var currentAppMode: ProfileMode = ProfileMode.ADAPTIVE

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())

    private var isCharging: Boolean = false
    private var isBatteryLow: Boolean = false
    private var isOverheating: Boolean = false

    private val powerAndBatteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    val mode = repository.operationMode.value
                    if (mode == OperationMode.ADAPTIVE || mode == OperationMode.ECO) {
                        val lowHz = repository.lowHzTarget.value
                        Log.d(TAG, "Screen OFF in $mode -> dropping to ${lowHz}Hz")
                        controller.dropToLow(lowHz)
                        AdaptiveCoordinator.setAdaptiveState(AdaptiveState.IDLE_TIMEOUT)
                    }
                }
                PowerManager.ACTION_POWER_SAVE_MODE_CHANGED -> {
                    controller.invalidateCache()
                    if (repository.isPsmBypassEnabled.value) {
                        val mode = repository.operationMode.value
                        val highHz = repository.highHzTarget.value
                        when (mode) {
                            OperationMode.FIXED_120 -> controller.bypassPowerSaving120Hz(highHz)
                            OperationMode.ADAPTIVE, OperationMode.ECO -> onUserInteraction()
                            else -> {}
                        }
                        // Anti-watchdog: One UI 6.0/6.1 DisplayPowerController resets asynchronously on PSM change.
                        // Re-assert refresh rate with debounce after 350ms.
                        handler.postDelayed({
                            val currentMode = repository.operationMode.value
                            if (repository.isPsmBypassEnabled.value) {
                                controller.invalidateCache()
                                if (currentMode == OperationMode.FIXED_120) {
                                    controller.bypassPowerSaving120Hz(highHz)
                                } else if (currentMode == OperationMode.ADAPTIVE || currentMode == OperationMode.ECO) {
                                    onUserInteraction()
                                }
                            }
                        }, 350L)
                    }
                }
                Intent.ACTION_POWER_CONNECTED -> {
                    isCharging = true
                    Log.d(TAG, "Charger CONNECTED -> Smart Charging evaluating")
                    val mode = repository.operationMode.value
                    if (mode == OperationMode.ADAPTIVE && repository.isSmartChargingEnabled.value) {
                        cancelIdleTimer()
                        controller.boostToHigh(repository.highHzTarget.value)
                        AdaptiveCoordinator.setAdaptiveState(AdaptiveState.ACTIVE_TOUCH)
                    } else if (mode == OperationMode.ADAPTIVE || mode == OperationMode.ECO) {
                        restartIdleTimer()
                    }
                }
                Intent.ACTION_POWER_DISCONNECTED -> {
                    isCharging = false
                    Log.d(TAG, "Charger DISCONNECTED -> Returning to adaptive")
                    val mode = repository.operationMode.value
                    if (mode == OperationMode.ADAPTIVE || mode == OperationMode.ECO) {
                        restartIdleTimer()
                    }
                }
                Intent.ACTION_BATTERY_CHANGED -> {
                    val level = intent.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1)
                    val scale = intent.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1)
                    val batteryPct = if (level >= 0 && scale > 0) (level * 100) / scale else 100
                    val tempTenths = intent.getIntExtra(android.os.BatteryManager.EXTRA_TEMPERATURE, 0)
                    val tempCelsius = tempTenths / 10.0f

                    // Thermal safeguard: > 41.5 C
                    val overheating = repository.isThermalProtectionEnabled.value && tempCelsius >= THERMAL_OVERHEAT_THRESHOLD_CELSIUS
                    // Low battery defender: < 20%
                    val batteryCritical = repository.isLowBatteryDefenderEnabled.value && !isCharging && batteryPct <= 20

                    val conditionChanged = (isOverheating != overheating) || (isBatteryLow != batteryCritical)
                    isOverheating = overheating
                    isBatteryLow = batteryCritical

                    if (conditionChanged) {
                        if (isOverheating || isBatteryLow) {
                            Log.w(TAG, "Safeguard engaged (Overheating: $isOverheating, LowBattery: $isBatteryLow) -> lock 60Hz")
                            cancelIdleTimer()
                            controller.dropToLow(60)
                            AdaptiveCoordinator.setAdaptiveState(AdaptiveState.LOCKED_APP)
                        } else {
                            onUserInteraction()
                        }
                    }
                }
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        AdaptiveCoordinator.setServiceActive(true)

        controller = RefreshRateController.getInstance(this)
        repository = SettingsRepository(this)

        // Validate and clamp configured targets against hardware panel capabilities
        val supportedRates = controller.getSupportedRefreshRates()
        val currentHigh = repository.highHzTarget.value
        val clampedHigh = com.healthcraft.adaptivehz.core.RefreshRateModeResolver.clampTargetHz(currentHigh, supportedRates, fallback = 120)
        if (currentHigh != clampedHigh) {
            Log.i(TAG, "Clamping unsupported highHzTarget $currentHigh to $clampedHigh")
            repository.setHighHzTarget(clampedHigh)
        }

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
            addAction(Intent.ACTION_BATTERY_CHANGED)
        }
        registerReceiver(powerAndBatteryReceiver, filter)

        observeOperationMode()
        observeUserInteractions()

        Log.i(TAG, "AdaptiveAccessibilityService connected successfully")
    }

    private fun observeOperationMode() {
        serviceScope.launch {
            repository.operationMode.collectLatest { mode ->
                Log.d(TAG, "Active OperationMode: $mode")
                when (mode) {
                    OperationMode.FIXED_120 -> {
                        cancelIdleTimer()
                        controller.setFixed120()
                        AdaptiveCoordinator.setAdaptiveState(AdaptiveState.LOCKED_APP)
                    }
                    OperationMode.FIXED_60 -> {
                        cancelIdleTimer()
                        controller.setFixed60()
                        AdaptiveCoordinator.setAdaptiveState(AdaptiveState.LOCKED_APP)
                    }
                    OperationMode.ADAPTIVE,
                    OperationMode.ECO -> {
                        onUserInteraction()
                    }
                }
            }
        }
    }

    private fun observeUserInteractions() {
        serviceScope.launch {
            AdaptiveCoordinator.userInteractionEvents.collect {
                onUserInteraction()
            }
        }
    }

    private var suppressInteractionUntil: Long = 0L

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val mode = repository.operationMode.value
        // If fixed mode, ignore touch-driven adaptive triggers
        if (mode == OperationMode.FIXED_120 || mode == OperationMode.FIXED_60) return

        val eventPkg = event.packageName?.toString() ?: ""

        // If event originates from AdaptiveHz itself, ignore it:
        // MainActivity.dispatchTouchEvent handles real user touches directly and accurately
        if (eventPkg == packageName && event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            return
        }

        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                if (eventPkg.isNotEmpty() && eventPkg != currentForegroundPackage && eventPkg != packageName) {
                    onForegroundAppChanged(eventPkg)
                } else if (eventPkg == "com.sec.android.app.launcher") {
                    // Samsung Launcher horizontal home screen page swipe
                    onUserInteraction()
                }
            }
            AccessibilityEvent.TYPE_TOUCH_INTERACTION_START,
            AccessibilityEvent.TYPE_GESTURE_DETECTION_START,
            AccessibilityEvent.TYPE_VIEW_CLICKED,
            AccessibilityEvent.TYPE_VIEW_LONG_CLICKED,
            AccessibilityEvent.TYPE_VIEW_SCROLLED -> {
                val now = System.currentTimeMillis()
                // If within transition suppression window, ignore synthetic scroll/view events
                if (now < suppressInteractionUntil && event.eventType != AccessibilityEvent.TYPE_TOUCH_INTERACTION_START) {
                    return
                }
                onUserInteraction()
            }
        }
    }

    override fun onInterrupt() {
        Log.w(TAG, "AdaptiveAccessibilityService interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        AdaptiveCoordinator.setServiceActive(false)
        serviceScope.cancel()
        try {
            unregisterReceiver(powerAndBatteryReceiver)
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering powerAndBatteryReceiver during cleanup", e)
        }
        cancelIdleTimer()
        Log.i(TAG, "AdaptiveAccessibilityService destroyed")
    }

    /**
     * Called when the active foreground application changes.
     */
    private fun onForegroundAppChanged(packageName: String) {
        currentForegroundPackage = packageName
        currentAppMode = repository.getProfileModeForApp(packageName)
        AdaptiveCoordinator.setActivePackageName(packageName)

        val mode = repository.operationMode.value
        if (mode == OperationMode.FIXED_120) {
            cancelIdleTimer()
            controller.setFixed120()
            AdaptiveCoordinator.setAdaptiveState(AdaptiveState.LOCKED_APP)
            return
        }
        if (mode == OperationMode.FIXED_60) {
            cancelIdleTimer()
            controller.setFixed60()
            AdaptiveCoordinator.setAdaptiveState(AdaptiveState.LOCKED_APP)
            return
        }

        Log.d(TAG, "Foreground app: $packageName, mode: $currentAppMode (Global: $mode)")

        when (currentAppMode) {
            ProfileMode.FORCE_60 -> {
                cancelIdleTimer()
                controller.dropToLow(repository.lowHzTarget.value)
                AdaptiveCoordinator.setAdaptiveState(AdaptiveState.LOCKED_APP)
            }
            ProfileMode.FORCE_120 -> {
                cancelIdleTimer()
                controller.boostToHigh(repository.highHzTarget.value)
                AdaptiveCoordinator.setAdaptiveState(AdaptiveState.LOCKED_APP)
            }
            ProfileMode.ADAPTIVE -> {
                onUserInteraction()
            }
        }
    }

    /**
     * Triggered on user touches, scrolls, or gestures.
     */
    fun onUserInteraction() {
        val mode = repository.operationMode.value
        Log.d(TAG, "onUserInteraction() called: mode=$mode, currentApp=$currentForegroundPackage, appMode=$currentAppMode, overheating=$isOverheating, batteryLow=$isBatteryLow")
        if (mode == OperationMode.FIXED_60) {
            controller.setFixed60()
            AdaptiveCoordinator.setAdaptiveState(AdaptiveState.LOCKED_APP)
            return
        }
        if (mode == OperationMode.FIXED_120) {
            controller.setFixed120()
            AdaptiveCoordinator.setAdaptiveState(AdaptiveState.LOCKED_APP)
            return
        }

        if (isOverheating || isBatteryLow) {
            controller.dropToLow(60)
            AdaptiveCoordinator.setAdaptiveState(AdaptiveState.LOCKED_APP)
            return
        }

        if (currentAppMode == ProfileMode.FORCE_60) {
            controller.dropToLow(repository.lowHzTarget.value)
            AdaptiveCoordinator.setAdaptiveState(AdaptiveState.LOCKED_APP)
            return
        }

        // Boost to high refresh rate immediately
        val highHz = repository.highHzTarget.value
        controller.boostToHigh(highHz)
        AdaptiveCoordinator.setAdaptiveState(AdaptiveState.ACTIVE_TOUCH)

        // If charging with smart charging, keep highHz constantly (only in ADAPTIVE, never in ECO)
        if (mode == OperationMode.ADAPTIVE && isCharging && repository.isSmartChargingEnabled.value) {
            cancelIdleTimer()
        } else {
            // Reset the idle countdown timer
            restartIdleTimer()
        }
    }

    private fun restartIdleTimer() {
        cancelIdleTimer()

        val mode = repository.operationMode.value
        if (mode == OperationMode.FIXED_120 || mode == OperationMode.FIXED_60) return

        // ECO mode uses aggressive 1.5s timeout; ADAPTIVE uses user slider (e.g. 5s)
        val timeoutMillis = if (mode == OperationMode.ECO) {
            ECO_IDLE_TIMEOUT_MS
        } else {
            repository.idleTimeoutSeconds.value * 1000L
        }

        idleRunnable = Runnable {
            val currentMode = repository.operationMode.value
            if (currentMode == OperationMode.ADAPTIVE || currentMode == OperationMode.ECO) {
                if (currentAppMode != ProfileMode.FORCE_120) {
                    val lowHz = repository.lowHzTarget.value
                    Log.d(TAG, "Idle timeout reached (${timeoutMillis}ms) in $currentMode -> dropping to ${lowHz}Hz")
                    suppressInteractionUntil = System.currentTimeMillis() + 600L
                    controller.dropToLow(lowHz)
                    AdaptiveCoordinator.setAdaptiveState(AdaptiveState.IDLE_TIMEOUT)
                }
            }
        }
        handler.postDelayed(idleRunnable!!, timeoutMillis)
    }

    fun cancelIdleTimer() {
        idleRunnable?.let { handler.removeCallbacks(it) }
        idleRunnable = null
    }

    companion object {
        private const val TAG = "AdaptiveAccService"

        val isServiceActive: StateFlow<Boolean>
            get() = AdaptiveCoordinator.isServiceActive

        val isServiceRunning: Boolean
            get() = isServiceActive.value

        fun isAccessibilityEnabled(context: Context): Boolean {
            if (isServiceRunning) return true
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false
            return enabledServices.contains(context.packageName)
        }

        val adaptiveState: StateFlow<AdaptiveState>
            get() = AdaptiveCoordinator.adaptiveState

        val activePackageName: StateFlow<String>
            get() = AdaptiveCoordinator.activePackageName

        const val THERMAL_OVERHEAT_THRESHOLD_CELSIUS = 41.5f
        const val ECO_IDLE_TIMEOUT_MS = 1500L
    }
}
