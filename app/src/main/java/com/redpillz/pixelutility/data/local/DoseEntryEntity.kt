package com.redpillz.pixelutility.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(
    tableName = "dose_entries",
    foreignKeys = [
        ForeignKey(
            entity = MedicationEntity::class,
            parentColumns = ["id"],
            childColumns = ["medicationId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("medicationId"), Index("takenAt")],
)
data class DoseEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val medicationId: Long,
    val dosageMg: Double,
    val takenAt: Instant,
    val notes: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
)

