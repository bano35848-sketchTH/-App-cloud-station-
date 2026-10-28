package com.example.model

import android.graphics.Bitmap
import android.graphics.drawable.Drawable

data class AppInfo(
    val packageName: String,
    val appName: String,
    val icon: Drawable? = null,
    val iconBitmap: Bitmap? = null,
    val isSystemApp: Boolean = false
)
