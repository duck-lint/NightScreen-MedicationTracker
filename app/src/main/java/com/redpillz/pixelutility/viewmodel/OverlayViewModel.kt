package com.redpillz.pixelutility.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.redpillz.pixelutility.overlay.OverlayController
import com.redpillz.pixelutility.overlay.OverlayPreferenceStore
import com.redpillz.pixelutility.overlay.OverlayStatusResolver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class OverlayUiState(
    val permissionGranted: Boolean = false,
    val enabled: Boolean = false,
    val alpha: Float = 0.45f,
    val isRunning: Boolean = false,
    val statusHeadline: String = "",
    val statusDetail: String = "",
    val infoMessage: String? = null,
)

class OverlayViewModel(
    private val preferencesRepository: OverlayPreferenceStore,
    private val overlayController: OverlayController,
    private val statusResolver: OverlayStatusResolver,
) : ViewModel() {
    private val permissionGranted = MutableStateFlow(overlayController.permissionGranted())
    private val infoMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<OverlayUiState> = combine(
        preferencesRepository.preferences,
        overlayController.runtimeState,
        permissionGranted,
        infoMessage,
    ) { preferences, runtimeState, permission, message ->
        val summary = statusResolver.resolve(permission, preferences, runtimeState)
        OverlayUiState(
            permissionGranted = permission,
            enabled = preferences.enabled,
            alpha = preferences.alpha,
            isRunning = runtimeState.isRunning,
            statusHeadline = summary.headline,
            statusDetail = summary.detail,
            infoMessage = message ?: runtimeState.statusMessage,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = OverlayUiState(),
    )

    fun refreshPermissionState() {
        permissionGranted.value = overlayController.permissionGranted()
    }

    fun dismissMessage() {
        infoMessage.value = null
    }

    fun setEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setEnabled(enabled)
            if (!enabled) {
                overlayController.stop()
            }
        }
    }

    fun setAlpha(alpha: Float) {
        viewModelScope.launch {
            preferencesRepository.setAlpha(alpha)
            overlayController.refresh()
        }
    }

    fun startOverlay() {
        viewModelScope.launch {
            val result = overlayController.startFromVisibleUi()
            infoMessage.value = result.exceptionOrNull()?.message ?: "Overlay start requested."
        }
    }

    fun stopOverlay() {
        viewModelScope.launch {
            val result = overlayController.stop()
            infoMessage.value = result.exceptionOrNull()?.message ?: "Overlay stop requested."
        }
    }
}
