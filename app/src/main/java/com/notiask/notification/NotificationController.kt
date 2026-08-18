package com.notiask.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.notiask.MainActivity
import com.notiask.R

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

    fun persistentNotification(configured: Boolean): android.app.Notification {
        val builder = base(CHANNEL_ASK)
            .setContentTitle(if (configured) "NotiAsk：在这里问 AI" else "NotiAsk：请先配置 AI")
            .setContentText(if (configured) "点按输入框即可提问" else "点按此处打开设置")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
        if (configured) {
            val askIntent = askAppIntent("输入你的问题", REQUEST_ASK)
            val contentView = askContentView("点击输入问题", askIntent)
            builder.setStyle(NotificationCompat.DecoratedCustomViewStyle())
                .setContentIntent(askIntent)
                .setCustomContentView(contentView)
                .setCustomBigContentView(contentView)
        } else {
            builder.setContentIntent(openAppIntent())
        }
        return builder.build()
    }

    fun showThinking(question: String) {
        notify(ANSWER_ID, base(CHANNEL_ANSWER)
            .setContentTitle("AI 正在思考")
            .setContentText(question)
            .setProgress(0, 0, true)
            .setOnlyAlertOnce(true)
            .build())
    }

    fun showAnswer(question: String, answer: String) {
        val askIntent = askAppIntent("继续输入追问", REQUEST_FOLLOWUP)
        notify(ANSWER_ID, base(CHANNEL_ANSWER)
            .setContentTitle("AI 回答")
            .setContentText(answer)
            .setStyle(NotificationCompat.BigTextStyle().bigText(answer).setBigContentTitle("AI 回答").setSummaryText(question))
            .setOnlyAlertOnce(false)
            .setContentIntent(askIntent)
            .addAction(inputAction("继续输入", askIntent))
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
        .setCategory(if (channel == CHANNEL_ASK) NotificationCompat.CATEGORY_SERVICE else NotificationCompat.CATEGORY_STATUS)
        .setAutoCancel(channel == CHANNEL_ANSWER)

    private fun inputAction(label: String, pendingIntent: PendingIntent) = NotificationCompat.Action.Builder(
        IconCompat.createWithResource(context, R.drawable.ic_notification_input),
        label,
        pendingIntent
    ).setSemanticAction(NotificationCompat.Action.SEMANTIC_ACTION_NONE).build()

    private fun askContentView(hint: String, clickIntent: PendingIntent): RemoteViews {
        return RemoteViews(context.packageName, R.layout.notification_ask).apply {
            setTextViewText(R.id.notification_input_hint, hint)
            setInt(R.id.notification_input_icon, "setColorFilter", 0xFF315EFB.toInt())
            setOnClickPendingIntent(R.id.notification_ask_root, clickIntent)
        }
    }

    private fun askAppIntent(hint: String, requestCode: Int): PendingIntent = PendingIntent.getActivity(
        context, requestCode, AskActivity.intent(context, hint),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

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
        private const val ANSWER_ID = 1002
        private const val REQUEST_ASK = 2101
        private const val REQUEST_FOLLOWUP = 2102
    }
}
