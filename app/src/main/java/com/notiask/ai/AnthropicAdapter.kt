package com.notiask.ai

import com.notiask.data.ConfiguredProfile
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class AnthropicAdapter(private val client: OkHttpClient) : AiServiceAdapter {
    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }

    override suspend fun ask(profile: ConfiguredProfile, question: String): String {
        val payload = json.encodeToString(
            AnthropicRequest.serializer(),
            AnthropicRequest(profile.profile.model, messages = listOf(AnthropicMessage("user", question)))
        )
        val request = Request.Builder()
            .url(profile.profile.baseUrl.trimEnd('/') + "/v1/messages")
            .header("x-api-key", profile.apiKey)
            .header("anthropic-version", "2023-06-01")
            .header("Content-Type", "application/json")
            .post(payload.toRequestBody(JSON))
            .build()
        return client.newCall(request).execute().use { response ->
            val raw = response.body.string()
            if (!response.isSuccessful) throw AiRequestException(readError(raw, response.code))
            val text = runCatching { json.decodeFromString(AnthropicResponse.serializer(), raw).content.firstOrNull()?.text }.getOrNull()
            text?.trim()?.takeIf { it.isNotEmpty() } ?: throw AiRequestException("服务未返回可用回答")
        }
    }

    private fun readError(raw: String, code: Int): String {
        val detail = runCatching { json.decodeFromString(AnthropicError.serializer(), raw).error.message }.getOrNull()
        return detail ?: when (code) {
            401, 403 -> "API Key 无效或没有权限"
            429 -> "请求过于频繁或额度不足"
            else -> "Anthropic 请求失败（$code）"
        }
    }

    @Serializable private data class AnthropicRequest(val model: String, val max_tokens: Int = 1024, val messages: List<AnthropicMessage>)
    @Serializable private data class AnthropicMessage(val role: String, val content: String)
    @Serializable private data class AnthropicResponse(val content: List<ContentBlock> = emptyList())
    @Serializable private data class ContentBlock(val type: String, val text: String? = null)
    @Serializable private data class AnthropicError(val error: ErrorBody)
    @Serializable private data class ErrorBody(val message: String? = null)
    private companion object { val JSON = "application/json; charset=utf-8".toMediaType() }
}
