package com.notiask.ui.backdrop

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.ContextCompat
import kotlin.math.roundToInt

internal fun createModelPatternBitmap(context: Context, tilePx: Int): ImageBitmap {
    val marks = ModelMark.entries
    val count = marks.size
    val size = tilePx * count
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    val iconPad = (tilePx * 0.18f).roundToInt()
    val glowRadius = tilePx * 0.42f

    for (row in 0 until count) {
        for (col in 0 until count) {
            val mark = marks[BackdropLattice.markIndex(col, row, count)]
            val cx = col * tilePx + tilePx / 2f
            val cy = row * tilePx + tilePx / 2f
            val packed = mark.glow.toArgb()
            val glow = android.graphics.Color.argb(
                96,
                android.graphics.Color.red(packed),
                android.graphics.Color.green(packed),
                android.graphics.Color.blue(packed),
            )
            glowPaint.shader = RadialGradient(
                cx,
                cy,
                glowRadius,
                intArrayOf(glow, android.graphics.Color.TRANSPARENT),
                floatArrayOf(0f, 1f),
                Shader.TileMode.CLAMP,
            )
            canvas.drawCircle(cx, cy, glowRadius, glowPaint)
            val drawable = ContextCompat.getDrawable(context, mark.iconRes)?.mutate() ?: continue
            drawable.setBounds(
                col * tilePx + iconPad,
                row * tilePx + iconPad,
                (col + 1) * tilePx - iconPad,
                (row + 1) * tilePx - iconPad,
            )
            drawable.alpha = 210
            drawable.draw(canvas)
        }
    }
    val softened = softenByDownsample(bitmap, factor = 3)
    if (softened !== bitmap) bitmap.recycle()
    return softened.asImageBitmap()
}

private fun softenByDownsample(src: Bitmap, factor: Int): Bitmap {
    val width = (src.width / factor).coerceAtLeast(1)
    val height = (src.height / factor).coerceAtLeast(1)
    val small = Bitmap.createScaledBitmap(src, width, height, true)
    val out = Bitmap.createScaledBitmap(small, src.width, src.height, true)
    if (small !== src && small !== out) small.recycle()
    return out
}
