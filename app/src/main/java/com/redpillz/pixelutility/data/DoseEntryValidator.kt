package com.redpillz.pixelutility.data

import java.time.Instant

data class DoseValidationInput(
    val medicationId: Long?,
    val dosageText: String,
    val takenAt: Instant?,
)

sealed interface DoseValidationResult {
    data object Valid : DoseValidationResult
    data class Invalid(val message: String) : DoseValidationResult
}

class DoseEntryValidator {
    fun validate(input: DoseValidationInput): DoseValidationResult {
        if (input.medicationId == null) {
            return DoseValidationResult.Invalid("Choose a medication.")
        }

        val dosage = input.dosageText.toDoubleOrNull()
        if (dosage == null || !dosage.isFinite() || dosage <= 0.0) {
            return DoseValidationResult.Invalid("Enter a dosage greater than 0 mg.")
        }

        if (input.takenAt == null) {
            return DoseValidationResult.Invalid("Pick a valid time.")
        }

        return DoseValidationResult.Valid
    }
}

