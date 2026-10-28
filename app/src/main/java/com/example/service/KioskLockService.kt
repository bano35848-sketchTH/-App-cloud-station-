package com.example.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.KioskPreferences
import com.example.state.KioskStateManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class KioskLockService : Service() {

    companion object {
        const val ACTION_START_LOCK = "com.example.action.START_LOCK"
        const val ACTION_STOP_LOCK = "com.example.action.STOP_LOCK"
        const val EXTRA_TARGET_PACKAGE = "extra_target_package"
        const val EXTRA_TARGET_APP_NAME = "extra_target_app_name"
        private const val CHANNEL_ID = "rapper_cloud_kiosk_channel"
        private const val NOTIFICATION_ID = 9001
    }

    private var windowManager: WindowManager? = null
    private var floatingButtonView: View? = null
    private var unlockDialogView: View? = null

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var watchdogJob: Job? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START_LOCK

        if (action == ACTION_STOP_LOCK) {
            stopLockAndSelf()
            return START_NOT_STICKY
        }

        val targetPackage = intent?.getStringExtra(EXTRA_TARGET_PACKAGE)
            ?: KioskPreferences(this).targetPackage
            ?: ""
        val targetAppName = intent?.getStringExtra(EXTRA_TARGET_APP_NAME)
            ?: KioskPreferences(this).targetAppName
            ?: "Target App"

        val notification = buildForegroundNotification(targetAppName)
        startForeground(NOTIFICATION_ID, notification)

        // Show floating tiny unlock overlay
        setupFloatingUnlockButton()

        // Start background watchdog
        startWatchdog(targetPackage)

        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Rapper Cloud Kiosk Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Monitors and locks device in Kiosk mode"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildForegroundNotification(targetAppName: String): Notification {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🔒 Rapper Cloud Kiosk Active")
            .setContentText("Screen locked to: $targetAppName. Tap tiny icon to unlock.")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupFloatingUnlockButton() {
        if (!Settings.canDrawOverlays(this)) return
        if (floatingButtonView != null) return

        val dpSize = dpToPx(44)
        val params = WindowManager.LayoutParams(
            dpSize,
            dpSize,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = dpToPx(12)
            y = dpToPx(48) // Safe margin below status bar
        }

        // Tiny floating button view
        val button = FrameLayout(this).apply {
            val bg = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#CC0A0F1D")) // Deep navy semi-transparent
                setStroke(dpToPx(1.5f), Color.parseColor("#8000E5FF")) // Glowing cyan border
            }
            background = bg
            elevation = dpToPx(8).toFloat()

            val icon = ImageView(this@KioskLockService).apply {
                setImageResource(android.R.drawable.ic_lock_lock)
                setColorFilter(Color.parseColor("#00E5FF"))
                val iconSize = dpToPx(20)
                val iconParams = FrameLayout.LayoutParams(iconSize, iconSize).apply {
                    gravity = Gravity.CENTER
                }
                layoutParams = iconParams
            }
            addView(icon)

            setOnClickListener {
                vibrate(40)
                showUnlockDialogOverlay()
            }
        }

        try {
            windowManager?.addView(button, params)
            floatingButtonView = button
        } catch (_: Exception) {
            // Error adding overlay
        }
    }

    @SuppressLint("SetTextI18n")
    private fun showUnlockDialogOverlay() {
        if (!Settings.canDrawOverlays(this)) return
        if (unlockDialogView != null) return

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_DIM_BEHIND or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            dimAmount = 0.75f
            gravity = Gravity.CENTER
        }

        val enteredPin = StringBuilder()

        // Root container (Dark frosted overlay)
        val root = FrameLayout(this).apply {
            setBackgroundColor(Color.parseColor("#CC000000"))
        }

        // Dialog Card Container
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            val pad = dpToPx(24)
            setPadding(pad, pad, pad, pad)
            val cardBg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(24).toFloat()
                setColor(Color.parseColor("#FF0F172A")) // Slate 900
                setStroke(dpToPx(2), Color.parseColor("#FF00E5FF")) // Neon Cyan border
            }
            background = cardBg
            val cardParams = FrameLayout.LayoutParams(
                dpToPx(340),
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER
            }
            layoutParams = cardParams
        }

        // Title
        val titleText = TextView(this).apply {
            text = "⚡ Rapper Cloud"
            textSize = 22f
            setTextColor(Color.parseColor("#00E5FF"))
            gravity = Gravity.CENTER
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }
        card.addView(titleText)

        // Subtitle
        val subText = TextView(this).apply {
            text = "Enter Secret PIN to Unlock"
            textSize = 14f
            setTextColor(Color.parseColor("#94A3B8"))
            gravity = Gravity.CENTER
            val p = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dpToPx(6)
                bottomMargin = dpToPx(16)
            }
            layoutParams = p
        }
        card.addView(subText)

        // PIN Dots Container
        val dotsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            val p = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dpToPx(20)
            }
            layoutParams = p
        }

        val dotViews = mutableListOf<View>()
        for (i in 0 until 4) {
            val dot = View(this).apply {
                val s = dpToPx(16)
                val dotParams = LinearLayout.LayoutParams(s, s).apply {
                    setMargins(dpToPx(8), 0, dpToPx(8), 0)
                }
                layoutParams = dotParams
                val bg = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(Color.parseColor("#334155"))
                }
                background = bg
            }
            dotViews.add(dot)
            dotsRow.addView(dot)
        }
        card.addView(dotsRow)

        // Error message view
        val errorText = TextView(this).apply {
            text = ""
            textSize = 13f
            setTextColor(Color.parseColor("#EF4444"))
            gravity = Gravity.CENTER
            val p = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dpToPx(12)
            }
            layoutParams = p
        }
        card.addView(errorText)

        // Function to update dots
        fun updateDots() {
            for (i in 0 until 4) {
                val dot = dotViews[i]
                val bg = dot.background as GradientDrawable
                if (i < enteredPin.length) {
                    bg.setColor(Color.parseColor("#00E5FF"))
                } else {
                    bg.setColor(Color.parseColor("#334155"))
                }
            }
        }

        fun onDigitAdded(digit: String) {
            if (enteredPin.length < 4) {
                enteredPin.append(digit)
                vibrate(30)
                errorText.text = ""
                updateDots()

                if (enteredPin.length == 4) {
                    // Check PIN
                    val pin = enteredPin.toString()
                    val isValid = KioskStateManager.verifyPin(this@KioskLockService, pin)
                    if (isValid) {
                        vibrate(80)
                        errorText.setTextColor(Color.parseColor("#10B981"))
                        errorText.text = "✓ Unlocked! Restoring normal mode..."
                        mainHandler.postDelayed({
                            dismissUnlockDialog()
                            KioskStateManager.unlock(this@KioskLockService)
                            // Launch MainActivity
                            val intent = Intent(this@KioskLockService, MainActivity::class.java).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                            }
                            startActivity(intent)
                        }, 500)
                    } else {
                        vibrate(300)
                        errorText.setTextColor(Color.parseColor("#EF4444"))
                        errorText.text = "Incorrect PIN. Try again."
                        mainHandler.postDelayed({
                            enteredPin.clear()
                            updateDots()
                        }, 600)
                    }
                }
            }
        }

        // Numeric Keypad Grid (3 cols x 4 rows)
        val keypadGrid = GridLayout(this).apply {
            columnCount = 3
            rowCount = 4
            alignmentMode = GridLayout.ALIGN_BOUNDS
            val p = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER_HORIZONTAL
            }
            layoutParams = p
        }

        val buttonLabels = listOf(
            "1", "2", "3",
            "4", "5", "6",
            "7", "8", "9",
            "✕", "0", "⌫"
        )

        for (label in buttonLabels) {
            val keyButton = Button(this).apply {
                text = label
                textSize = 20f
                setTextColor(Color.WHITE)
                val btnSize = dpToPx(64)
                val btnParams = GridLayout.LayoutParams().apply {
                    width = btnSize
                    height = btnSize
                    setMargins(dpToPx(6), dpToPx(6), dpToPx(6), dpToPx(6))
                }
                layoutParams = btnParams

                val keyBg = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    if (label == "✕" || label == "⌫") {
                        setColor(Color.parseColor("#1E293B"))
                    } else {
                        setColor(Color.parseColor("#1E293B"))
                        setStroke(dpToPx(1), Color.parseColor("#334155"))
                    }
                }
                background = keyBg

                setOnClickListener {
                    when (label) {
                        "✕" -> {
                            vibrate(25)
                            dismissUnlockDialog()
                        }
                        "⌫" -> {
                            if (enteredPin.isNotEmpty()) {
                                enteredPin.deleteCharAt(enteredPin.length - 1)
                                vibrate(25)
                                updateDots()
                                errorText.text = ""
                            }
                        }
                        else -> {
                            onDigitAdded(label)
                        }
                    }
                }
            }
            keypadGrid.addView(keyButton)
        }

        card.addView(keypadGrid)

        // Cancel / Back to Target App button
        val dismissButton = TextView(this).apply {
            text = "Back to Locked App"
            textSize = 14f
            setTextColor(Color.parseColor("#94A3B8"))
            gravity = Gravity.CENTER
            val p = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dpToPx(16)
            }
            layoutParams = p
            setPadding(dpToPx(12), dpToPx(8), dpToPx(12), dpToPx(8))
            setOnClickListener {
                dismissUnlockDialog()
            }
        }
        card.addView(dismissButton)

        root.addView(card)

        try {
            windowManager?.addView(root, params)
            unlockDialogView = root
        } catch (_: Exception) {
            // Failed to show overlay
        }
    }

    private fun dismissUnlockDialog() {
        unlockDialogView?.let {
            try {
                windowManager?.removeView(it)
            } catch (_: Exception) {}
            unlockDialogView = null
        }
    }

    private fun startWatchdog(targetPackage: String) {
        watchdogJob?.cancel()
        if (targetPackage.isEmpty()) return

        val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
        val ourPackage = packageName

        watchdogJob = serviceScope.launch {
            while (isActive) {
                delay(800)
                if (!KioskStateManager.isLocked.value) break

                // If unlock dialog is showing, do not intercept
                if (unlockDialogView != null) continue

                // Check foreground package via UsageStats
                if (usageStatsManager != null && Settings.canDrawOverlays(this@KioskLockService)) {
                    val time = System.currentTimeMillis()
                    val stats = usageStatsManager.queryUsageStats(
                        UsageStatsManager.INTERVAL_DAILY,
                        time - 1000 * 5,
                        time
                    )
                    if (!stats.isNullOrEmpty()) {
                        val currentApp = stats.maxByOrNull { it.lastTimeUsed }?.packageName
                        if (currentApp != null &&
                            currentApp != targetPackage &&
                            currentApp != ourPackage &&
                            !currentApp.contains("inputmethod", ignoreCase = true) &&
                            !currentApp.contains("keyboard", ignoreCase = true)
                        ) {
                            // User slipped out! Bring target app back
                            bringTargetToFront(targetPackage)
                        }
                    }
                }
            }
        }
    }

    private fun bringTargetToFront(targetPackage: String) {
        try {
            val intent = packageManager.getLaunchIntentForPackage(targetPackage)?.apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP
                )
            }
            if (intent != null) {
                startActivity(intent)
            }
        } catch (_: Exception) {}
    }

    private fun stopLockAndSelf() {
        watchdogJob?.cancel()
        dismissUnlockDialog()
        floatingButtonView?.let {
            try {
                windowManager?.removeView(it)
            } catch (_: Exception) {}
            floatingButtonView = null
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        watchdogJob?.cancel()
        dismissUnlockDialog()
        floatingButtonView?.let {
            try {
                windowManager?.removeView(it)
            } catch (_: Exception) {}
            floatingButtonView = null
        }
    }

    private fun dpToPx(dp: Float): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp,
            resources.displayMetrics
        ).toInt()
    }

    private fun dpToPx(dp: Int): Int = dpToPx(dp.toFloat())

    private fun vibrate(durationMs: Long) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                @Suppress("DEPRECATION")
                vibrator?.vibrate(durationMs)
            }
        } catch (_: Exception) {}
    }
}
