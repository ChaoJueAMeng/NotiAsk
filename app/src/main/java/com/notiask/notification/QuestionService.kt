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
import kotlinx.coroutines.launch

class QuestionService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = appContainer().notifications.persistentNotification(appContainer().profiles.defaultProfile() != null)
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NotificationController.FOREGROUND_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NotificationController.FOREGROUND_ID, notification)
        }
        intent?.getStringExtra(EXTRA_QUESTION)?.takeIf { it.isNotBlank() }?.let(::askAi)
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun askAi(question: String) = serviceScope.launch {
        val container = appContainer()
        val profile = container.profiles.defaultProfile()
        if (profile == null) {
            container.notifications.showError("请先打开 NotiAsk，保存并选中一组 AI 配置")
            return@launch
        }
        container.notifications.showThinking(question)
        container.aiGateway.ask(profile, question)
            .onSuccess { container.notifications.showAnswer(question, it) }
            .onFailure { container.notifications.showError(it.userMessage()) }
    }

    private fun Throwable.userMessage(): String = when (this) {
        is java.net.SocketTimeoutException -> "请求超时，请检查网络或稍后重试"
        is java.net.UnknownHostException -> "无法连接网络，请检查网络设置"
        else -> message ?: "发生未知错误"
    }

    companion object {
        fun start(context: android.content.Context) {
            ContextCompat.startForegroundService(context, Intent(context, QuestionService::class.java))
        }

        fun ask(context: android.content.Context, question: String) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, QuestionService::class.java).putExtra(EXTRA_QUESTION, question)
            )
        }

        private const val EXTRA_QUESTION = "question"
    }
}
