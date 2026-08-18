package com.notiask.screenshot

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ScreenshotSession {
    private val _state = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State> = _state.asStateFlow()

    @Volatile
    private var pendingAskImage: ByteArray? = null

    fun capturing() {
        _state.value = State.Capturing
    }

    fun ready(jpeg: ByteArray) {
        _state.value = State.Ready(jpeg)
    }

    fun failed(message: String) {
        _state.value = State.Failed(message)
    }

    fun reset() {
        _state.value = State.Idle
        pendingAskImage = null
    }

    fun idleKeepingPending() {
        _state.value = State.Idle
    }

    fun setPendingAskImage(jpeg: ByteArray) {
        pendingAskImage = jpeg
    }

    fun takePendingAskImage(): ByteArray? {
        val image = pendingAskImage
        pendingAskImage = null
        return image
    }

    sealed class State {
        data object Idle : State()
        data object Capturing : State()
        data class Ready(val jpeg: ByteArray) : State()
        data class Failed(val message: String) : State()
    }
}
