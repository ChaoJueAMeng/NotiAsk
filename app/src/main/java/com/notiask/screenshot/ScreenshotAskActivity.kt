package com.notiask.screenshot

import android.app.Activity
import android.media.projection.MediaProjectionConfig
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.notiask.appContainer

/** Invisible trampoline: request screen-capture consent, then leave the user in the shade. */
class ScreenshotAskActivity : ComponentActivity() {
    private val screenCapture = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode != Activity.RESULT_OK || result.data == null) {
            appContainer().notifications.showError("未授权截屏")
            finish()
            return@registerForActivityResult
        }
        ScreenshotCaptureService.start(this, result.resultCode, result.data!!)
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        skipTransitions()
        window.decorView.alpha = 0f
        requestCapture()
    }

    override fun finish() {
        super.finish()
        skipTransitions()
    }

    private fun skipTransitions() {
        if (Build.VERSION.SDK_INT >= 34) {
            overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, 0, 0)
            overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, 0, 0)
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }
    }

    private fun requestCapture() {
        val manager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val intent = if (Build.VERSION.SDK_INT >= 34) {
            manager.createScreenCaptureIntent(MediaProjectionConfig.createConfigForDefaultDisplay())
        } else {
            manager.createScreenCaptureIntent()
        }
        screenCapture.launch(intent)
    }
}
