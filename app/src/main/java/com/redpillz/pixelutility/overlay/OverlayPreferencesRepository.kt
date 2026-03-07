package com.redpillz.pixelutility.overlay

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

private val Context.overlayPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "overlay_preferences",
)

data class OverlayPreferences(
    val enabled: Boolean = false,
    val alpha: Float = 0.45f,
)

interface OverlayPreferenceStore {
    val preferences: StateFlow<OverlayPreferences>

    suspend fun setEnabled(enabled: Boolean)

    suspend fun setAlpha(alpha: Float)
}

class OverlayPreferencesRepository(
    context: Context,
    scope: CoroutineScope,
) : OverlayPreferenceStore {
    private val dataStore = context.overlayPreferencesDataStore

    override val preferences: StateFlow<OverlayPreferences> = dataStore.data
        .map { preferences ->
            OverlayPreferences(
                enabled = preferences[Keys.Enabled] ?: false,
                alpha = (preferences[Keys.Alpha] ?: 0.45f).coerceIn(0.1f, 0.8f),
            )
        }
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = OverlayPreferences(),
        )

    override suspend fun setEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[Keys.Enabled] = enabled
        }
    }

    override suspend fun setAlpha(alpha: Float) {
        dataStore.edit { preferences ->
            preferences[Keys.Alpha] = alpha.coerceIn(0.1f, 0.8f)
        }
    }

    private object Keys {
        val Enabled = booleanPreferencesKey("overlay_enabled")
        val Alpha = floatPreferencesKey("overlay_alpha")
    }
}
