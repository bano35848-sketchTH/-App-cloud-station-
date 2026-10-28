package com.example.data

import android.content.Context
import android.content.SharedPreferences

class KioskPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("rapper_cloud_kiosk_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_TARGET_PACKAGE = "target_package"
        private const val KEY_TARGET_APP_NAME = "target_app_name"
        private const val KEY_PIN_HASH = "pin_hash"
        private const val KEY_IS_LOCKED = "is_locked"
        private const val KEY_OVERLAY_STYLE = "overlay_style"
        private const val KEY_START_TIME = "lock_start_time"
    }

    var targetPackage: String?
        get() = prefs.getString(KEY_TARGET_PACKAGE, null)
        set(value) = prefs.edit().putString(KEY_TARGET_PACKAGE, value).apply()

    var targetAppName: String?
        get() = prefs.getString(KEY_TARGET_APP_NAME, null)
        set(value) = prefs.edit().putString(KEY_TARGET_APP_NAME, value).apply()

    var pinCode: String?
        get() = prefs.getString(KEY_PIN_HASH, null)
        set(value) = prefs.edit().putString(KEY_PIN_HASH, value).apply()

    var isLocked: Boolean
        get() = prefs.getBoolean(KEY_IS_LOCKED, false)
        set(value) = prefs.edit().putBoolean(KEY_IS_LOCKED, value).apply()

    var overlayStyle: String
        get() = prefs.getString(KEY_OVERLAY_STYLE, "DISCREET_LOCK") ?: "DISCREET_LOCK"
        set(value) = prefs.edit().putString(KEY_OVERLAY_STYLE, value).apply()

    var lockStartTime: Long
        get() = prefs.getLong(KEY_START_TIME, 0L)
        set(value) = prefs.edit().putLong(KEY_START_TIME, value).apply()

    fun clearLock() {
        prefs.edit()
            .putBoolean(KEY_IS_LOCKED, false)
            .putLong(KEY_START_TIME, 0L)
            .apply()
    }
}
