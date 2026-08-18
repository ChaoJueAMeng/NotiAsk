package com.notiask.screenshot

import android.graphics.Bitmap
import android.media.Image
import java.io.ByteArrayOutputStream

internal object ScreenshotBitmap {
    fun fromImage(image: Image): Bitmap {
        val width = image.width
        val height = image.height
        val plane = image.planes[0]
        val buffer = plane.buffer
        val pixelStride = plane.pixelStride
        val rowStride = plane.rowStride
        val rowPadding = rowStride - pixelStride * width
        buffer.rewind()
        val bitmap = Bitmap.createBitmap(
            width + if (pixelStride == 0) 0 else rowPadding / pixelStride,
            height,
            Bitmap.Config.ARGB_8888
        )
        bitmap.copyPixelsFromBuffer(buffer)
        if (bitmap.width == width) return bitmap
        val cropped = Bitmap.createBitmap(bitmap, 0, 0, width, height)
        bitmap.recycle()
        return cropped
    }

    fun toJpeg(bitmap: Bitmap, maxEdge: Int = 1280, quality: Int = 75): ByteArray {
        val scaled = scaleDown(bitmap, maxEdge)
        val out = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
        if (scaled !== bitmap) scaled.recycle()
        return out.toByteArray()
    }

    private fun scaleDown(bitmap: Bitmap, maxEdge: Int): Bitmap {
        val longest = maxOf(bitmap.width, bitmap.height)
        if (longest <= maxEdge) return bitmap
        val scale = maxEdge.toFloat() / longest
        return Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * scale).toInt().coerceAtLeast(1),
            (bitmap.height * scale).toInt().coerceAtLeast(1),
            true
        )
    }
}
