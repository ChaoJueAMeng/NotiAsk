package com.notiask.notification

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.content.ContextCompat
import com.notiask.appContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class QuestionService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        serviceScope.launch(Dispatchers.Main) {
            combine(
                appContainer().profiles.profiles,
                appContainer().profiles.defaultId,
            ) { _, _ -> }
                .collect { startForegroundNotification() }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundNotification()
        intent?.getStringExtra(EXTRA_QUESTION)?.takeIf { it.isNotBlank() }?.let { question ->
            val wantsImage = intent.getBooleanExtra(EXTRA_HAS_IMAGE, false)
            val image = if (wantsImage) appContainer().screenshotSession.takePendingAskImage() else null
            askAi(question, image, wantsImage)
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun startForegroundNotification() {
        val repo = appContainer().profiles
        val notification = appContainer().notifications.persistentNotification(
            repo.defaultProfile(),
            repo.profiles.value,
            repo.defaultId.value,
        )
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NotificationController.FOREGROUND_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NotificationController.FOREGROUND_ID, notification)
        }
    }

    private fun askAi(question: String, imageJpeg: ByteArray?, wantsImage: Boolean) = serviceScope.launch {
        val container = appContainer()
        val profile = container.profiles.defaultProfile()
        if (profile == null) {
            container.notifications.showError("请先打开 NotiAsk，保存并选中一组 AI 配置")
            return@launch
        }
        if (wantsImage && imageJpeg == null) {
            container.notifications.showError("截图已失效，请重新截屏提问")
            return@launch
        }
        container.notifications.showThinking(question, fromScreenshot = imageJpeg != null)
        container.aiGateway.ask(profile, question, imageJpeg)
            .onSuccess { container.notifications.showAnswer(question, it) }
            .onFailure { container.notifications.showError(it.userMessage(imageJpeg != null)) }
    }

    private fun Throwable.userMessage(fromScreenshot: Boolean): String = when (this) {
        is java.net.SocketTimeoutException -> "请求超时，请检查网络或稍后重试"
        is java.net.UnknownHostException -> "无法连接网络，请检查网络设置"
        else -> {
            val msg = message ?: "发生未知错误"
            if (fromScreenshot && looksLikeVisionUnsupported(msg)) {
                "当前模型可能不支持识图，请改用支持视觉的模型后重试\n$msg"
            } else msg
        }
    }

    private fun looksLikeVisionUnsupported(msg: String): Boolean {
        val lower = msg.lowercase()
        return lower.contains("image") || lower.contains("vision") || lower.contains("multimodal") ||
            msg.contains("图片") || msg.contains("视觉") || msg.contains("识图")
    }

    companion object {
        fun start(context: android.content.Context) {
            ContextCompat.startForegroundService(context, Intent(context, QuestionService::class.java))
        }

        fun ask(context: android.content.Context, question: String, hasImage: Boolean = false) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, QuestionService::class.java)
                    .putExtra(EXTRA_QUESTION, question)
                    .putExtra(EXTRA_HAS_IMAGE, hasImage)
            )
        }

        private const val EXTRA_QUESTION = "question"
        private const val EXTRA_HAS_IMAGE = "has_image"
    }
}
