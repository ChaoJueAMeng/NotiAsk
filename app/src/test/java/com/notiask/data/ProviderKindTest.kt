package com.notiask.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProviderKindTest {
    @Test
    fun deepSeekDefaultsMatchOfficialOpenAiCompatibleApi() {
        assertEquals("DeepSeek", ProviderKind.DEEPSEEK.displayName)
        assertEquals("https://api.deepseek.com", ProviderKind.DEEPSEEK.defaultBaseUrl)
        assertEquals("deepseek-v4-flash", ProviderKind.DEEPSEEK.defaultModel)
    }

    @Test
    fun kimiDefaultsMatchOfficialOpenAiCompatibleApi() {
        assertEquals("Kimi / Moonshot", ProviderKind.KIMI.displayName)
        assertEquals("https://api.moonshot.cn/v1", ProviderKind.KIMI.defaultBaseUrl)
        assertEquals("kimi-k2.6", ProviderKind.KIMI.defaultModel)
    }

    @Test
    fun dropdownIncludesDeepSeekAndKimi() {
        val names = ProviderKind.entries.map { it.name }
        assertTrue(names.contains("DEEPSEEK"))
        assertTrue(names.contains("KIMI"))
    }
}
