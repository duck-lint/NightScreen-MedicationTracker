package com.redpillz.pixelutility.data.local

import androidx.room.ColumnInfo
import java.time.Instant

data class DoseEntryWithMedicationRow(
    @ColumnInfo(name = "dose_entry_id") val doseEntryId: Long,
    @ColumnInfo(name = "medication_id") val medicationId: Long,
    @ColumnInfo(name = "medication_name") val medicationName: String,
    @ColumnInfo(name = "medication_is_active") val medicationIsActive: Boolean,
    @ColumnInfo(name = "dosage_mg") val dosageMg: Double,
    @ColumnInfo(name = "taken_at") val takenAt: Instant,
    @ColumnInfo(name = "notes") val notes: String?,
    @ColumnInfo(name = "created_at") val createdAt: Instant,
    @ColumnInfo(name = "updated_at") val updatedAt: Instant,
)

