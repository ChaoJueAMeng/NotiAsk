package com.notiask.ui.backdrop

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import com.notiask.R
import com.notiask.data.ProviderKind

enum class ModelMark(
    val provider: ProviderKind,
    @param:DrawableRes val iconRes: Int,
    val glow: Color,
) {
    OPENAI(ProviderKind.OPENAI, R.drawable.ic_mark_openai, Color(0xFF10A37F)),
    CLAUDE(ProviderKind.ANTHROPIC, R.drawable.ic_mark_claude, Color(0xFFD97757)),
    QWEN(ProviderKind.DASHSCOPE, R.drawable.ic_mark_qwen, Color(0xFF6D5CFF)),
    DEEPSEEK(ProviderKind.DEEPSEEK, R.drawable.ic_mark_deepseek, Color(0xFF4D6BFE)),
    KIMI(ProviderKind.KIMI, R.drawable.ic_mark_kimi, Color(0xFF2563EB)),
    COMPATIBLE(ProviderKind.COMPATIBLE, R.drawable.ic_mark_compatible, Color(0xFF64748B));

    companion object {
        fun forProvider(kind: ProviderKind): ModelMark =
            entries.first { it.provider == kind }
    }
}
