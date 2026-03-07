package com.redpillz.pixelutility.data.repository

import com.redpillz.pixelutility.data.local.DoseEntryDao
import com.redpillz.pixelutility.data.local.DoseEntryEntity
import com.redpillz.pixelutility.data.local.DoseEntryWithMedicationRow
import com.redpillz.pixelutility.data.model.DoseEntryDraft
import com.redpillz.pixelutility.data.model.DoseEntryRecord
import java.time.Clock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomDoseEntryRepository(
    private val doseEntryDao: DoseEntryDao,
    private val clock: Clock,
) : DoseEntryRepository {
    override fun observeAllEntries(): Flow<List<DoseEntryRecord>> {
        return doseEntryDao.observeAllWithMedication().map { rows -> rows.map { it.toModel() } }
    }

    override suspend fun createEntry(draft: DoseEntryDraft): Result<Unit> {
        val now = clock.instant()
        doseEntryDao.insert(
            DoseEntryEntity(
                medicationId = draft.medicationId,
                dosageMg = draft.dosageMg,
                takenAt = draft.takenAt,
                notes = draft.notes?.trim()?.takeIf { it.isNotEmpty() },
                createdAt = now,
                updatedAt = now,
            ),
        )
        return Result.success(Unit)
    }

    override suspend fun updateEntry(id: Long, draft: DoseEntryDraft): Result<Unit> {
        val existing = doseEntryDao.getById(id)
            ?: return Result.failure(IllegalArgumentException("Dose entry no longer exists."))
        doseEntryDao.update(
            existing.copy(
                medicationId = draft.medicationId,
                dosageMg = draft.dosageMg,
                takenAt = draft.takenAt,
                notes = draft.notes?.trim()?.takeIf { it.isNotEmpty() },
                updatedAt = clock.instant(),
            ),
        )
        return Result.success(Unit)
    }

    override suspend fun deleteEntry(id: Long): Result<Unit> {
        doseEntryDao.deleteById(id)
        return Result.success(Unit)
    }

    private fun DoseEntryWithMedicationRow.toModel(): DoseEntryRecord {
        return DoseEntryRecord(
            id = doseEntryId,
            medicationId = medicationId,
            medicationName = medicationName,
            medicationIsActive = medicationIsActive,
            dosageMg = dosageMg,
            takenAt = takenAt,
            notes = notes,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )
    }
}
