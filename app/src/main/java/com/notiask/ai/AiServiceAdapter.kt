package com.notiask.ai

import com.notiask.data.ConfiguredProfile

/** A protocol adapter; adding a provider normally only requires another implementation here. */
interface AiServiceAdapter {
    suspend fun ask(profile: ConfiguredProfile, question: String): String
}

class AiRequestException(message: String) : Exception(message)
