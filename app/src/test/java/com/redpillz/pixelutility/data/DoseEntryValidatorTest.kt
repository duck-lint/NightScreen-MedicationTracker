package com.redpillz.pixelutility.data

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DoseEntryValidatorTest {
    private val validator = DoseEntryValidator()
    private val timestamp = Instant.parse("2026-03-07T12:00:00Z")

    @Test
    fun rejectsMissingMedication() {
        val result = validator.validate(
            DoseValidationInput(
                medicationId = null,
                dosageText = "10",
                takenAt = timestamp,
            ),
        )

        assertEquals("Choose a medication.", (result as DoseValidationResult.Invalid).message)
    }

    @Test
    fun rejectsInvalidDosageValues() {
        val invalidValues = listOf("", "0", "-2", "NaN", "Infinity")

        invalidValues.forEach { value ->
            val result = validator.validate(
                DoseValidationInput(
                    medicationId = 1L,
                    dosageText = value,
                    takenAt = timestamp,
                ),
            )

            assertEquals("Enter a dosage greater than 0 mg.", (result as DoseValidationResult.Invalid).message)
        }
    }

    @Test
    fun acceptsValidPositiveDecimal() {
        val result = validator.validate(
            DoseValidationInput(
                medicationId = 1L,
                dosageText = "12.5",
                takenAt = timestamp,
            ),
        )

        assertTrue(result is DoseValidationResult.Valid)
    }
}

