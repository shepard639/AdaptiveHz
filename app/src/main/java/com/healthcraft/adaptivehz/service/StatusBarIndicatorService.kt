package com.healthcraft.adaptivehz.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.IconCompat
import com.healthcraft.adaptivehz.R
import com.healthcraft.adaptivehz.core.RefreshRateController
import com.healthcraft.adaptivehz.data.ProfileMode
import com.healthcraft.adaptivehz.data.SettingsRepository
import com.healthcraft.adaptivehz.ui.MainActivity
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class StatusBarIndicatorService : Service() {

    private lateinit var controller: RefreshRateController
    private lateinit var repository: SettingsRepository
    private lateinit var notificationManager: NotificationManager
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())

    override fun onCreate() {
        super.onCreate()
        controller = RefreshRateController.getInstance(this)
        repository = SettingsRepository(this)
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        createNotificationChannel()

        val initialHz = controller.currentRefreshRateHz.value
        startForeground(NOTIFICATION_ID, buildNotification(initialHz, createStatusIconBitmap(initialHz)))

        observeRefreshRate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.action?.let { action ->
            when (action) {
                ACTION_FORCE_60 -> {
                    repository.setOperationMode(com.healthcraft.adaptivehz.data.OperationMode.FIXED_60)
                }
                ACTION_ADAPTIVE -> {
                    repository.setOperationMode(com.healthcraft.adaptivehz.data.OperationMode.ADAPTIVE)
                }
                ACTION_FORCE_120 -> {
                    repository.setOperationMode(com.healthcraft.adaptivehz.data.OperationMode.FIXED_120)
                }
            }
        }
        return START_STICKY
    }

    private fun observeRefreshRate() {
        serviceScope.launch {
            controller.currentRefreshRateHz.collectLatest { hz ->
                val iconBitmap = withContext(Dispatchers.Default) {
                    createStatusIconBitmap(hz)
                }
                val notification = buildNotification(hz, iconBitmap)
                notificationManager.notify(NOTIFICATION_ID, notification)
            }
        }
    }

    private fun buildNotification(hz: Int, dynamicIconBitmap: Bitmap): Notification {
        val openIntent = Intent(this, MainActivity::class.java)
        val pendingOpen = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        // Actions: 60Hz, Adaptive, 120Hz
        val force60Intent = Intent(this, StatusBarIndicatorService::class.java).apply { action = ACTION_FORCE_60 }
        val p60 = PendingIntent.getService(this, 1, force60Intent, PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0))

        val adaptiveIntent = Intent(this, StatusBarIndicatorService::class.java).apply { action = ACTION_ADAPTIVE }
        val pAdaptive = PendingIntent.getService(this, 2, adaptiveIntent, PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0))

        val force120Intent = Intent(this, StatusBarIndicatorService::class.java).apply { action = ACTION_FORCE_120 }
        val p120 = PendingIntent.getService(this, 3, force120Intent, PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0))

        val mode = repository.operationMode.value
        val stateText = when (mode) {
            com.healthcraft.adaptivehz.data.OperationMode.FIXED_120 -> getString(R.string.state_desc_fixed_120)
            com.healthcraft.adaptivehz.data.OperationMode.FIXED_60 -> getString(R.string.state_desc_fixed_60)
            com.healthcraft.adaptivehz.data.OperationMode.ECO -> if (hz > 60) getString(R.string.state_desc_eco_touch) else getString(R.string.state_desc_eco_idle)
            com.healthcraft.adaptivehz.data.OperationMode.ADAPTIVE -> if (hz > 60) getString(R.string.state_desc_adaptive_touch) else getString(R.string.state_desc_adaptive_idle)
        }

        val iconCompat = IconCompat.createWithBitmap(dynamicIconBitmap)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_status_title, hz))
            .setContentText(stateText)
            .setSmallIcon(iconCompat)
            .setContentIntent(pendingOpen)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setShowWhen(false)
            .addAction(0, getString(R.string.notification_action_60), p60)
            .addAction(0, getString(R.string.notification_action_adaptive), pAdaptive)
            .addAction(0, getString(R.string.notification_action_120), p120)
            .build()
    }

    private val iconCache = java.util.concurrent.ConcurrentHashMap<Int, Bitmap>()

    /**
     * Creates a high-contrast numeric bitmap with an upper-right "Hz" index (e.g. 60ᴴᶻ / 120ᴴᶻ)
     * for the Android status bar tray, matching the system clock and weather degree style.
     */
    private fun createStatusIconBitmap(hz: Int): Bitmap {
        iconCache[hz]?.let { return it }

        val size = 96
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val numText = "$hz"
        val unitText = "Hz"

        val numPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            if (hz >= 100) {
                textScaleX = 0.56f
                letterSpacing = -0.04f
            } else {
                textScaleX = 0.85f
                letterSpacing = 0f
            }
        }

        val unitPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textScaleX = 0.80f
            letterSpacing = -0.02f
        }

        val maxAllowedWidth = size * 0.90f
        val targetNumHeight = size * 0.68f
        val numBounds = Rect()
        val unitBounds = Rect()
        val gap = 2f

        var low = 15f
        var high = 110f
        var bestNumSize = low
        while (high - low > 0.5f) {
            val mid = (low + high) / 2f
            numPaint.textSize = mid
            numPaint.getTextBounds(numText, 0, numText.length, numBounds)

            unitPaint.textSize = mid * 0.38f
            unitPaint.getTextBounds(unitText, 0, unitText.length, unitBounds)

            val totalW = numBounds.width() + gap + unitBounds.width()

            if (numBounds.height() <= targetNumHeight && totalW <= maxAllowedWidth) {
                bestNumSize = mid
                low = mid
            } else {
                high = mid
            }
        }
        numPaint.textSize = bestNumSize
        numPaint.getTextBounds(numText, 0, numText.length, numBounds)

        unitPaint.textSize = bestNumSize * 0.38f
        unitPaint.getTextBounds(unitText, 0, unitText.length, unitBounds)

        val totalWidth = numBounds.width() + gap + unitBounds.width()

        val startX = (size - totalWidth) / 2f
        val numX = startX - numBounds.left
        val numY = (size - numBounds.height()) / 2f - numBounds.top

        val unitX = startX + numBounds.width() + gap - unitBounds.left
        val unitY = (numY + numBounds.top) - unitBounds.top

        canvas.drawText(numText, numX, numY, numPaint)
        canvas.drawText(unitText, unitX, unitY, unitPaint)

        Log.i("StatusBarIndicator", "hz=$hz: totalW=$totalWidth, numH=${numBounds.height()}, unitH=${unitBounds.height()}")

        iconCache[hz] = bitmap
        return bitmap
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.status_bar_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.status_bar_channel_desc)
            setShowBadge(false)
        }
        notificationManager.createNotificationChannel(channel)
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        iconCache.clear()
        try {
            notificationManager.cancel(NOTIFICATION_ID)
        } catch (e: Exception) {
            Log.e("StatusBarIndicator", "Error cancelling notification during onDestroy", e)
        }
        Log.i("StatusBarIndicator", "StatusBarIndicatorService destroyed")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val CHANNEL_ID = NotificationChannels.STATUS_BAR_CHANNEL_ID
        const val NOTIFICATION_ID = 2002

        const val ACTION_FORCE_60 = "com.healthcraft.adaptivehz.ACTION_FORCE_60"
        const val ACTION_ADAPTIVE = "com.healthcraft.adaptivehz.ACTION_ADAPTIVE"
        const val ACTION_FORCE_120 = "com.healthcraft.adaptivehz.ACTION_FORCE_120"

        fun start(context: Context) {
            val intent = Intent(context, StatusBarIndicatorService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, StatusBarIndicatorService::class.java)
            context.stopService(intent)
        }
    }
}
