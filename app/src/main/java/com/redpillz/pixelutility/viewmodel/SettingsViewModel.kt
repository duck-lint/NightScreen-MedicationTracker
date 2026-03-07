package com.redpillz.pixelutility.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.redpillz.pixelutility.data.model.Medication
import com.redpillz.pixelutility.data.repository.MedicationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val medications: List<Medication> = emptyList(),
    val newMedicationName: String = "",
    val renameMedicationId: Long? = null,
    val renameMedicationName: String = "",
    val infoMessage: String? = null,
)

class SettingsViewModel(
    private val medicationRepository: MedicationRepository,
) : ViewModel() {
    private val editorState = MutableStateFlow(SettingsUiState())
    private val infoMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<SettingsUiState> = combine(
        medicationRepository.observeAll(),
        editorState,
        infoMessage,
    ) { medications, editor, message ->
        editor.copy(
            medications = medications,
            infoMessage = message,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = SettingsUiState(),
    )

    fun dismissMessage() {
        infoMessage.value = null
    }

    fun updateNewMedicationName(value: String) {
        editorState.update { it.copy(newMedicationName = value) }
    }

    fun beginRename(medication: Medication) {
        editorState.update {
            it.copy(
                renameMedicationId = medication.id,
                renameMedicationName = medication.name,
            )
        }
    }

    fun updateRenameMedicationName(value: String) {
        editorState.update { it.copy(renameMedicationName = value) }
    }

    fun cancelRename() {
        editorState.update { it.copy(renameMedicationId = null, renameMedicationName = "") }
    }

    fun saveMedication() {
        viewModelScope.launch {
            val name = uiState.value.newMedicationName
            val result = medicationRepository.createMedication(name)
            infoMessage.value = result.exceptionOrNull()?.message ?: "Medication added."
            if (result.isSuccess) {
                editorState.update { it.copy(newMedicationName = "") }
            }
        }
    }

    fun saveRename() {
        viewModelScope.launch {
            val state = uiState.value
            val id = state.renameMedicationId ?: return@launch
            val result = medicationRepository.renameMedication(id, state.renameMedicationName)
            infoMessage.value = result.exceptionOrNull()?.message ?: "Medication updated."
            if (result.isSuccess) {
                cancelRename()
            }
        }
    }

    fun toggleMedication(medication: Medication) {
        viewModelScope.launch {
            val result = medicationRepository.setMedicationActive(medication.id, !medication.isActive)
            infoMessage.value = result.exceptionOrNull()?.message
                ?: if (medication.isActive) "Medication archived." else "Medication reactivated."
        }
    }
}
