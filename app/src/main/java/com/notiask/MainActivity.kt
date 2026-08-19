package com.notiask

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.notiask.notification.QuestionService
import com.notiask.ui.settings.SettingsScreen
import com.notiask.ui.theme.NotiAskTheme

class MainActivity : ComponentActivity() {
    private var notificationsEnabled by mutableStateOf(false)

    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        notificationsEnabled = granted
        if (granted) QuestionService.start(this)
        else Toast.makeText(this, "未授权通知；前台服务通知不会显示在通知栏", Toast.LENGTH_LONG).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = true
        notificationsEnabled = notificationsAllowed()
        setContent {
            NotiAskTheme {
                SettingsScreen(
                    container = appContainer(),
                    notificationsEnabled = notificationsEnabled,
                    onEnable = { enableNotificationAssistant() },
                    onBatterySettings = { openBatterySettings() },
                )
            }
        }
        if (appContainer().profiles.defaultProfile() != null && notificationsAllowed()) {
            QuestionService.start(this)
        }
    }

    override fun onResume() {
        super.onResume()
        notificationsEnabled = notificationsAllowed()
    }

    private fun enableNotificationAssistant() {
        if (!notificationsAllowed()) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else QuestionService.start(this)
    }

    private fun notificationsAllowed() = Build.VERSION.SDK_INT < 33 ||
        ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun openBatterySettings() = startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
}
