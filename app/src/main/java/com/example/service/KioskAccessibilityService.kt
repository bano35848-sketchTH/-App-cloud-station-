package com.example.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import com.example.data.KioskPreferences
import com.example.state.KioskStateManager

class KioskAccessibilityService : AccessibilityService() {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var lastInterceptTime = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        KioskStateManager.init(applicationContext)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val eventType = event.eventType
        if (eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            eventType != AccessibilityEvent.TYPE_WINDOWS_CHANGED
        ) {
            return
        }

        val eventPackage = event.packageName?.toString() ?: return

        // Check if Kiosk is active
        val prefs = KioskPreferences(applicationContext)
        val isLocked = prefs.isLocked || KioskStateManager.isLocked.value
        val targetPackage = prefs.targetPackage ?: KioskStateManager.lockedPackage.value

        if (!isLocked || targetPackage.isNullOrEmpty()) {
            return
        }

        // Allowed packages:
        // 1. The target locked app
        // 2. Rapper Cloud app (for unlock dialog / main activity)
        // 3. Android Input Method (keyboard) so user can type!
        val isTarget = eventPackage == targetPackage
        val isOurApp = eventPackage == applicationContext.packageName
        val isKeyboard = eventPackage.contains("inputmethod", ignoreCase = true) ||
                eventPackage.contains("keyboard", ignoreCase = true) ||
                eventPackage.contains("ime", ignoreCase = true)

        if (!isTarget && !isOurApp && !isKeyboard) {
            // Unauthorized package detected! Force back to target app immediately
            val now = System.currentTimeMillis()
            if (now - lastInterceptTime > 300) { // Debounce slightly
                lastInterceptTime = now
                forceBackToTarget(targetPackage)
            }
        }
    }

    private fun forceBackToTarget(targetPackage: String) {
        try {
            val launchIntent = packageManager.getLaunchIntentForPackage(targetPackage)?.apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP
                )
            }
            if (launchIntent != null) {
                startActivity(launchIntent)
                mainHandler.post {
                    Toast.makeText(
                        applicationContext,
                        "🔒 Rapper Cloud: Locked to Target App",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        } catch (_: Exception) {
            // Ignore error
        }
    }

    override fun onInterrupt() {
        // Accessibility service interrupted
    }
}
