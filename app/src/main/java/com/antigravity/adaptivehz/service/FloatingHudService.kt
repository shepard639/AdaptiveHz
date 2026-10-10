package com.antigravity.adaptivehz.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.antigravity.adaptivehz.AdaptiveHzApplication
import com.antigravity.adaptivehz.R
import com.antigravity.adaptivehz.core.RefreshRateController
import com.antigravity.adaptivehz.ui.MainActivity
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class FloatingHudService : Service() {

    private lateinit var windowManager: WindowManager
    private var hudView: TextView? = null
    private lateinit var controller: RefreshRateController

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f

    override fun onCreate() {
        super.onCreate()
        if (!Settings.canDrawOverlays(this)) {
            Log.w(TAG, "Cannot start FloatingHudService: SYSTEM_ALERT_WINDOW permission missing")
            stopSelf()
            return
        }

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        controller = RefreshRateController.getInstance(this)

        startForegroundNotification()
        createFloatingHud()
        observeRefreshRate()
    }

    private fun startForegroundNotification() {
        val openIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val notification: Notification = NotificationCompat.Builder(this, NotificationChannels.HUD_CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setSmallIcon(R.drawable.ic_tile_hz)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    private fun createFloatingHud() {
        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 40
            y = 180
        }

        val limeColor = androidx.core.content.ContextCompat.getColor(this, R.color.indicator_120hz)
        val mintColor = androidx.core.content.ContextCompat.getColor(this, R.color.indicator_60hz)

        val initialHz = controller.currentRefreshRateHz.value
        val initialColor = if (initialHz <= 60) mintColor else limeColor

        val textView = TextView(this).apply {
            text = "$initialHz Hz"
            setTextColor(initialColor)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setPadding(dpToPx(12), dpToPx(6), dpToPx(12), dpToPx(6))
            background = createPillBackground(initialColor)
            elevation = dpToPx(6).toFloat()
        }

        // Draggable and clickable touch listener
        textView.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - initialTouchX).toInt()
                    params.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager.updateViewLayout(view, params)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val diffX = Math.abs(event.rawX - initialTouchX)
                    val diffY = Math.abs(event.rawY - initialTouchY)
                    if (diffX < 15 && diffY < 15) {
                        // Quick tap detected: toggle profile for current active app
                        val pkg = AdaptiveCoordinator.activePackageName.value
                        if (pkg.isNotEmpty() && pkg != packageName) {
                            val repo = com.antigravity.adaptivehz.data.SettingsRepository(this)
                            val currentMode = repo.getProfileModeForApp(pkg)
                            if (currentMode == com.antigravity.adaptivehz.data.ProfileMode.FORCE_60) {
                                repo.setAppProfileMode(pkg, com.antigravity.adaptivehz.data.ProfileMode.ADAPTIVE)
                                android.widget.Toast.makeText(this, getString(R.string.hud_toast_adaptive_for_app, pkg), android.widget.Toast.LENGTH_SHORT).show()
                                AdaptiveCoordinator.notifyUserInteraction()
                            } else {
                                repo.setAppProfileMode(pkg, com.antigravity.adaptivehz.data.ProfileMode.FORCE_60)
                                android.widget.Toast.makeText(this, getString(R.string.hud_toast_force60_for_app, pkg), android.widget.Toast.LENGTH_SHORT).show()
                                controller.dropToLow(repo.lowHzTarget.value)
                            }
                        } else {
                            android.widget.Toast.makeText(this, getString(R.string.hud_toast_hint), android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                    true
                }
                else -> false
            }
        }

        hudView = textView
        windowManager.addView(textView, params)
    }

    private fun observeRefreshRate() {
        val limeColor = androidx.core.content.ContextCompat.getColor(this, R.color.indicator_120hz)
        val mintColor = androidx.core.content.ContextCompat.getColor(this, R.color.indicator_60hz)

        serviceScope.launch {
            controller.currentRefreshRateHz.collectLatest { hz ->
                hudView?.let { view ->
                    view.text = "$hz Hz"
                    if (hz <= 60) {
                        view.setTextColor(mintColor)
                        view.background = createPillBackground(mintColor)
                    } else {
                        view.setTextColor(limeColor)
                        view.background = createPillBackground(limeColor)
                    }
                }
            }
        }
    }

    private fun createPillBackground(strokeColor: Int): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dpToPx(16).toFloat()
            setColor(Color.parseColor("#E6161E19")) // 90% opacity deep sage
            setStroke(dpToPx(1), strokeColor)
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        hudView?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {
                Log.w(TAG, "Error removing HUD view from windowManager during cleanup", e)
            }
        }
        Log.i(TAG, "FloatingHudService destroyed")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TAG = "FloatingHudService"
        private const val NOTIFICATION_ID = 1001

        fun start(context: Context) {
            val intent = Intent(context, FloatingHudService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, FloatingHudService::class.java)
            context.stopService(intent)
        }
    }
}
