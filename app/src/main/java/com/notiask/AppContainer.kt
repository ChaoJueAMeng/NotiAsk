package com.notiask

import android.content.Context
import com.notiask.ai.AiGateway
import com.notiask.data.ProfileRepository
import com.notiask.notification.NotificationController

class AppContainer(context: Context) {
    val profiles = ProfileRepository(context)
    val aiGateway = AiGateway()
    val notifications = NotificationController(context)
}

fun Context.appContainer(): AppContainer = (applicationContext as NotiAskApplication).container
