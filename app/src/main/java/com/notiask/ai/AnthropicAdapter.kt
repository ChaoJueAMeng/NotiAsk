package com.notiask.ai

import com.notiask.data.ConfiguredProfile
import kotlinx.serialization.Serializable
import okhttp3.OkHttpClient

class AnthropicAdapter(client: OkHttpClient) : JsonHttpAdapter(client) {

    override suspend fun ask(profile: ConfiguredProfile, question: String, imageJpeg: ByteArray?): String {
        val request = jsonPost(
            url = profile.profile.baseUrl.trimEnd('/') + "/v1/messages",
            body = VisionRequestBodies.anthropic(profile.profile.model, question, imageJpeg),
            headers = mapOf(
                "x-api-key" to profile.apiKey,
                "anthropic-version" to "2023-06-01",
            ),
        )
        val raw = executeForBody(request)
        val parsed = decodeOrThrow { json.decodeFromString(AnthropicResponse.serializer(), raw) }
        // Skip thinking/tool_use blocks and stitch every text block together.
        val text = parsed.content
            .filter { it.type == "text" }
            .mapNotNull { it.text }
            .joinToString("\n")
            .trim()
        return text.ifEmpty { throw AiRequestException("服务未返回可用回答") }
    }

    @Serializable private data class AnthropicResponse(val content: List<ContentBlock> = emptyList())
    @Serializable private data class ContentBlock(val type: String = "", val text: String? = null)
}
