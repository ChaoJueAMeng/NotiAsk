package com.notiask.data

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
enum class ProviderKind(val displayName: String, val defaultBaseUrl: String, val defaultModel: String) {
    OPENAI("OpenAI", "https://api.openai.com/v1", "gpt-4.1-mini"),
    ANTHROPIC("Anthropic", "https://api.anthropic.com", "claude-sonnet-4-20250514"),
    DASHSCOPE("通义千问 / DashScope", "https://dashscope.aliyuncs.com/compatible-mode/v1", "qwen-plus"),
    DEEPSEEK("DeepSeek", "https://api.deepseek.com", "deepseek-v4-flash"),
    KIMI("Kimi / Moonshot", "https://api.moonshot.cn/v1", "kimi-k2.6"),
    COMPATIBLE("OpenAI 兼容接口", "", "")
}

@Serializable
data class AiProfile(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val provider: ProviderKind = ProviderKind.OPENAI,
    val baseUrl: String = provider.defaultBaseUrl,
    val model: String = provider.defaultModel,
)

data class ConfiguredProfile(val profile: AiProfile, val apiKey: String)
