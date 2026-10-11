package com.healthcraft.adaptivehz.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import com.healthcraft.adaptivehz.core.RefreshRateController
import com.healthcraft.adaptivehz.data.OperationMode
import com.healthcraft.adaptivehz.data.SettingsRepository
import com.healthcraft.adaptivehz.service.FloatingHudService
import com.healthcraft.adaptivehz.service.StatusBarIndicatorService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Handles device boot (ACTION_BOOT_COMPLETED) and app update (ACTION_MY_PACKAGE_REPLACED)
 * to restore user-configured display refresh rate mode, status bar indicator, and floating HUD overlay.
 */
class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return
        val action = intent.action
        if (action != Intent.ACTION_BOOT_COMPLETED && action != Intent.ACTION_MY_PACKAGE_REPLACED) {
            return
        }

        Log.i(TAG, "Received broadcast action: $action. Restoring AdaptiveHz state...")

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repository = SettingsRepository.getInstance(context)
                val controller = RefreshRateController.getInstance(context)

                // 1. Re-apply display refresh rate mode if WRITE_SECURE_SETTINGS is granted
                if (controller.hasWriteSecureSettings()) {
                    val mode = repository.operationMode.value
                    Log.d(TAG, "Restoring display operation mode on boot: $mode")
                    when (mode) {
                        OperationMode.FIXED_120 -> controller.setFixed120()
                        OperationMode.FIXED_60 -> controller.setFixed60()
                        OperationMode.ADAPTIVE,
                        OperationMode.ECO -> {
                            controller.boostToHigh(repository.highHzTarget.value)
                        }
                    }
                } else {
                    Log.w(TAG, "WRITE_SECURE_SETTINGS is not granted, skipping refresh rate configuration on boot.")
                }

                // 2. Restore StatusBarIndicatorService if enabled
                val isStatusBarEnabled = repository.isStatusBarIndicatorEnabled.value
                val hasNotificationPermission = NotificationManagerCompat.from(context).areNotificationsEnabled()
                if (isStatusBarEnabled && hasNotificationPermission) {
                    Log.d(TAG, "Restoring StatusBarIndicatorService on boot")
                    StatusBarIndicatorService.start(context)
                }

                // 3. Restore FloatingHudService if enabled
                val isHudEnabled = repository.isHudEnabled.value
                val hasOverlayPermission = Settings.canDrawOverlays(context)
                if (isHudEnabled && hasOverlayPermission) {
                    Log.d(TAG, "Restoring FloatingHudService on boot")
                    FloatingHudService.start(context)
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error restoring state in BootCompletedReceiver", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "BootCompletedReceiver"
    }
}
