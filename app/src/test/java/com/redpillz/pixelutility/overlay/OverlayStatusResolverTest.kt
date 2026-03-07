package com.redpillz.pixelutility.overlay

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OverlayStatusResolverTest {
    private val resolver = OverlayStatusResolver()

    @Test
    fun reportsPermissionRequirementFirst() {
        val summary = resolver.resolve(
            permissionGranted = false,
            preferences = OverlayPreferences(enabled = true, alpha = 0.4f),
            runtimeState = OverlayRuntimeState(),
        )

        assertEquals("Permission required", summary.headline)
        assertFalse(summary.canStart)
    }

    @Test
    fun reportsReadyWhenPermissionGrantedAndEnabled() {
        val summary = resolver.resolve(
            permissionGranted = true,
            preferences = OverlayPreferences(enabled = true, alpha = 0.4f),
            runtimeState = OverlayRuntimeState(),
        )

        assertEquals("Ready to start", summary.headline)
        assertTrue(summary.canStart)
    }
}

