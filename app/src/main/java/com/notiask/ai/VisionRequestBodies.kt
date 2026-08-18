package com.notiask.ai

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.Base64

/** Builds OpenAI-compatible / Anthropic JSON bodies for text, optionally with a JPEG screenshot. */
object VisionRequestBodies {
    private val json = Json { explicitNulls = false }

    const val DEFAULT_SCREENSHOT_QUESTION =
        "请根据这张截图回答：画面里是什么？请提取关键信息；如果有文字请一并整理。"

    fun openAi(model: String, text: String, imageJpeg: ByteArray?): String {
        val body = buildJsonObject {
            put("model", model)
            put("messages", buildJsonArray {
                add(buildJsonObject {
                    put("role", "user")
                    put("content", openAiContent(text, imageJpeg))
                })
            })
        }
        return json.encodeToString(JsonObject.serializer(), body)
    }

    fun anthropic(model: String, text: String, imageJpeg: ByteArray?, maxTokens: Int = 1024): String {
        val body = buildJsonObject {
            put("model", model)
            put("max_tokens", maxTokens)
            put("messages", buildJsonArray {
                add(buildJsonObject {
                    put("role", "user")
                    put("content", anthropicContent(text, imageJpeg))
                })
            })
        }
        return json.encodeToString(JsonObject.serializer(), body)
    }

    private fun openAiContent(text: String, imageJpeg: ByteArray?): JsonElement {
        if (imageJpeg == null) return JsonPrimitive(text)
        return buildJsonArray {
            add(buildJsonObject {
                put("type", "text")
                put("text", text)
            })
            add(buildJsonObject {
                put("type", "image_url")
                put("image_url", buildJsonObject {
                    put("url", "data:image/jpeg;base64,${encode(imageJpeg)}")
                })
            })
        }
    }

    private fun anthropicContent(text: String, imageJpeg: ByteArray?): JsonElement {
        if (imageJpeg == null) return JsonPrimitive(text)
        return buildJsonArray {
            add(buildJsonObject {
                put("type", "image")
                put("source", buildJsonObject {
                    put("type", "base64")
                    put("media_type", "image/jpeg")
                    put("data", encode(imageJpeg))
                })
            })
            add(buildJsonObject {
                put("type", "text")
                put("text", text)
            })
        }
    }

    private fun encode(bytes: ByteArray): String = Base64.getEncoder().encodeToString(bytes)
}
