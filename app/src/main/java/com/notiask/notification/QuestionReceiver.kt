package com.notiask.notification

import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.app.RemoteInput
import com.notiask.R
import com.notiask.ai.VisionRequestBodies
import com.notiask.appContainer

class QuestionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val notifications = context.appContainer().notifications
        val profiles = context.appContainer().profiles
        when (intent.action) {
            NotificationController.ACTION_ASK -> {
                val typed = RemoteInput.getResultsFromIntent(intent)
                    ?.getCharSequence(NotificationController.INPUT_KEY)
                    ?.toString()?.trim().orEmpty()
                val hasImage = intent.getBooleanExtra(NotificationController.EXTRA_HAS_IMAGE, false)
                val question = typed.ifEmpty {
                    if (hasImage) VisionRequestBodies.DEFAULT_SCREENSHOT_QUESTION else ""
                }
                if (question.isEmpty()) return
                QuestionService.ask(context, question, hasImage)
            }
            NotificationController.ACTION_ASK_SCREENSHOT -> {
                QuestionService.ask(context, VisionRequestBodies.DEFAULT_SCREENSHOT_QUESTION, hasImage = true)
            }
            NotificationController.ACTION_DISCARD_SCREENSHOT -> {
                context.appContainer().screenshotSession.clear()
            }
            NotificationController.ACTION_COPY_ANSWER -> {
                val answer = notifications.lastAnswer()
                if (answer.isBlank()) return
                val clipboard = context.getSystemService(ClipboardManager::class.java)
                clipboard.setPrimaryClip(ClipData.newPlainText("AI 回答", answer))
                Toast.makeText(context, context.getString(R.string.copied_answer), Toast.LENGTH_SHORT).show()
            }
            NotificationController.ACTION_SWITCH_MODEL -> {
                notifications.showSwitchModelPane()
                QuestionService.start(context)
            }
            NotificationController.ACTION_SHOW_ASK -> {
                notifications.showAskPane()
                QuestionService.start(context)
            }
            NotificationController.ACTION_SELECT_MODEL -> {
                val id = intent.getStringExtra(NotificationController.EXTRA_PROFILE_ID) ?: return
                if (profiles.profiles.value.none { it.id == id }) return
                if (!profiles.isDefault(id)) {
                    profiles.selectDefault(id)
                    val selected = profiles.profiles.value.first { it.id == id }
                    Toast.makeText(context, "已切换为 ${selected.name} · ${selected.model}", Toast.LENGTH_SHORT).show()
                }
                notifications.showAskPane()
                QuestionService.start(context)
            }
            NotificationController.ACTION_MODEL_PAGE -> {
                notifications.setModelPage(intent.getIntExtra(NotificationController.EXTRA_MODEL_PAGE, 0))
                QuestionService.start(context)
            }
        }
    }
}
