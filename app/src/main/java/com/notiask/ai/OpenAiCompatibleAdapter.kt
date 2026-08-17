package com.notiask.ai

import com.notiask.data.ConfiguredProfile
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class OpenAiCompatibleAdapter(private val client: OkHttpClient) : AiServiceAdapter {
    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }

    override suspend fun ask(profile: ConfiguredProfile, question: String): String {
        val body = json.encodeToString(
            ChatCompletionRequest.serializer(),
            ChatCompletionRequest(model = profile.profile.model, messages = listOf(ChatMessage("user", question)))
        )
        val request = Request.Builder()
            .url(profile.profile.baseUrl.trimEnd('/') + "/chat/completions")
            .header("Authorization", "Bearer ${profile.apiKey}")
            .header("Content-Type", "application/json")
            .post(body.toRequestBody(JSON))
            .build()
        return client.newCall(request).execute().use { response ->
            val raw = response.body.string()
            if (!response.isSuccessful) throw AiRequestException(readError(raw, response.code))
            val content = runCatching { json.decodeFromString(ChatCompletionResponse.serializer(), raw).choices.firstOrNull()?.message?.content }
                .getOrNull()?.trim()
            content?.takeIf { it.isNotEmpty() } ?: throw AiRequestException("服务未返回可用回答")
        }
    }

    private fun readError(raw: String, code: Int): String {
        val detail = runCatching { json.decodeFromString(ApiError.serializer(), raw).error.message }.getOrNull()
        return detail ?: when (code) {
            401, 403 -> "API Key 无效或没有权限"
            429 -> "请求过于频繁或额度不足"
            in 500..599 -> "AI 服务暂时不可用（$code）"
            else -> "AI 服务请求失败（$code）"
        }
    }

    @Serializable private data class ChatCompletionRequest(val model: String, val messages: List<ChatMessage>)
    @Serializable private data class ChatMessage(val role: String, val content: String)
    @Serializable private data class ChatCompletionResponse(val choices: List<Choice> = emptyList())
    @Serializable private data class Choice(val message: ChatMessage)
    @Serializable private data class ApiError(val error: ErrorBody)
    @Serializable private data class ErrorBody(val message: String? = null)
    private companion object { val JSON = "application/json; charset=utf-8".toMediaType() }
}
