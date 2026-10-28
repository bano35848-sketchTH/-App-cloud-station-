package com.example.state

import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.data.KioskPreferences
import com.example.service.KioskLockService
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

object KioskStateManager {

    private val _isLocked = MutableStateFlow(false)
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    private val _lockedPackage = MutableStateFlow<String?>(null)
    val lockedPackage: StateFlow<String?> = _lockedPackage.asStateFlow()

    private val _lockedAppName = MutableStateFlow<String?>(null)
    val lockedAppName: StateFlow<String?> = _lockedAppName.asStateFlow()

    private val _lockStartTime = MutableStateFlow(0L)
    val lockStartTime: StateFlow<Long> = _lockStartTime.asStateFlow()

    private val _unlockEvents = MutableSharedFlow<Boolean>(extraBufferCapacity = 1)
    val unlockEvents: SharedFlow<Boolean> = _unlockEvents.asSharedFlow()

    fun init(context: Context) {
        val prefs = KioskPreferences(context)
        _isLocked.value = prefs.isLocked
        _lockedPackage.value = prefs.targetPackage
        _lockedAppName.value = prefs.targetAppName
        _lockStartTime.value = prefs.lockStartTime
    }

    fun startLock(
        context: Context,
        packageName: String,
        appName: String,
        pin: String
    ): Boolean {
        val prefs = KioskPreferences(context)
        prefs.targetPackage = packageName
        prefs.targetAppName = appName
        prefs.pinCode = pin
        prefs.isLocked = true
        prefs.lockStartTime = System.currentTimeMillis()

        _isLocked.value = true
        _lockedPackage.value = packageName
        _lockedAppName.value = appName
        _lockStartTime.value = prefs.lockStartTime

        // Start Foreground Service
        val serviceIntent = Intent(context, KioskLockService::class.java).apply {
            action = KioskLockService.ACTION_START_LOCK
            putExtra(KioskLockService.EXTRA_TARGET_PACKAGE, packageName)
            putExtra(KioskLockService.EXTRA_TARGET_APP_NAME, appName)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }

        // Launch target app
        val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        }
        if (launchIntent != null) {
            context.startActivity(launchIntent)
            return true
        }
        return false
    }

    fun verifyPin(context: Context, candidatePin: String): Boolean {
        val prefs = KioskPreferences(context)
        val savedPin = prefs.pinCode
        return savedPin != null && savedPin == candidatePin
    }

    fun unlock(context: Context): Boolean {
        val prefs = KioskPreferences(context)
        prefs.clearLock()

        _isLocked.value = false
        _lockedPackage.value = null
        _lockedAppName.value = null
        _lockStartTime.value = 0L

        // Stop Foreground Service
        val serviceIntent = Intent(context, KioskLockService::class.java).apply {
            action = KioskLockService.ACTION_STOP_LOCK
        }
        context.stopService(serviceIntent)

        _unlockEvents.tryEmit(true)
        return true
    }
}
