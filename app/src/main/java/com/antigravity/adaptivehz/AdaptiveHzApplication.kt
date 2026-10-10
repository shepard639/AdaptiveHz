package com.antigravity.adaptivehz

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import com.antigravity.adaptivehz.service.NotificationChannels

class AdaptiveHzApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        val manager = getSystemService(NotificationManager::class.java) ?: return

        // 1. HUD floating overlay notification channel
        val hudChannel = NotificationChannel(
            NotificationChannels.HUD_CHANNEL_ID,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.notification_channel_desc)
            setShowBadge(false)
        }
        manager.createNotificationChannel(hudChannel)

        // 2. Status bar refresh rate indicator channel
        val statusBarChannel = NotificationChannel(
            NotificationChannels.STATUS_BAR_CHANNEL_ID,
            getString(R.string.status_bar_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.status_bar_channel_desc)
            setShowBadge(false)
        }
        manager.createNotificationChannel(statusBarChannel)
    }

    companion object {
        const val HUD_NOTIFICATION_CHANNEL_ID = NotificationChannels.HUD_CHANNEL_ID
    }
}
