package com.redpillz.pixelutility.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DoseEntryDao {
    @Query(
        """
        SELECT
            dose_entries.id AS dose_entry_id,
            medications.id AS medication_id,
            medications.name AS medication_name,
            medications.isActive AS medication_is_active,
            dose_entries.dosageMg AS dosage_mg,
            dose_entries.takenAt AS taken_at,
            dose_entries.notes AS notes,
            dose_entries.createdAt AS created_at,
            dose_entries.updatedAt AS updated_at
        FROM dose_entries
        INNER JOIN medications ON medications.id = dose_entries.medicationId
        ORDER BY dose_entries.takenAt DESC, dose_entries.id DESC
        """,
    )
    fun observeAllWithMedication(): Flow<List<DoseEntryWithMedicationRow>>

    @Query("SELECT * FROM dose_entries WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): DoseEntryEntity?

    @Insert
    suspend fun insert(entity: DoseEntryEntity): Long

    @Update
    suspend fun update(entity: DoseEntryEntity)

    @Query("DELETE FROM dose_entries WHERE id = :id")
    suspend fun deleteById(id: Long)
}

