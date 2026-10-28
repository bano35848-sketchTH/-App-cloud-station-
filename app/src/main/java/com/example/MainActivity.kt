package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.state.KioskStateManager
import com.example.ui.MainScreen
import com.example.ui.theme.RapperCloudTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        KioskStateManager.init(applicationContext)

        setContent {
            RapperCloudTheme {
                MainScreen()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        KioskStateManager.init(applicationContext)
    }

    override fun onResume() {
        super.onResume()
        KioskStateManager.init(applicationContext)
    }
}
