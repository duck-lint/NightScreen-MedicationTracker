package com.redpillz.pixelutility.viewmodel

import com.redpillz.pixelutility.MainDispatcherRule
import com.redpillz.pixelutility.data.DoseEntryValidator
import com.redpillz.pixelutility.data.TodayDoseSummaryCalculator
import com.redpillz.pixelutility.data.model.DoseEntryDraft
import com.redpillz.pixelutility.data.model.DoseEntryRecord
import com.redpillz.pixelutility.data.model.Medication
import com.redpillz.pixelutility.data.repository.DoseEntryRepository
import com.redpillz.pixelutility.data.repository.MedicationRepository
import com.redpillz.pixelutility.overlay.OverlayController
import com.redpillz.pixelutility.overlay.OverlayPreferenceStore
import com.redpillz.pixelutility.overlay.OverlayPreferences
import com.redpillz.pixelutility.overlay.OverlayRuntimeState
import com.redpillz.pixelutility.overlay.OverlayStatusResolver
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val clock = Clock.fixed(Instant.parse("2026-03-07T12:00:00Z"), ZoneOffset.UTC)

    @Test
    fun repoChangesUpdateTodayEntriesAndTotals() = runTest {
        val medicationRepository = FakeMedicationRepository()
        val doseEntryRepository = FakeDoseEntryRepository()
        medicationRepository.seed(
            Medication(
                id = 1L,
                name = "Magnesium",
                isActive = true,
                createdAt = clock.instant(),
                updatedAt = clock.instant(),
            ),
        )
        val viewModel = HomeViewModel(
            medicationRepository = medicationRepository,
            doseEntryRepository = doseEntryRepository,
            validator = DoseEntryValidator(),
            calculator = TodayDoseSummaryCalculator(clock, ZoneOffset.UTC),
            overlayPreferencesRepository = FakeOverlayPreferenceStore(),
            overlayController = FakeOverlayController(),
            overlayStatusResolver = OverlayStatusResolver(),
            clock = clock,
        )

        doseEntryRepository.createEntry(
            DoseEntryDraft(
                medicationId = 1L,
                dosageMg = 10.0,
                takenAt = clock.instant(),
                notes = null,
            ),
        )
        doseEntryRepository.createEntry(
            DoseEntryDraft(
                medicationId = 1L,
                dosageMg = 5.0,
                takenAt = clock.instant().plusSeconds(60),
                notes = null,
            ),
        )
        advanceUntilIdle()
        assertEquals(2, viewModel.uiState.value.todayEntries.size)
        assertEquals(15.0, viewModel.uiState.value.todayTotals.single().totalMg, 0.0)

        doseEntryRepository.updateEntry(
            id = 1L,
            draft = DoseEntryDraft(
                medicationId = 1L,
                dosageMg = 3.0,
                takenAt = clock.instant(),
                notes = null,
            ),
        )
        advanceUntilIdle()
        assertEquals(8.0, viewModel.uiState.value.todayTotals.single().totalMg, 0.0)

        doseEntryRepository.deleteEntry(2L)
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.todayEntries.size)
        assertEquals(3.0, viewModel.uiState.value.todayTotals.single().totalMg, 0.0)
    }
}

private class FakeMedicationRepository : MedicationRepository {
    private val medications = MutableStateFlow<List<Medication>>(emptyList())

    fun seed(vararg items: Medication) {
        medications.value = items.toList()
    }

    override fun observeAll(): Flow<List<Medication>> = medications

    override fun observeActive(): Flow<List<Medication>> = medications.map { items ->
        items.filter { it.isActive }
    }

    override suspend fun createMedication(name: String): Result<Unit> = Result.success(Unit)

    override suspend fun renameMedication(id: Long, name: String): Result<Unit> = Result.success(Unit)

    override suspend fun setMedicationActive(id: Long, isActive: Boolean): Result<Unit> = Result.success(Unit)
}

private class FakeDoseEntryRepository : DoseEntryRepository {
    private val entries = MutableStateFlow<List<DoseEntryRecord>>(emptyList())
    private var nextId = 1L

    override fun observeAllEntries(): Flow<List<DoseEntryRecord>> = entries

    override suspend fun createEntry(draft: DoseEntryDraft): Result<Unit> {
        val instant = draft.takenAt
        entries.value = (entries.value + DoseEntryRecord(
            id = nextId++,
            medicationId = draft.medicationId,
            medicationName = "Magnesium",
            medicationIsActive = true,
            dosageMg = draft.dosageMg,
            takenAt = draft.takenAt,
            notes = draft.notes,
            createdAt = instant,
            updatedAt = instant,
        )).sortedByDescending { it.takenAt }
        return Result.success(Unit)
    }

    override suspend fun updateEntry(id: Long, draft: DoseEntryDraft): Result<Unit> {
        entries.value = entries.value.map { existing ->
            if (existing.id == id) {
                existing.copy(
                    dosageMg = draft.dosageMg,
                    takenAt = draft.takenAt,
                    updatedAt = draft.takenAt,
                )
            } else {
                existing
            }
        }.sortedByDescending { it.takenAt }
        return Result.success(Unit)
    }

    override suspend fun deleteEntry(id: Long): Result<Unit> {
        entries.value = entries.value.filterNot { it.id == id }
        return Result.success(Unit)
    }
}

private class FakeOverlayPreferenceStore : OverlayPreferenceStore {
    private val mutablePreferences = MutableStateFlow(OverlayPreferences())
    override val preferences: StateFlow<OverlayPreferences> = mutablePreferences.asStateFlow()

    override suspend fun setEnabled(enabled: Boolean) {
        mutablePreferences.value = mutablePreferences.value.copy(enabled = enabled)
    }

    override suspend fun setAlpha(alpha: Float) {
        mutablePreferences.value = mutablePreferences.value.copy(alpha = alpha)
    }
}

private class FakeOverlayController : OverlayController {
    private val mutableRuntime = MutableStateFlow(OverlayRuntimeState())
    override val runtimeState: StateFlow<OverlayRuntimeState> = mutableRuntime.asStateFlow()

    override fun permissionGranted(): Boolean = false

    override suspend fun startFromVisibleUi(): Result<Unit> = Result.success(Unit)

    override suspend fun stop(): Result<Unit> = Result.success(Unit)

    override suspend fun refresh(): Result<Unit> = Result.success(Unit)
}
