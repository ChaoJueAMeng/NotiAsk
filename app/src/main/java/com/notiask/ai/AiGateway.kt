package com.notiask.ai

import com.notiask.data.ConfiguredProfile
import com.notiask.data.ProviderKind
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class AiGateway {
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .callTimeout(100, TimeUnit.SECONDS)
        .build()

    suspend fun ask(profile: ConfiguredProfile, question: String): Result<String> = runCatching {
        val adapter: AiServiceAdapter = when (profile.profile.provider) {
            ProviderKind.ANTHROPIC -> AnthropicAdapter(client)
            ProviderKind.OPENAI,
            ProviderKind.DASHSCOPE,
            ProviderKind.DEEPSEEK,
            ProviderKind.KIMI,
            ProviderKind.COMPATIBLE -> OpenAiCompatibleAdapter(client)
        }
        adapter.ask(profile, question)
    }
}
