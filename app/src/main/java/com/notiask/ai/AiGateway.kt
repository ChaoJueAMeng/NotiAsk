package com.notiask.ai

import com.notiask.data.ConfiguredProfile
import com.notiask.data.ProviderKind
import kotlinx.coroutines.CancellationException
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class AiGateway {
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .callTimeout(100, TimeUnit.SECONDS)
        .build()
    private val anthropic = AnthropicAdapter(client)
    private val openAiCompatible = OpenAiCompatibleAdapter(client)

    suspend fun ask(profile: ConfiguredProfile, question: String, imageJpeg: ByteArray? = null): Result<String> {
        val adapter: AiServiceAdapter = when (profile.profile.provider) {
            ProviderKind.ANTHROPIC -> anthropic
            ProviderKind.OPENAI,
            ProviderKind.DASHSCOPE,
            ProviderKind.DEEPSEEK,
            ProviderKind.KIMI,
            ProviderKind.COMPATIBLE -> openAiCompatible
        }
        return try {
            Result.success(adapter.ask(profile, question, imageJpeg))
        } catch (e: CancellationException) {
            throw e // 协程取消必须向上传播，不能当成业务失败吞掉
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
