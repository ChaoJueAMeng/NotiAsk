package com.notiask.screenshot

class ScreenshotSession {
    @Volatile
    private var pendingAskImage: ByteArray? = null

    fun setPendingAskImage(jpeg: ByteArray) {
        pendingAskImage = jpeg
    }

    fun takePendingAskImage(): ByteArray? {
        val image = pendingAskImage
        pendingAskImage = null
        return image
    }

    fun clear() {
        pendingAskImage = null
    }
}
