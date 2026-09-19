package com.notiask.ai

import com.notiask.data.ConfiguredProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/** A protocol adapter; adding a provider normally only requires another implementation here. */
interface AiServiceAdapter {
    suspend fun ask(profile: ConfiguredProfile, question: String, imageJpeg: ByteArray? = null): String
}

class AiRequestException(message: String) : Exception(message)

/** Shared JSON-over-HTTP plumbing: request execution, error-body parsing and HTTP status fallbacks. */
abstract class JsonHttpAdapter(private val client: OkHttpClient) : AiServiceAdapter {
    protected val json = Json { ignoreUnknownKeys = true; explicitNulls = false }

    protected fun jsonPost(url: String, body: String, headers: Map<String, String>): Request =
        Request.Builder()
            .url(url)
            .apply { headers.forEach { (name, value) -> header(name, value) } }
            .header("Content-Type", "application/json")
            .post(body.toRequestBody(JSON))
            .build()

    /** Runs the call off the caller's dispatcher and returns the raw body, or throws a user-facing error. */
    protected suspend fun executeForBody(request: Request): String = withContext(Dispatchers.IO) {
        client.newCall(request).execute().use { response ->
            val raw = response.body.string()
            if (!response.isSuccessful) throw AiRequestException(readError(raw, response.code))
            raw
        }
    }

    protected inline fun <T> decodeOrThrow(block: () -> T): T =
        runCatching(block).getOrElse { throw AiRequestException("服务返回了无法解析的内容") }

    private fun readError(raw: String, code: Int): String {
        val detail = runCatching { json.decodeFromString(ApiError.serializer(), raw).error.messageOrNull() }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }
        return detail ?: when (code) {
            401, 403 -> "API Key 无效或没有权限"
            404 -> "接口地址不存在，请检查 Base URL（$code）"
            429 -> "请求过于频繁或额度不足"
            in 500..599 -> "AI 服务暂时不可用（$code）"
            else -> "AI 服务请求失败（$code）"
        }
    }

    /** Providers return either `{"error": {"message": "..."}}` or the bare `{"error": "..."}`. */
    private fun JsonElement?.messageOrNull(): String? = when (this) {
        is JsonObject -> (this["message"] as? JsonPrimitive)?.contentOrNull
        is JsonPrimitive -> if (isString) content else null
        else -> null
    }

    @Serializable private data class ApiError(val error: JsonElement? = null)

    private companion object { val JSON = "application/json; charset=utf-8".toMediaType() }
}
