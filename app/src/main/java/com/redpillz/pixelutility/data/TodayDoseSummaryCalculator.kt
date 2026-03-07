package com.redpillz.pixelutility.data

import com.redpillz.pixelutility.data.model.DoseEntryRecord
import com.redpillz.pixelutility.data.model.TodayMedicationTotal
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

class TodayDoseSummaryCalculator(
    private val clock: Clock,
    private val zoneId: ZoneId,
) {
    fun todayEntries(entries: List<DoseEntryRecord>): List<DoseEntryRecord> {
        val (start, end) = todayBounds()
        return entries.filter { it.takenAt >= start && it.takenAt < end }
    }

    fun todayTotals(entries: List<DoseEntryRecord>): List<TodayMedicationTotal> {
        return todayEntries(entries)
            .groupBy { it.medicationId }
            .map { (_, records) ->
                val first = records.first()
                TodayMedicationTotal(
                    medicationId = first.medicationId,
                    medicationName = first.medicationName,
                    totalMg = records.sumOf { it.dosageMg },
                )
            }
            .sortedBy { it.medicationName.lowercase() }
    }

    private fun todayBounds(): Pair<Instant, Instant> {
        val today = clock.instant().atZone(zoneId).toLocalDate()
        val start = today.atStartOfDay(zoneId).toInstant()
        val end = today.plusDays(1).atStartOfDay(zoneId).toInstant()
        return start to end
    }
}

