package com.redpillz.pixelutility.data.repository

import com.redpillz.pixelutility.data.local.MedicationDao
import com.redpillz.pixelutility.data.local.MedicationEntity
import com.redpillz.pixelutility.data.model.Medication
import java.time.Clock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomMedicationRepository(
    private val medicationDao: MedicationDao,
    private val clock: Clock,
) : MedicationRepository {
    override fun observeAll(): Flow<List<Medication>> {
        return medicationDao.observeAll().map { entities -> entities.map { it.toModel() } }
    }

    override fun observeActive(): Flow<List<Medication>> {
        return medicationDao.observeActive().map { entities -> entities.map { it.toModel() } }
    }

    override suspend fun createMedication(name: String): Result<Unit> {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) {
            return Result.failure(IllegalArgumentException("Medication name cannot be blank."))
        }

        val now = clock.instant()
        medicationDao.insert(
            MedicationEntity(
                name = trimmedName,
                isActive = true,
                createdAt = now,
                updatedAt = now,
            ),
        )
        return Result.success(Unit)
    }

    override suspend fun renameMedication(id: Long, name: String): Result<Unit> {
        val existing = medicationDao.getById(id)
            ?: return Result.failure(IllegalArgumentException("Medication no longer exists."))
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) {
            return Result.failure(IllegalArgumentException("Medication name cannot be blank."))
        }

        medicationDao.update(
            existing.copy(
                name = trimmedName,
                updatedAt = clock.instant(),
            ),
        )
        return Result.success(Unit)
    }

    override suspend fun setMedicationActive(id: Long, isActive: Boolean): Result<Unit> {
        val existing = medicationDao.getById(id)
            ?: return Result.failure(IllegalArgumentException("Medication no longer exists."))
        medicationDao.update(
            existing.copy(
                isActive = isActive,
                updatedAt = clock.instant(),
            ),
        )
        return Result.success(Unit)
    }

    private fun MedicationEntity.toModel(): Medication {
        return Medication(
            id = id,
            name = name,
            isActive = isActive,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )
    }
}
