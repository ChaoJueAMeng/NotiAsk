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
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.notiask.notification.QuestionService
import com.notiask.ui.settings.SettingsScreen
import com.notiask.ui.theme.NotiAskTheme

class MainActivity : ComponentActivity() {
    private var notificationsEnabled by mutableStateOf(false)

    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        notificationsEnabled = notificationsAllowed()
        when {
            granted -> QuestionService.start(this)
            // 用户此前已永久拒绝：系统不会再弹窗，只能带去设置页手动开启。
            !shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> openNotificationSettings()
            else -> Toast.makeText(this, "未授权通知；前台服务通知不会显示在通知栏", Toast.LENGTH_LONG).show()
        }
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
    }

    override fun onResume() {
        super.onResume()
        notificationsEnabled = notificationsAllowed()
        // 首次进入与误滑常驻通知后返回都走这里：有配置且能发通知就把前台服务挂回（DeleteIntent 为主路径）。
        if (appContainer().profiles.defaultProfile() != null && notificationsAllowed()) {
            QuestionService.start(this)
        }
    }

    private fun enableNotificationAssistant() {
        when {
            !permissionGranted() -> notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            // 权限已给但用户在系统设置里把应用通知整体关掉了，弹权限框没有意义。
            !NotificationManagerCompat.from(this).areNotificationsEnabled() -> openNotificationSettings()
            else -> QuestionService.start(this)
        }
    }

    private fun permissionGranted() = Build.VERSION.SDK_INT < 33 ||
        ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun notificationsAllowed() = permissionGranted() && NotificationManagerCompat.from(this).areNotificationsEnabled()

    private fun openNotificationSettings() = startActivity(
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
    )

    private fun openBatterySettings() = startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
}
