package com.redpillz.pixelutility.overlay

data class OverlayStatusSummary(
    val headline: String,
    val detail: String,
    val canStart: Boolean,
)

class OverlayStatusResolver {
    fun resolve(
        permissionGranted: Boolean,
        preferences: OverlayPreferences,
        runtimeState: OverlayRuntimeState,
    ): OverlayStatusSummary {
        return when {
            !permissionGranted -> OverlayStatusSummary(
                headline = "Permission required",
                detail = "Grant Display over other apps access in Android settings before the system-wide red overlay can run.",
                canStart = false,
            )

            runtimeState.isRunning -> OverlayStatusSummary(
                headline = "Overlay active",
                detail = "The red overlay is running. Some system surfaces can still appear above it.",
                canStart = false,
            )

            !preferences.enabled -> OverlayStatusSummary(
                headline = "Overlay disabled",
                detail = "Turn on the overlay control first, then start it from this visible screen in v1.",
                canStart = false,
            )

            else -> OverlayStatusSummary(
                headline = "Ready to start",
                detail = "Overlay service may only be started from a visible in-app screen in v1.",
                canStart = true,
            )
        }
    }
}

