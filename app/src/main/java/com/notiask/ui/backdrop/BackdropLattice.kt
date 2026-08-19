package com.notiask.ui.backdrop

import kotlin.math.cos
import kotlin.math.sin

/** 斜向循环平铺与漂浮位移的纯计算，便于单测。 */
object BackdropLattice {
    fun patternPeriodPx(tilePx: Float, markCount: Int): Float {
        require(markCount > 0)
        require(tilePx > 0f)
        return tilePx * markCount
    }

    fun wrap(offset: Float, period: Float): Float {
        require(period > 0f)
        val remainder = offset % period
        return if (remainder < 0f) remainder + period else remainder
    }

    fun markIndex(col: Int, row: Int, count: Int): Int {
        require(count > 0)
        return (col + row).mod(count)
    }

    fun cellOrigin(col: Int, row: Int, tilePx: Float, shiftPx: Float): Pair<Float, Float> {
        val x = col * tilePx - shiftPx
        val y = row * tilePx - shiftPx
        return x to y
    }

    fun columnCount(widthPx: Float, tilePx: Float, markCount: Int): Int =
        kotlin.math.ceil(widthPx / tilePx).toInt() + markCount + 2

    fun rowCount(heightPx: Float, tilePx: Float, markCount: Int): Int =
        kotlin.math.ceil(heightPx / tilePx).toInt() + markCount + 2

    fun floatDisplacement(timeRad: Float, seed: Int, amplitude: Float): Pair<Float, Float> {
        val phase = seed * 1.6180339887f
        val x = sin(timeRad + phase) * amplitude * 0.55f
        val y = cos(timeRad * 0.83f + phase * 1.27f) * amplitude
        return x to y
    }

    fun tiltDegrees(timeRad: Float, seed: Int): Float {
        val phase = seed * 0.973f
        return sin(timeRad * 0.7f + phase) * 7.5f
    }

    fun breatheScale(timeRad: Float, seed: Int): Float {
        val phase = seed * 1.324f
        return 1f + 0.045f * sin(timeRad * 0.9f + phase)
    }
}
