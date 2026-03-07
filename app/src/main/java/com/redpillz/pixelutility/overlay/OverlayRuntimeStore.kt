package com.redpillz.pixelutility.overlay

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class OverlayRuntimeState(
    val isRunning: Boolean = false,
    val alpha: Float = 0.0f,
    val statusMessage: String? = null,
)

class OverlayRuntimeStore {
    private val mutableState = MutableStateFlow(OverlayRuntimeState())
    val state: StateFlow<OverlayRuntimeState> = mutableState.asStateFlow()

    fun setRunning(alpha: Float) {
        mutableState.value = OverlayRuntimeState(
            isRunning = true,
            alpha = alpha,
            statusMessage = "Overlay running across other apps where Android allows it.",
        )
    }

    fun setStopped(message: String? = "Overlay stopped.") {
        mutableState.value = OverlayRuntimeState(
            isRunning = false,
            alpha = 0.0f,
            statusMessage = message,
        )
    }
}

