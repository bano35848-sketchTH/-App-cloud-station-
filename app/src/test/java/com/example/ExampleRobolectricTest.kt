package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.KioskPreferences
import com.example.state.KioskStateManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Rapper Cloud", appName)
    }

    @Test
    fun `test kiosk preferences storage and pin verification`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = KioskPreferences(context)

        prefs.targetPackage = "com.android.calculator2"
        prefs.targetAppName = "Calculator"
        prefs.pinCode = "1234"
        prefs.isLocked = true

        assertEquals("com.android.calculator2", prefs.targetPackage)
        assertEquals("Calculator", prefs.targetAppName)
        assertEquals("1234", prefs.pinCode)
        assertTrue(prefs.isLocked)

        // Verify PIN via KioskStateManager
        assertTrue(KioskStateManager.verifyPin(context, "1234"))
        assertFalse(KioskStateManager.verifyPin(context, "9999"))

        // Unlock
        KioskStateManager.unlock(context)
        assertFalse(prefs.isLocked)
    }
}
