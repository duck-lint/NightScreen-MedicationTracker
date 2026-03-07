package com.redpillz.pixelutility.data.model

import java.time.Instant

data class DoseEntryRecord(
    val id: Long,
    val medicationId: Long,
    val medicationName: String,
    val medicationIsActive: Boolean,
    val dosageMg: Double,
    val takenAt: Instant,
    val notes: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
)

