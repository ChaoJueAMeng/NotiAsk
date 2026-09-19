package com.notiask.ai

import org.junit.Assert.assertEquals
import org.junit.Test

class AnswerFormatterTest {
    @Test
    fun stripsHeadingsBoldAndInlineCode() {
        val input = "## 结论\n\n**Kotlin** 里用 `val` 声明只读变量。"
        assertEquals("结论\n\nKotlin 里用 val 声明只读变量。", AnswerFormatter.plain(input))
    }

    @Test
    fun turnsBulletsIntoDotsAndKeepsNumbering() {
        val input = "- 第一点\n* 第二点\n1. 第三点"
        assertEquals("• 第一点\n• 第二点\n1. 第三点", AnswerFormatter.plain(input))
    }

    @Test
    fun dropsCodeFencesButKeepsCode() {
        val input = "示例：\n```kotlin\nval x = 1 * 2\n```\n完。"
        assertEquals("示例：\nval x = 1 * 2\n完。", AnswerFormatter.plain(input))
    }

    @Test
    fun flattensLinksImagesAndTables() {
        val input = "见 [官网](https://example.com)。\n![图](https://x/y.png)\n| 名称 | 值 |\n|---|---|\n| a | 1 |"
        assertEquals("见 官网 (https://example.com)。\n图\n名称  值\na  1", AnswerFormatter.plain(input))
    }

    @Test
    fun leavesMathAndIdentifiersAlone() {
        val input = "结果是 2*3*4=24，变量名 snake_case_name 不变。"
        assertEquals(input, AnswerFormatter.plain(input))
    }

    @Test
    fun removesRulesQuotesAndEscapes() {
        val input = "> 引用\n---\n价格 \\*含税\\*\n\n\n\n结束"
        assertEquals("引用\n价格 *含税*\n\n结束", AnswerFormatter.plain(input))
    }

    @Test
    fun plainTextPassesThroughUnchanged() {
        val input = "今天多云，最高 26 度。"
        assertEquals(input, AnswerFormatter.plain(input))
    }
}
