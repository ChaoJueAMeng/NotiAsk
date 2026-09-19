package com.notiask.ai

import com.notiask.data.ConfiguredProfile
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import okhttp3.OkHttpClient

class OpenAiCompatibleAdapter(client: OkHttpClient) : JsonHttpAdapter(client) {

    override suspend fun ask(profile: ConfiguredProfile, question: String, imageJpeg: ByteArray?): String {
        val request = jsonPost(
            url = profile.profile.baseUrl.trimEnd('/') + "/chat/completions",
            body = VisionRequestBodies.openAi(profile.profile.model, question, imageJpeg),
            headers = mapOf("Authorization" to "Bearer ${profile.apiKey}"),
        )
        val raw = executeForBody(request)
        val parsed = decodeOrThrow { json.decodeFromString(ChatCompletionResponse.serializer(), raw) }
        val content = parsed.choices.firstOrNull()?.message?.content.asText()?.trim().orEmpty()
        return content.ifEmpty { throw AiRequestException("服务未返回可用回答") }
    }

    /**
     * `content` is usually a string, but compatible providers also send `null`
     * (reasoning / tool-call turns) or an array of `{"type":"text","text":...}` parts.
     */
    private fun JsonElement?.asText(): String? = when (this) {
        is JsonPrimitive -> if (isString) content else null
        is JsonArray -> mapNotNull { part ->
            val obj = part as? JsonObject ?: return@mapNotNull null
            if ((obj["type"] as? JsonPrimitive)?.contentOrNull != "text") return@mapNotNull null
            (obj["text"] as? JsonPrimitive)?.contentOrNull
        }.joinToString("\n").ifBlank { null }
        else -> null
    }

    @Serializable private data class ChatMessage(val content: JsonElement? = null)
    @Serializable private data class ChatCompletionResponse(val choices: List<Choice> = emptyList())
    @Serializable private data class Choice(val message: ChatMessage? = null)
}
