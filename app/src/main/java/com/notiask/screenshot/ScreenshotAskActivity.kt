package com.notiask.screenshot

import android.app.Activity
import android.graphics.BitmapFactory
import android.media.projection.MediaProjectionConfig
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.notiask.ai.VisionRequestBodies
import com.notiask.appContainer
import com.notiask.notification.QuestionService

class ScreenshotAskActivity : ComponentActivity() {
    private val session by lazy { appContainer().screenshotSession }
    private var submitted = false

    private val screenCapture = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode != Activity.RESULT_OK || result.data == null) {
            Toast.makeText(this, "未授权截屏", Toast.LENGTH_SHORT).show()
            finish()
            return@registerForActivityResult
        }
        ScreenshotCaptureService.start(this, result.resultCode, result.data!!)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        skipTransitions()
        if (session.state.value !is ScreenshotSession.State.Ready) {
            window.decorView.alpha = 0f
        }
        setContent {
            val state by session.state.collectAsStateWithLifecycle()
            when (val current = state) {
                is ScreenshotSession.State.Ready -> MaterialTheme {
                    ScreenshotAskScreen(
                        jpeg = current.jpeg,
                        onSubmit = { submit(current.jpeg, it) },
                        onRetake = {
                            session.reset()
                            requestCapture()
                        },
                        onCancel = { finish() }
                    )
                }
                else -> Box(Modifier.fillMaxSize())
            }
            LaunchedEffect(state) {
                val current = state
                val ready = current is ScreenshotSession.State.Ready
                window.decorView.alpha = if (ready) 1f else 0f
                window.setBackgroundDrawableResource(
                    if (ready) android.R.color.white else android.R.color.transparent
                )
                if (current is ScreenshotSession.State.Failed) {
                    Toast.makeText(this@ScreenshotAskActivity, current.message, Toast.LENGTH_LONG).show()
                    finish()
                }
            }
        }
        when (session.state.value) {
            is ScreenshotSession.State.Ready, ScreenshotSession.State.Capturing -> Unit
            else -> requestCapture()
        }
    }

    override fun finish() {
        super.finish()
        skipTransitions()
    }

    override fun onDestroy() {
        if (!isChangingConfigurations && !submitted) {
            session.reset()
            stopService(android.content.Intent(this, ScreenshotCaptureService::class.java))
        }
        super.onDestroy()
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

    private fun submit(jpeg: ByteArray, question: String) {
        submitted = true
        session.setPendingAskImage(jpeg)
        session.idleKeepingPending()
        val text = question.trim().ifBlank { VisionRequestBodies.DEFAULT_SCREENSHOT_QUESTION }
        QuestionService.ask(this, text, hasImage = true)
        finish()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScreenshotAskScreen(
    jpeg: ByteArray,
    onSubmit: (String) -> Unit,
    onRetake: () -> Unit,
    onCancel: () -> Unit
) {
    var question by rememberSaveable { mutableStateOf("") }
    val preview = remember(jpeg) { BitmapFactory.decodeByteArray(jpeg, 0, jpeg.size) }
    Scaffold(topBar = { TopAppBar(title = { Text("截屏提问") }) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("已截取当前屏幕。可以补充问题后发送，也可以直接让 AI 解读截图。")
            Text(
                "识图需要当前默认模型支持视觉（如 GPT-4o、Claude、Qwen-VL、Kimi 视觉模型）。",
                style = MaterialTheme.typography.bodySmall
            )
            if (preview != null) {
                Image(
                    bitmap = preview.asImageBitmap(),
                    contentDescription = "截屏预览",
                    modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp),
                    contentScale = ContentScale.Fit
                )
            }
            OutlinedTextField(
                value = question,
                onValueChange = { question = it },
                label = { Text("补充问题（可选）") },
                placeholder = { Text("例如：这段报错怎么处理？") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )
            Button(onClick = { onSubmit(question) }, modifier = Modifier.fillMaxWidth()) { Text("发送给 AI") }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = onRetake, modifier = Modifier.weight(1f)) { Text("重拍") }
                OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text("取消") }
            }
        }
    }
}
