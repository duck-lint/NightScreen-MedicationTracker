package com.redpillz.pixelutility.ui

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val timestampFormatter = DateTimeFormatter.ofPattern("MMM d, h:mm a")

fun formatInstant(instant: Instant, zoneId: ZoneId = ZoneId.systemDefault()): String {
    return instant.atZone(zoneId).format(timestampFormatter)
}

fun formatDoseMg(value: Double): String {
    return if (value % 1.0 == 0.0) {
        "${value.toInt()} mg"
    } else {
        String.format(Locale.US, "%.1f mg", value)
    }
}

