package com.notiask.ai

import com.notiask.data.AiProfile
import com.notiask.data.ConfiguredProfile
import com.notiask.data.ProviderKind
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/** Drives both adapters through a canned-response interceptor; no network, no extra test deps. */
class AdapterResponseParsingTest {
    private var lastRequest: Request? = null

    private fun clientReturning(code: Int, body: String): OkHttpClient =
        OkHttpClient.Builder().addInterceptor(Interceptor { chain ->
            lastRequest = chain.request()
            Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .code(code)
                .message(if (code < 400) "OK" else "Error")
                .body(body.toResponseBody("application/json".toMediaType()))
                .build()
        }).build()

    private fun profile(kind: ProviderKind, baseUrl: String) = ConfiguredProfile(
        AiProfile(name = "t", provider = kind, baseUrl = baseUrl, model = "m"),
        apiKey = "sk-test",
    )

    private fun requestBody(): String = Buffer().also { lastRequest!!.body!!.writeTo(it) }.readUtf8()

    private fun assertFails(expected: String, block: suspend () -> String) = runBlocking {
        try {
            block()
            fail("expected AiRequestException")
        } catch (e: AiRequestException) {
            assertEquals(expected, e.message)
        }
    }

    // ---- OpenAI-compatible ----

    @Test
    fun openAiParsesStringContentAndSendsBearerHeader() = runBlocking {
        val adapter = OpenAiCompatibleAdapter(clientReturning(200, """{"choices":[{"message":{"role":"assistant","content":" 你好 "}}]}"""))
        val answer = adapter.ask(profile(ProviderKind.OPENAI, "https://api.example.com/v1/"), "hi")
        assertEquals("你好", answer)
        assertEquals("https://api.example.com/v1/chat/completions", lastRequest!!.url.toString())
        assertEquals("Bearer sk-test", lastRequest!!.header("Authorization"))
        assertTrue(requestBody().contains("\"role\":\"system\""))
    }

    @Test
    fun openAiJoinsArrayContentParts() = runBlocking {
        val body = """{"choices":[{"message":{"content":[{"type":"text","text":"a"},{"type":"image_url","image_url":{}},{"type":"text","text":"b"}]}}]}"""
        val answer = OpenAiCompatibleAdapter(clientReturning(200, body)).ask(profile(ProviderKind.COMPATIBLE, "http://h/v1"), "q")
        assertEquals("a\nb", answer)
    }

    @Test
    fun openAiNullContentIsReportedAsEmptyAnswerNotParseError() {
        val body = """{"choices":[{"message":{"role":"assistant","content":null,"reasoning_content":"..."}}]}"""
        assertFails("服务未返回可用回答") {
            OpenAiCompatibleAdapter(clientReturning(200, body)).ask(profile(ProviderKind.DEEPSEEK, "http://h"), "q")
        }
    }

    @Test
    fun openAiGarbageBodyIsAParseError() {
        assertFails("服务返回了无法解析的内容") {
            OpenAiCompatibleAdapter(clientReturning(200, "<html>oops</html>")).ask(profile(ProviderKind.OPENAI, "http://h"), "q")
        }
    }

    @Test
    fun errorObjectMessageWins() {
        assertFails("model not found") {
            OpenAiCompatibleAdapter(clientReturning(404, """{"error":{"message":"model not found","type":"x"}}"""))
                .ask(profile(ProviderKind.OPENAI, "http://h"), "q")
        }
    }

    @Test
    fun bareStringErrorIsAccepted() {
        assertFails("quota exceeded") {
            OpenAiCompatibleAdapter(clientReturning(429, """{"error":"quota exceeded"}""")).ask(profile(ProviderKind.KIMI, "http://h"), "q")
        }
    }

    @Test
    fun unknownErrorShapeFallsBackToStatusText() {
        assertFails("接口地址不存在，请检查 Base URL（404）") {
            OpenAiCompatibleAdapter(clientReturning(404, "not found")).ask(profile(ProviderKind.OPENAI, "http://h"), "q")
        }
        assertFails("API Key 无效或没有权限") {
            OpenAiCompatibleAdapter(clientReturning(401, "")).ask(profile(ProviderKind.OPENAI, "http://h"), "q")
        }
        assertFails("AI 服务暂时不可用（503）") {
            OpenAiCompatibleAdapter(clientReturning(503, "{}")).ask(profile(ProviderKind.OPENAI, "http://h"), "q")
        }
    }

    // ---- Anthropic ----

    @Test
    fun anthropicSkipsThinkingBlocksAndJoinsText() = runBlocking {
        val body = """{"content":[{"type":"thinking","thinking":"..."},{"type":"text","text":"第一段"},{"type":"tool_use","id":"1"},{"type":"text","text":"第二段"}]}"""
        val adapter = AnthropicAdapter(clientReturning(200, body))
        val answer = adapter.ask(profile(ProviderKind.ANTHROPIC, "https://api.anthropic.com"), "q")
        assertEquals("第一段\n第二段", answer)
        assertEquals("https://api.anthropic.com/v1/messages", lastRequest!!.url.toString())
        assertEquals("sk-test", lastRequest!!.header("x-api-key"))
        assertEquals("2023-06-01", lastRequest!!.header("anthropic-version"))
        assertTrue(requestBody().contains("\"system\":"))
    }

    @Test
    fun anthropicWithoutTextBlocksIsEmptyAnswer() {
        assertFails("服务未返回可用回答") {
            AnthropicAdapter(clientReturning(200, """{"content":[{"type":"thinking","thinking":"x"}]}"""))
                .ask(profile(ProviderKind.ANTHROPIC, "http://h"), "q")
        }
    }

    @Test
    fun anthropicErrorEnvelopeIsParsed() {
        assertFails("invalid x-api-key") {
            AnthropicAdapter(clientReturning(401, """{"type":"error","error":{"type":"authentication_error","message":"invalid x-api-key"}}"""))
                .ask(profile(ProviderKind.ANTHROPIC, "http://h"), "q")
        }
    }
}
