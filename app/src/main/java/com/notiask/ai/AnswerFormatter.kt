package com.notiask.ai

/**
 * Notifications render plain text only, so strip the Markdown that models still
 * emit despite the system prompt. Conservative on purpose: it removes markup,
 * never rewrites wording.
 */
object AnswerFormatter {
    private val fence = Regex("^\\s*(```|~~~).*$")
    private val heading = Regex("^\\s{0,3}#{1,6}\\s+")
    private val blockquote = Regex("^\\s{0,3}>\\s?")
    private val bullet = Regex("^(\\s*)[-*+]\\s+")
    private val horizontalRule = Regex("^\\s*([-*_])(\\s*\\1){2,}\\s*$")
    private val tableSeparator = Regex("^\\s*\\|?\\s*:?-{2,}:?\\s*(\\|\\s*:?-{2,}:?\\s*)*\\|?\\s*$")
    private val tableCell = Regex("\\s*\\|\\s*")
    private val image = Regex("!\\[([^\\]]*)]\\([^)]*\\)")
    private val link = Regex("\\[([^\\]]+)]\\(([^)\\s]+)(?:\\s+\"[^\"]*\")?\\)")
    private val boldAsterisk = Regex("\\*\\*(?=\\S)(.+?)(?<=\\S)\\*\\*")
    private val boldUnderscore = Regex("__(?=\\S)(.+?)(?<=\\S)__")
    private val italicAsterisk = Regex("(?<![*\\w])\\*(?=\\S)([^*\\n]+?)(?<=\\S)\\*(?![*\\w])")
    private val italicUnderscore = Regex("(?<![\\w_])_(?=\\S)([^_\\n]+?)(?<=\\S)_(?![\\w_])")
    private val strike = Regex("~~(?=\\S)(.+?)(?<=\\S)~~")
    private val inlineCode = Regex("`([^`\\n]+)`")
    private val escape = Regex("\\\\([\\\\`*_{}\\[\\]()#+\\-.!>|~])")
    private val blankRuns = Regex("\\n{3,}")

    fun plain(markdown: String): String {
        if (markdown.isBlank()) return markdown.trim()
        val lines = markdown.replace("\r\n", "\n").split('\n')
        val out = StringBuilder(markdown.length)
        var inFence = false
        for (rawLine in lines) {
            if (fence.matches(rawLine)) {
                inFence = !inFence
                continue // 丢掉围栏行，保留代码本身
            }
            if (inFence) {
                out.append(rawLine).append('\n')
                continue
            }
            if (horizontalRule.matches(rawLine) || tableSeparator.matches(rawLine)) continue
            out.append(inlineMarkup(blockMarkup(rawLine))).append('\n')
        }
        return blankRuns.replace(out.toString(), "\n\n").trim()
    }

    private fun blockMarkup(line: String): String {
        var s = heading.replace(line, "")
        s = blockquote.replace(s, "")
        s = bullet.replace(s, "$1• ")
        if (s.trimStart().startsWith("|")) {
            s = s.trim().removePrefix("|").removeSuffix("|").replace(tableCell, "  ").trim()
        }
        return s
    }

    private fun inlineMarkup(line: String): String {
        // 先把 \* \_ 这类转义字符换成占位符，避免被当成强调标记吃掉，最后再还原。
        val escaped = ArrayList<Char>()
        var s = escape.replace(line) { match -> escaped += match.groupValues[1][0]; ESC.toString() }
        s = image.replace(s, "$1")
        s = link.replace(s, "$1 ($2)")
        s = inlineCode.replace(s, "$1")
        s = boldAsterisk.replace(s, "$1")
        s = boldUnderscore.replace(s, "$1")
        s = strike.replace(s, "$1")
        s = italicAsterisk.replace(s, "$1")
        s = italicUnderscore.replace(s, "$1")
        if (escaped.isEmpty()) return s
        var next = 0
        return buildString(s.length) {
            for (ch in s) append(if (ch == ESC && next < escaped.size) escaped[next++] else ch)
        }
    }

    private const val ESC = '\uE000'
}
