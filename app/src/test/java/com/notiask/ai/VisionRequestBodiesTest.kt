package com.notiask.ai

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VisionRequestBodiesTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun openAiTextOnlyKeepsStringContent() {
        val body = json.parseToJsonElement(VisionRequestBodies.openAi("gpt-4.1-mini", "你好", null)).jsonObject
        val content = body["messages"]!!.jsonArray.first().jsonObject["content"]!!.jsonPrimitive.content
        assertEquals("你好", content)
        assertFalse(body.toString().contains("image_url"))
    }

    @Test
    fun openAiWithJpegUsesImageUrlDataUri() {
        val jpeg = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0x01, 0x02)
        val body = json.parseToJsonElement(VisionRequestBodies.openAi("gpt-4.1-mini", "这是什么", jpeg)).jsonObject
        val content = body["messages"]!!.jsonArray.first().jsonObject["content"]!!.jsonArray
        assertEquals("text", content[0].jsonObject["type"]!!.jsonPrimitive.content)
        assertEquals("这是什么", content[0].jsonObject["text"]!!.jsonPrimitive.content)
        assertEquals("image_url", content[1].jsonObject["type"]!!.jsonPrimitive.content)
        val url = content[1].jsonObject["image_url"]!!.jsonObject["url"]!!.jsonPrimitive.content
        assertTrue(url.startsWith("data:image/jpeg;base64,"))
        assertTrue(url.length > "data:image/jpeg;base64,".length)
    }

    @Test
    fun anthropicWithJpegPutsImageBeforeText() {
        val jpeg = byteArrayOf(0x01, 0x02, 0x03)
        val body = json.parseToJsonElement(VisionRequestBodies.anthropic("claude-sonnet-4-20250514", "请解读", jpeg)).jsonObject
        val content = body["messages"]!!.jsonArray.first().jsonObject["content"]!!.jsonArray
        assertEquals("image", content[0].jsonObject["type"]!!.jsonPrimitive.content)
        assertEquals("base64", content[0].jsonObject["source"]!!.jsonObject["type"]!!.jsonPrimitive.content)
        assertEquals("image/jpeg", content[0].jsonObject["source"]!!.jsonObject["media_type"]!!.jsonPrimitive.content)
        assertEquals("text", content[1].jsonObject["type"]!!.jsonPrimitive.content)
        assertEquals("请解读", content[1].jsonObject["text"]!!.jsonPrimitive.content)
        assertEquals(1024, body["max_tokens"]!!.jsonPrimitive.int)
    }
}
