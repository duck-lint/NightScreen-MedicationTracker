package com.redpillz.pixelutility.data.model

import java.time.Instant

data class DoseEntryDraft(
    val medicationId: Long,
    val dosageMg: Double,
    val takenAt: Instant,
    val notes: String?,
)

