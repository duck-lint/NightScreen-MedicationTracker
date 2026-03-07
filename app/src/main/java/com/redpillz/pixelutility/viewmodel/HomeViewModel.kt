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
import com.redpillz.pixelutility.data.model.TodayMedicationTotal
import com.redpillz.pixelutility.data.repository.DoseEntryRepository
import com.redpillz.pixelutility.data.repository.MedicationRepository
import com.redpillz.pixelutility.overlay.OverlayController
import com.redpillz.pixelutility.overlay.OverlayPreferenceStore
import com.redpillz.pixelutility.overlay.OverlayPreferences
import com.redpillz.pixelutility.overlay.OverlayRuntimeState
import com.redpillz.pixelutility.overlay.OverlayStatusResolver
import java.time.Clock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class QuickLogFormState(
    val selectedMedicationId: Long? = null,
    val dosageInput: String = "",
)

data class HomeUiState(
    val activeMedications: List<Medication> = emptyList(),
    val quickLog: QuickLogFormState = QuickLogFormState(),
    val todayEntries: List<DoseEntryRecord> = emptyList(),
    val todayTotals: List<TodayMedicationTotal> = emptyList(),
    val overlayHeadline: String = "",
    val overlayDetail: String = "",
    val overlayCanStartFromHome: Boolean = false,
    val overlayPermissionGranted: Boolean = false,
    val infoMessage: String? = null,
)

class HomeViewModel(
    private val medicationRepository: MedicationRepository,
    private val doseEntryRepository: DoseEntryRepository,
    private val validator: DoseEntryValidator,
    private val calculator: TodayDoseSummaryCalculator,
    private val overlayPreferencesRepository: OverlayPreferenceStore,
    private val overlayController: OverlayController,
    private val overlayStatusResolver: OverlayStatusResolver,
    private val clock: Clock,
) : ViewModel() {
    private val quickLogForm = MutableStateFlow(QuickLogFormState())
    private val infoMessage = MutableStateFlow<String?>(null)
    private val overlayPermission = MutableStateFlow(overlayController.permissionGranted())

    val uiState: StateFlow<HomeUiState> = combine(
        medicationRepository.observeActive(),
        doseEntryRepository.observeAllEntries(),
        quickLogForm,
        infoMessage,
        overlayPreferencesRepository.preferences,
        overlayController.runtimeState,
        overlayPermission,
    ) { args ->
        @Suppress("UNCHECKED_CAST")
        val medications = args[0] as List<Medication>
        @Suppress("UNCHECKED_CAST")
        val entries = args[1] as List<DoseEntryRecord>
        val form = args[2] as QuickLogFormState
        val message = args[3] as String?
        val overlayPreferences = args[4] as OverlayPreferences
        val runtimeState = args[5] as OverlayRuntimeState
        val permissionGranted = args[6] as Boolean

        val effectiveMedicationId = form.selectedMedicationId ?: medications.firstOrNull()?.id
        val overlayStatus = overlayStatusResolver.resolve(
            permissionGranted = permissionGranted,
            preferences = overlayPreferences,
            runtimeState = runtimeState,
        )

        HomeUiState(
            activeMedications = medications,
            quickLog = form.copy(selectedMedicationId = effectiveMedicationId),
            todayEntries = calculator.todayEntries(entries),
            todayTotals = calculator.todayTotals(entries),
            overlayHeadline = overlayStatus.headline,
            overlayDetail = overlayStatus.detail,
            overlayCanStartFromHome = false,
            overlayPermissionGranted = permissionGranted,
            infoMessage = message,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = HomeUiState(),
    )

    fun refreshOverlayPermission() {
        overlayPermission.value = overlayController.permissionGranted()
    }

    fun updateMedicationSelection(id: Long) {
        quickLogForm.update { it.copy(selectedMedicationId = id) }
    }

    fun updateDosageInput(value: String) {
        quickLogForm.update { it.copy(dosageInput = value) }
    }

    fun dismissMessage() {
        infoMessage.value = null
    }

    fun quickLogNow() {
        viewModelScope.launch {
            val state = uiState.value
            val validation = validator.validate(
                DoseValidationInput(
                    medicationId = state.quickLog.selectedMedicationId,
                    dosageText = state.quickLog.dosageInput,
                    takenAt = clock.instant(),
                ),
            )
            when (validation) {
                is DoseValidationResult.Invalid -> infoMessage.value = validation.message
                DoseValidationResult.Valid -> {
                    val result = doseEntryRepository.createEntry(
                        DoseEntryDraft(
                            medicationId = state.quickLog.selectedMedicationId!!,
                            dosageMg = state.quickLog.dosageInput.toDouble(),
                            takenAt = clock.instant(),
                            notes = null,
                        ),
                    )
                    infoMessage.value = result.exceptionOrNull()?.message ?: "Dose logged for today."
                    if (result.isSuccess) {
                        quickLogForm.value = QuickLogFormState(selectedMedicationId = state.quickLog.selectedMedicationId)
                    }
                }
            }
        }
    }
}
