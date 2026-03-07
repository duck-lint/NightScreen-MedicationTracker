package com.redpillz.pixelutility.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.redpillz.pixelutility.data.DoseEntryValidator
import com.redpillz.pixelutility.data.DoseValidationInput
import com.redpillz.pixelutility.data.DoseValidationResult
import com.redpillz.pixelutility.data.TodayDoseSummaryCalculator
import com.redpillz.pixelutility.data.model.DoseEntryDraft
import com.redpillz.pixelutility.data.model.DoseEntryRecord
import com.redpillz.pixelutility.data.model.Medication
import com.redpillz.pixelutility.data.repository.DoseEntryRepository
import com.redpillz.pixelutility.data.repository.MedicationRepository
import java.time.Clock
import java.time.Instant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PillLogFormState(
    val editingEntryId: Long? = null,
    val selectedMedicationId: Long? = null,
    val dosageInput: String = "",
    val notesInput: String = "",
    val takenAt: Instant? = null,
)

data class PillLogUiState(
    val medicationOptions: List<Medication> = emptyList(),
    val form: PillLogFormState = PillLogFormState(),
    val todayEntries: List<DoseEntryRecord> = emptyList(),
    val infoMessage: String? = null,
)

class PillLogViewModel(
    private val medicationRepository: MedicationRepository,
    private val doseEntryRepository: DoseEntryRepository,
    private val validator: DoseEntryValidator,
    private val calculator: TodayDoseSummaryCalculator,
    private val clock: Clock,
) : ViewModel() {
    private val formState = MutableStateFlow(PillLogFormState(takenAt = clock.instant()))
    private val infoMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<PillLogUiState> = combine(
        medicationRepository.observeAll(),
        doseEntryRepository.observeAllEntries(),
        formState,
        infoMessage,
    ) { medications, entries, form, message ->
        val todayEntries = calculator.todayEntries(entries)
        val selectedMedicationId = form.selectedMedicationId
            ?: medications.firstOrNull { it.isActive }?.id
        val selectedInactiveMedication = medications.firstOrNull { it.id == selectedMedicationId && !it.isActive }
        val medicationOptions = medications.filter { it.isActive }.toMutableList().apply {
            if (selectedInactiveMedication != null && none { it.id == selectedInactiveMedication.id }) {
                add(0, selectedInactiveMedication)
            }
        }

        PillLogUiState(
            medicationOptions = medicationOptions,
            form = form.copy(
                selectedMedicationId = selectedMedicationId,
                takenAt = form.takenAt ?: clock.instant(),
            ),
            todayEntries = todayEntries,
            infoMessage = message,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = PillLogUiState(),
    )

    fun dismissMessage() {
        infoMessage.value = null
    }

    fun updateMedicationSelection(id: Long) {
        formState.update { it.copy(selectedMedicationId = id) }
    }

    fun updateDosageInput(value: String) {
        formState.update { it.copy(dosageInput = value) }
    }

    fun updateNotes(value: String) {
        formState.update { it.copy(notesInput = value) }
    }

    fun updateTakenAt(value: Instant) {
        formState.update { it.copy(takenAt = value) }
    }

    fun startEditing(entry: DoseEntryRecord) {
        formState.value = PillLogFormState(
            editingEntryId = entry.id,
            selectedMedicationId = entry.medicationId,
            dosageInput = entry.dosageMg.toString(),
            notesInput = entry.notes.orEmpty(),
            takenAt = entry.takenAt,
        )
    }

    fun cancelEditing() {
        formState.value = PillLogFormState(takenAt = clock.instant())
    }

    fun saveEntry() {
        viewModelScope.launch {
            val form = uiState.value.form
            when (val validation = validator.validate(
                DoseValidationInput(
                    medicationId = form.selectedMedicationId,
                    dosageText = form.dosageInput,
                    takenAt = form.takenAt,
                ),
            )) {
                is DoseValidationResult.Invalid -> infoMessage.value = validation.message
                DoseValidationResult.Valid -> {
                    val draft = DoseEntryDraft(
                        medicationId = form.selectedMedicationId!!,
                        dosageMg = form.dosageInput.toDouble(),
                        takenAt = form.takenAt!!,
                        notes = form.notesInput,
                    )
                    val result = if (form.editingEntryId == null) {
                        doseEntryRepository.createEntry(draft)
                    } else {
                        doseEntryRepository.updateEntry(form.editingEntryId, draft)
                    }
                    infoMessage.value = result.exceptionOrNull()?.message
                        ?: if (form.editingEntryId == null) "Dose entry saved." else "Dose entry updated."
                    if (result.isSuccess) {
                        formState.value = PillLogFormState(
                            selectedMedicationId = form.selectedMedicationId,
                            takenAt = clock.instant(),
                        )
                    }
                }
            }
        }
    }

    fun deleteEntry(id: Long) {
        viewModelScope.launch {
            val result = doseEntryRepository.deleteEntry(id)
            infoMessage.value = result.exceptionOrNull()?.message ?: "Dose entry deleted."
            if (formState.value.editingEntryId == id) {
                cancelEditing()
            }
        }
    }
}
