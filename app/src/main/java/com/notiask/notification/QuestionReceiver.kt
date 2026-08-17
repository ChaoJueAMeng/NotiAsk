package com.notiask.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.RemoteInput

class QuestionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != NotificationController.ACTION_ASK) return
        val question = RemoteInput.getResultsFromIntent(intent)
            ?.getCharSequence(NotificationController.INPUT_KEY)
            ?.toString()?.trim().orEmpty()
        if (question.isEmpty()) return

        // The foreground service owns the possibly long network request, rather than this
        // short-lived broadcast receiver.
        QuestionService.ask(context, question)
    }
}
