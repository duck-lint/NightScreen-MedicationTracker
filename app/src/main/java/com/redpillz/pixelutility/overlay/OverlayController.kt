package com.redpillz.pixelutility.overlay

import kotlinx.coroutines.flow.StateFlow

interface OverlayController {
    val runtimeState: StateFlow<OverlayRuntimeState>

    fun permissionGranted(): Boolean

    suspend fun startFromVisibleUi(): Result<Unit>

    suspend fun stop(): Result<Unit>

    suspend fun refresh(): Result<Unit>
}

