package com.redpillz.pixelutility.data

import com.redpillz.pixelutility.data.model.DoseEntryRecord
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Test

class TodayDoseSummaryCalculatorTest {
    @Test
    fun groupsTotalsByMedicationForToday() {
        val clock = Clock.fixed(Instant.parse("2026-03-07T18:00:00Z"), ZoneOffset.UTC)
        val calculator = TodayDoseSummaryCalculator(clock, ZoneOffset.UTC)

        val totals = calculator.todayTotals(
            listOf(
                record(id = 1, medicationId = 1, medicationName = "Magnesium", dosage = 100.0, takenAt = "2026-03-07T09:00:00Z"),
                record(id = 2, medicationId = 1, medicationName = "Magnesium", dosage = 50.0, takenAt = "2026-03-07T12:00:00Z"),
                record(id = 3, medicationId = 2, medicationName = "Melatonin", dosage = 5.0, takenAt = "2026-03-07T22:00:00Z"),
                record(id = 4, medicationId = 2, medicationName = "Melatonin", dosage = 1.0, takenAt = "2026-03-06T23:59:00Z"),
            ),
        )

        assertEquals(2, totals.size)
        assertEquals(150.0, totals.first { it.medicationName == "Magnesium" }.totalMg, 0.0)
        assertEquals(5.0, totals.first { it.medicationName == "Melatonin" }.totalMg, 0.0)
    }

    @Test
    fun respectsLocalMidnightBoundaries() {
        val zoneId = ZoneId.of("America/Edmonton")
        val clock = Clock.fixed(Instant.parse("2026-03-07T08:00:00Z"), ZoneOffset.UTC)
        val calculator = TodayDoseSummaryCalculator(clock, zoneId)

        val todayEntries = calculator.todayEntries(
            listOf(
                record(id = 1, medicationId = 1, medicationName = "A", dosage = 1.0, takenAt = "2026-03-07T06:59:00Z"),
                record(id = 2, medicationId = 1, medicationName = "A", dosage = 2.0, takenAt = "2026-03-07T07:00:00Z"),
            ),
        )

        assertEquals(listOf(2L), todayEntries.map { it.id })
    }

    private fun record(
        id: Long,
        medicationId: Long,
        medicationName: String,
        dosage: Double,
        takenAt: String,
    ): DoseEntryRecord {
        val instant = Instant.parse(takenAt)
        return DoseEntryRecord(
            id = id,
            medicationId = medicationId,
            medicationName = medicationName,
            medicationIsActive = true,
            dosageMg = dosage,
            takenAt = instant,
            notes = null,
            createdAt = instant,
            updatedAt = instant,
        )
    }
}

