package com.redpillz.pixelutility.data.repository

import com.redpillz.pixelutility.data.model.Medication
import kotlinx.coroutines.flow.Flow

interface MedicationRepository {
    fun observeAll(): Flow<List<Medication>>
    fun observeActive(): Flow<List<Medication>>
    suspend fun createMedication(name: String): Result<Unit>
    suspend fun renameMedication(id: Long, name: String): Result<Unit>
    suspend fun setMedicationActive(id: Long, isActive: Boolean): Result<Unit>
}

