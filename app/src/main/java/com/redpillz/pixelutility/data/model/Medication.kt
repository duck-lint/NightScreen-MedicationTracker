package com.redpillz.pixelutility.data.model

import java.time.Instant

data class Medication(
    val id: Long,
    val name: String,
    val isActive: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
)

