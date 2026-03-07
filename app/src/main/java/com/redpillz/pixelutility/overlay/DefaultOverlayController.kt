package com.redpillz.pixelutility.overlay

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.redpillz.pixelutility.service.OverlayService

class DefaultOverlayController(
    context: Context,
    private val preferencesRepository: OverlayPreferenceStore,
    private val runtimeStore: OverlayRuntimeStore,
) : OverlayController {
    private val appContext = context.applicationContext

    override val runtimeState = runtimeStore.state

    override fun permissionGranted(): Boolean = Settings.canDrawOverlays(appContext)

    override suspend fun startFromVisibleUi(): Result<Unit> {
        if (!permissionGranted()) {
            return Result.failure(IllegalStateException("Overlay permission is not granted."))
        }
        val preferences = preferencesRepository.preferences.value
        if (!preferences.enabled) {
            return Result.failure(IllegalStateException("Enable the overlay before starting it."))
        }

        ContextCompat.startForegroundService(
            appContext,
            OverlayService.createStartIntent(appContext, preferences.alpha),
        )
        return Result.success(Unit)
    }

    override suspend fun stop(): Result<Unit> {
        appContext.stopService(Intent(appContext, OverlayService::class.java))
        runtimeStore.setStopped("Overlay stop requested.")
        return Result.success(Unit)
    }

    override suspend fun refresh(): Result<Unit> {
        val preferences = preferencesRepository.preferences.value
        return if (runtimeState.value.isRunning && permissionGranted() && preferences.enabled) {
            appContext.startService(OverlayService.createUpdateIntent(appContext, preferences.alpha))
            Result.success(Unit)
        } else if (runtimeState.value.isRunning) {
            stop()
        } else {
            Result.success(Unit)
        }
    }
}
