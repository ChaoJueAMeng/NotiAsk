package com.notiask.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import com.notiask.R
import com.notiask.MainActivity
import com.notiask.screenshot.ScreenshotAskActivity

class NotificationController(private val context: Context) {
    fun createChannels() {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ASK, context.getString(R.string.notification_channel_name), NotificationManager.IMPORTANCE_LOW).apply {
                description = context.getString(R.string.notification_channel_description)
                setShowBadge(false)
            }
        )
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ANSWER, "AI 回答", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "AI 回答和错误提示"
            }
        )
    }

    fun persistentNotification(configured: Boolean) = base(CHANNEL_ASK)
        .setContentTitle(if (configured) "NotiAsk：在这里问 AI" else "NotiAsk：请先配置 AI")
        .setContentText(if (configured) "展开后可提问或截屏搜索" else "点按此处打开设置")
        .setOngoing(true)
        .setOnlyAlertOnce(true)
        .also { builder ->
            if (configured) {
                builder.addAction(replyAction("提问"))
                builder.addAction(screenshotAction())
            } else builder.setContentIntent(openAppIntent())
        }
        .build()

    fun showThinking(question: String, fromScreenshot: Boolean = false) {
        notify(ANSWER_ID, base(CHANNEL_ANSWER)
            .setContentTitle(if (fromScreenshot) "AI 正在看图" else "AI 正在思考")
            .setContentText(if (fromScreenshot) "正在分析截图…" else question)
            .setProgress(0, 0, true)
            .setOnlyAlertOnce(true)
            .build())
    }

    fun showAnswer(question: String, answer: String) {
        notify(ANSWER_ID, base(CHANNEL_ANSWER)
            .setContentTitle("AI 回答")
            .setContentText(answer)
            .setStyle(NotificationCompat.BigTextStyle().bigText(answer).setBigContentTitle("AI 回答").setSummaryText(question))
            .setOnlyAlertOnce(false)
            .addAction(replyAction("继续追问"))
            .build())
    }

    fun showError(message: String) {
        notify(ANSWER_ID, base(CHANNEL_ANSWER)
            .setContentTitle("AI 问答失败")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .build())
    }

    private fun base(channel: String) = NotificationCompat.Builder(context, channel)
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setCategory(NotificationCompat.CATEGORY_MESSAGE)
        .setAutoCancel(channel == CHANNEL_ANSWER)

    private fun replyAction(label: String): NotificationCompat.Action {
        val remoteInput = RemoteInput.Builder(INPUT_KEY).setLabel("输入你的问题").build()
        val intent = Intent(context, QuestionReceiver::class.java).setAction(ACTION_ASK)
        val pendingIntent = PendingIntent.getBroadcast(
            context, REQUEST_ASK, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
        return NotificationCompat.Action.Builder(0, label, pendingIntent).addRemoteInput(remoteInput).build()
    }

    private fun screenshotAction(): NotificationCompat.Action {
        val intent = Intent(context, ScreenshotAskActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NO_ANIMATION)
        val pendingIntent = PendingIntent.getActivity(
            context, REQUEST_SCREENSHOT, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Action.Builder(R.drawable.ic_screenshot, "截屏搜索", pendingIntent).build()
    }

    private fun openAppIntent(): PendingIntent = PendingIntent.getActivity(
        context, 0, Intent(context, MainActivity::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private fun notify(id: Int, notification: android.app.Notification) {
        NotificationManagerCompat.from(context).notify(id, notification)
    }

    companion object {
        const val CHANNEL_ASK = "ai_ask"
        const val CHANNEL_ANSWER = "ai_answer"
        const val INPUT_KEY = "ai_question"
        const val ACTION_ASK = "com.notiask.action.ASK"
        const val FOREGROUND_ID = 1001
        const val SCREENSHOT_FGS_ID = 1003
        private const val ANSWER_ID = 1002
        private const val REQUEST_ASK = 2101
        private const val REQUEST_SCREENSHOT = 2102
    }
}
