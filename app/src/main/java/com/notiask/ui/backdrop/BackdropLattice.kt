package com.notiask.ui.backdrop

import kotlin.math.cos
import kotlin.math.floor
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

    fun tileShift(shiftPx: Float, tilePx: Float): Int {
        require(tilePx > 0f)
        return floor(shiftPx / tilePx).toInt()
    }

    fun pixelOffset(shiftPx: Float, tilePx: Float): Float = wrap(shiftPx, tilePx)

    fun copiesNeeded(spanPx: Float, periodPx: Float): Int {
        require(periodPx > 0f)
        return kotlin.math.ceil((spanPx + periodPx) / periodPx).toInt() + 1
    }

    fun floatDisplacement(timeRad: Float, amplitude: Float): Pair<Float, Float> {
        val x = sin(timeRad) * amplitude * 0.55f
        val y = cos(timeRad * 0.83f) * amplitude
        return x to y
    }

    fun breatheScale(timeRad: Float): Float = 1f + 0.035f * sin(timeRad * 0.9f)
}
