package com.notiask.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import com.notiask.MainActivity
import com.notiask.R
import com.notiask.data.AiProfile
import com.notiask.data.ConfiguredProfile
import com.notiask.screenshot.ScreenshotAskActivity
import java.io.File

class NotificationController(private val context: Context) {
    private var persistentPane = PersistentPane.ASK
    private var modelPage = 0

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
                description = "AI 回答、已截图提示和错误提示"
            }
        )
    }

    fun showSwitchModelPane() {
        persistentPane = PersistentPane.SWITCH_MODEL
        modelPage = 0
    }

    fun showAskPane() {
        persistentPane = PersistentPane.ASK
    }

    fun setModelPage(page: Int) {
        persistentPane = PersistentPane.SWITCH_MODEL
        modelPage = page.coerceAtLeast(0)
    }

    fun persistentNotification(
        profile: ConfiguredProfile?,
        profiles: List<AiProfile> = emptyList(),
        defaultId: String? = null,
    ): android.app.Notification {
        val configured = profile != null
        if (!configured) persistentPane = PersistentPane.ASK
        return if (configured && persistentPane == PersistentPane.SWITCH_MODEL) {
            switchModelNotification(profiles, defaultId)
        } else {
            askNotification(profile)
        }
    }

    fun showThinking(question: String, fromScreenshot: Boolean = false) {
        notify(ANSWER_ID, base(CHANNEL_ANSWER)
            .setContentTitle(if (fromScreenshot) "AI 正在看图" else "AI 正在思考")
            .setContentText(if (fromScreenshot) "正在分析截图…" else question)
            .setProgress(0, 0, true)
            .setOnlyAlertOnce(true)
            .build())
    }

    fun showAnswer(question: String, answer: String) {
        saveLastAnswer(question, answer)
        notify(ANSWER_ID, compactAnswerNotification(question, answer))
    }

    fun showError(message: String) {
        notify(ANSWER_ID, base(CHANNEL_ANSWER)
            .setContentTitle("AI 问答失败")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .build())
    }

    fun showScreenshotReady() {
        notify(ANSWER_ID, base(CHANNEL_ANSWER)
            .setContentTitle("已截图")
            .setContentText("点「提问」补充问题，或点「直接提问」")
            .setStyle(NotificationCompat.BigTextStyle().bigText("已截取当前屏幕。可补充问题再发给 AI，也可以直接提问。"))
            .setOnlyAlertOnce(false)
            .setAutoCancel(false)
            .setDeleteIntent(discardScreenshotIntent())
            .addAction(replyAction(context.getString(R.string.action_ask), hasImage = true))
            .addAction(screenshotDirectAction())
            .build())
    }

    fun lastAnswer(): String = runCatching { answerFile().readText() }.getOrDefault("")

    fun lastQuestion(): String = runCatching { questionFile().readText() }.getOrDefault("")

    private fun askNotification(profile: ConfiguredProfile?): android.app.Notification {
        val configured = profile != null
        val using = profile?.profile?.let { "${it.name} · ${it.model}" }.orEmpty()
        val title = if (configured) {
            context.getString(R.string.ask_title)
        } else {
            context.getString(R.string.ask_title_unconfigured)
        }
        val subtitle = if (configured) {
            "${context.getString(R.string.currently_using)}：$using"
        } else {
            context.getString(R.string.ask_configure_hint)
        }
        val askAction = replyAction(context.getString(R.string.action_ask), hasImage = false)
        return base(CHANNEL_ASK)
            .setContentTitle(title)
            .setContentText(subtitle)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setDeleteIntent(restorePersistentIntent())
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(askRemoteViews(title, subtitle, showExpandHint = configured))
            .setCustomHeadsUpContentView(askRemoteViews(title, subtitle, showExpandHint = configured))
            .setCustomBigContentView(askRemoteViews(title, subtitle, showExpandHint = false))
            .also { builder ->
                hideSystemReplyAffordance(builder)
                if (configured) {
                    builder.addAction(askAction)
                        .addAction(NotificationCompat.Action.Builder(0, context.getString(R.string.action_switch_model), broadcast(ACTION_SWITCH_MODEL, REQUEST_SWITCH_MODEL)).build())
                        .addAction(screenshotAction())
                } else builder.setContentIntent(openAppIntent())
            }
            .build()
    }

    private fun askRemoteViews(
        title: String,
        subtitle: String,
        showExpandHint: Boolean,
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.notification_ask)
        views.setTextViewText(R.id.title, title)
        views.setTextViewText(R.id.subtitle, subtitle)
        views.setTextViewText(R.id.expand_hint, context.getString(R.string.ask_expand_more_hint))
        views.setViewVisibility(R.id.expand_hint, if (showExpandHint) View.VISIBLE else View.GONE)
        return views
    }

    private fun hideSystemReplyAffordance(builder: NotificationCompat.Builder) {
        builder.setAllowSystemGeneratedContextualActions(false)
        builder.extras.putBoolean("android.hideSmartReplies", true)
        builder.extras.putBoolean("miui.showAction", false)
    }

    private fun switchModelNotification(profiles: List<AiProfile>, defaultId: String?): android.app.Notification {
        val ordered = if (defaultId == null) profiles else profiles.sortedByDescending { it.id == defaultId }
        val pageCount = (ordered.size + PAGE_SIZE - 1).coerceAtLeast(1) / PAGE_SIZE
        if (modelPage >= pageCount) modelPage = 0
        val pageItems = ordered.drop(modelPage * PAGE_SIZE).take(PAGE_SIZE)

        val collapsed = RemoteViews(context.packageName, R.layout.notification_ask_collapsed)
        collapsed.setTextViewText(R.id.status, context.getString(R.string.switch_model_expand_hint))

        val expanded = RemoteViews(context.packageName, R.layout.notification_switch_model)
        expanded.setTextViewText(R.id.title, context.getString(R.string.switch_model_title))
        expanded.removeAllViews(R.id.model_list)
        pageItems.forEach { profile ->
            val inUse = profile.id == defaultId
            val row = RemoteViews(context.packageName, R.layout.notification_model_row)
            row.setTextViewText(R.id.model_name, profile.name.ifBlank { profile.provider.displayName })
            row.setTextViewText(R.id.model_meta, "${profile.provider.displayName} · ${profile.model}")
            row.setTextViewText(
                R.id.model_action,
                if (inUse) context.getString(R.string.currently_using) else context.getString(R.string.tap_to_use)
            )
            row.setOnClickPendingIntent(R.id.model_row, selectModelIntent(profile.id))
            expanded.addView(R.id.model_list, row)
        }
        if (ordered.isEmpty()) {
            val empty = RemoteViews(context.packageName, R.layout.notification_model_row)
            empty.setTextViewText(R.id.model_name, context.getString(R.string.switch_model_empty))
            empty.setTextViewText(R.id.model_meta, "")
            empty.setTextViewText(R.id.model_action, "")
            expanded.addView(R.id.model_list, empty)
        } else if (pageCount > 1) {
            val more = RemoteViews(context.packageName, R.layout.notification_model_row)
            more.setTextViewText(R.id.model_name, context.getString(R.string.action_more_models))
            more.setTextViewText(R.id.model_meta, "${modelPage + 1} / $pageCount")
            more.setTextViewText(R.id.model_action, context.getString(R.string.tap_next_page))
            more.setOnClickPendingIntent(R.id.model_row, modelPageIntent((modelPage + 1) % pageCount))
            expanded.addView(R.id.model_list, more)
        }

        return base(CHANNEL_ASK)
            .setContentTitle(context.getString(R.string.action_switch_model))
            .setContentText(context.getString(R.string.switch_model_expand_hint))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setDeleteIntent(restorePersistentIntent())
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(collapsed)
            .setCustomBigContentView(expanded)
            .addAction(NotificationCompat.Action.Builder(0, context.getString(R.string.action_back_to_ask), broadcast(ACTION_SHOW_ASK, REQUEST_SHOW_ASK)).build())
            .addAction(screenshotAction())
            .build()
    }

    private fun compactAnswerNotification(question: String, answer: String) = base(CHANNEL_ANSWER)
        .setContentTitle("AI 回答")
        .setContentText(answer)
        .setStyle(NotificationCompat.BigTextStyle().bigText(answer).setBigContentTitle("AI 回答").setSummaryText(question))
        .setOnlyAlertOnce(false)
        .setAutoCancel(false)
        .addAction(NotificationCompat.Action.Builder(0, context.getString(R.string.action_copy), broadcast(ACTION_COPY_ANSWER, REQUEST_COPY_ANSWER)).build())
        .build()

    private fun saveLastAnswer(question: String, answer: String) {
        runCatching { questionFile().writeText(question) }
        runCatching { answerFile().writeText(answer) }
    }

    private fun answerFile() = File(context.cacheDir, "last_ai_answer.txt")

    private fun questionFile() = File(context.cacheDir, "last_ai_question.txt")

    private fun base(channel: String) = NotificationCompat.Builder(context, channel)
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setCategory(if (channel == CHANNEL_ASK) NotificationCompat.CATEGORY_SERVICE else NotificationCompat.CATEGORY_MESSAGE)
        .setAutoCancel(channel == CHANNEL_ANSWER)

    private fun replyAction(label: String, hasImage: Boolean): NotificationCompat.Action {
        val remoteInput = RemoteInput.Builder(INPUT_KEY)
            .setLabel(if (hasImage) context.getString(R.string.screenshot_input_hint) else context.getString(R.string.notification_input_hint))
            .build()
        val intent = Intent(context, QuestionReceiver::class.java)
            .setAction(ACTION_ASK)
            .putExtra(EXTRA_HAS_IMAGE, hasImage)
        val request = if (hasImage) REQUEST_ASK_SCREENSHOT else REQUEST_ASK
        val pendingIntent = PendingIntent.getBroadcast(
            context, request, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
        return NotificationCompat.Action.Builder(0, label, pendingIntent)
            .addRemoteInput(remoteInput)
            .setAllowGeneratedReplies(false)
            .setSemanticAction(NotificationCompat.Action.SEMANTIC_ACTION_NONE)
            .build()
    }

    private fun selectModelIntent(profileId: String): PendingIntent {
        val intent = Intent(context, QuestionReceiver::class.java)
            .setAction(ACTION_SELECT_MODEL)
            .putExtra(EXTRA_PROFILE_ID, profileId)
        return PendingIntent.getBroadcast(
            context, REQUEST_SELECT_MODEL + (profileId.hashCode() and 0x7FFF), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun modelPageIntent(page: Int): PendingIntent {
        val intent = Intent(context, QuestionReceiver::class.java)
            .setAction(ACTION_MODEL_PAGE)
            .putExtra(EXTRA_MODEL_PAGE, page)
        return PendingIntent.getBroadcast(
            context, REQUEST_MODEL_PAGE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun broadcast(action: String, requestCode: Int): PendingIntent {
        val intent = Intent(context, QuestionReceiver::class.java).setAction(action)
        return PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun screenshotDirectAction(): NotificationCompat.Action {
        return NotificationCompat.Action.Builder(0, "直接提问", broadcast(ACTION_ASK_SCREENSHOT, REQUEST_ASK_SCREENSHOT_DIRECT)).build()
    }

    private fun discardScreenshotIntent(): PendingIntent = broadcast(ACTION_DISCARD_SCREENSHOT, REQUEST_DISCARD_SCREENSHOT)

    /** Android 13+ 允许用户滑掉 FGS 常驻通知；DeleteIntent 触发后通过 startForeground 重新挂上。 */
    private fun restorePersistentIntent(): PendingIntent = broadcast(ACTION_RESTORE_PERSISTENT, REQUEST_RESTORE_PERSISTENT)

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

    private enum class PersistentPane { ASK, SWITCH_MODEL }

    companion object {
        const val CHANNEL_ASK = "ai_ask"
        const val CHANNEL_ANSWER = "ai_answer"
        const val INPUT_KEY = "ai_question"
        const val ACTION_ASK = "com.notiask.action.ASK"
        const val ACTION_ASK_SCREENSHOT = "com.notiask.action.ASK_SCREENSHOT"
        const val ACTION_DISCARD_SCREENSHOT = "com.notiask.action.DISCARD_SCREENSHOT"
        const val ACTION_COPY_ANSWER = "com.notiask.action.COPY_ANSWER"
        const val ACTION_SWITCH_MODEL = "com.notiask.action.SWITCH_MODEL"
        const val ACTION_SHOW_ASK = "com.notiask.action.SHOW_ASK"
        const val ACTION_SELECT_MODEL = "com.notiask.action.SELECT_MODEL"
        const val ACTION_MODEL_PAGE = "com.notiask.action.MODEL_PAGE"
        const val ACTION_RESTORE_PERSISTENT = "com.notiask.action.RESTORE_PERSISTENT"
        const val EXTRA_HAS_IMAGE = "has_image"
        const val EXTRA_PROFILE_ID = "profile_id"
        const val EXTRA_MODEL_PAGE = "model_page"
        const val FOREGROUND_ID = 1001
        const val SCREENSHOT_FGS_ID = 1003
        private const val ANSWER_ID = 1002
        private const val REQUEST_ASK = 2101
        private const val REQUEST_SCREENSHOT = 2102
        private const val REQUEST_ASK_SCREENSHOT = 2103
        private const val REQUEST_ASK_SCREENSHOT_DIRECT = 2104
        private const val REQUEST_DISCARD_SCREENSHOT = 2105
        private const val REQUEST_SWITCH_MODEL = 2106
        private const val REQUEST_COPY_ANSWER = 2108
        private const val REQUEST_SHOW_ASK = 2109
        private const val REQUEST_RESTORE_PERSISTENT = 2110
        private const val REQUEST_MODEL_PAGE = 2111
        private const val REQUEST_SELECT_MODEL = 2200
        private const val PAGE_SIZE = 5
    }
}
