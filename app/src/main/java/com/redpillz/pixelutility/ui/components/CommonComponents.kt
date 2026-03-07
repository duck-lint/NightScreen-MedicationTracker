package com.redpillz.pixelutility.ui.components

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.redpillz.pixelutility.data.model.DoseEntryRecord
import com.redpillz.pixelutility.data.model.Medication
import com.redpillz.pixelutility.ui.formatDoseMg
import com.redpillz.pixelutility.ui.formatInstant
import java.time.Instant
import java.time.ZoneId

@Composable
fun SectionCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
fun InfoMessage(message: String?) {
    if (message != null) {
        AssistChip(
            onClick = {},
            label = { Text(message) },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicationSelector(
    medications: List<Medication>,
    selectedMedicationId: Long?,
    onMedicationSelected: (Long) -> Unit,
    label: String,
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = medications.firstOrNull { it.id == selectedMedicationId }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded && medications.isNotEmpty() },
    ) {
        OutlinedTextField(
            value = selected?.let { if (it.isActive) it.name else "${it.name} (archived)" } ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            enabled = medications.isNotEmpty(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            medications.forEach { medication ->
                DropdownMenuItem(
                    text = {
                        Text(
                            if (medication.isActive) medication.name else "${medication.name} (archived)",
                        )
                    },
                    onClick = {
                        onMedicationSelected(medication.id)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
fun QuickDoseCard(
    medications: List<Medication>,
    selectedMedicationId: Long?,
    dosageInput: String,
    onMedicationSelected: (Long) -> Unit,
    onDosageChanged: (String) -> Unit,
    onSave: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    SectionCard(title = "Quick log dose") {
        if (medications.isEmpty()) {
            Text("Add a medication in Settings before you log your first dose.")
            Button(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth()) {
                Text("Open settings")
            }
        } else {
            MedicationSelector(
                medications = medications,
                selectedMedicationId = selectedMedicationId,
                onMedicationSelected = onMedicationSelected,
                label = "Medication",
            )
            OutlinedTextField(
                value = dosageInput,
                onValueChange = onDosageChanged,
                label = { Text("Dosage (mg)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
            Button(onClick = onSave, modifier = Modifier.fillMaxWidth()) {
                Text("Log now")
            }
        }
    }
}

@Composable
fun DoseEditorCard(
    medications: List<Medication>,
    selectedMedicationId: Long?,
    dosageInput: String,
    notesInput: String,
    takenAt: Instant,
    isEditing: Boolean,
    onMedicationSelected: (Long) -> Unit,
    onDosageChanged: (String) -> Unit,
    onNotesChanged: (String) -> Unit,
    onTakenAtChanged: (Instant) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    SectionCard(title = if (isEditing) "Edit dose entry" else "Dose entry") {
        if (medications.isEmpty()) {
            Text("Add a medication in Settings before creating entries.")
        } else {
            MedicationSelector(
                medications = medications,
                selectedMedicationId = selectedMedicationId,
                onMedicationSelected = onMedicationSelected,
                label = "Medication",
            )
        }
        OutlinedTextField(
            value = dosageInput,
            onValueChange = onDosageChanged,
            label = { Text("Dosage (mg)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = notesInput,
            onValueChange = onNotesChanged,
            label = { Text("Notes (optional)") },
            modifier = Modifier.fillMaxWidth(),
        )
        TimestampEditor(
            instant = takenAt,
            onInstantChanged = onTakenAtChanged,
        )
        Button(onClick = onSave, modifier = Modifier.fillMaxWidth()) {
            Text(if (isEditing) "Save changes" else "Save dose")
        }
        if (isEditing) {
            OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
                Text("Cancel editing")
            }
        }
    }
}

@Composable
fun TimestampEditor(
    instant: Instant,
    onInstantChanged: (Instant) -> Unit,
) {
    val context = LocalContext.current
    val zoneId = remember { ZoneId.systemDefault() }
    val zonedDateTime = instant.atZone(zoneId)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Taken at: ${formatInstant(instant, zoneId)}")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = {
                    DatePickerDialog(
                        context,
                        { _, year, month, dayOfMonth ->
                            val updated = zonedDateTime
                                .withYear(year)
                                .withMonth(month + 1)
                                .withDayOfMonth(dayOfMonth)
                            onInstantChanged(updated.toInstant())
                        },
                        zonedDateTime.year,
                        zonedDateTime.monthValue - 1,
                        zonedDateTime.dayOfMonth,
                    ).show()
                },
                modifier = Modifier.weight(1f),
            ) {
                Text("Change date")
            }
            OutlinedButton(
                onClick = {
                    TimePickerDialog(
                        context,
                        { _, hourOfDay, minute ->
                            val updated = zonedDateTime
                                .withHour(hourOfDay)
                                .withMinute(minute)
                            onInstantChanged(updated.toInstant())
                        },
                        zonedDateTime.hour,
                        zonedDateTime.minute,
                        false,
                    ).show()
                },
                modifier = Modifier.weight(1f),
            ) {
                Text("Change time")
            }
        }
        TextButton(onClick = { onInstantChanged(Instant.now()) }) {
            Text("Use current time")
        }
    }
}

@Composable
fun TotalsList(entries: List<Pair<String, String>>) {
    if (entries.isEmpty()) {
        Text("No doses logged today yet.")
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            entries.forEach { (name, total) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(name)
                    Text(total)
                }
            }
        }
    }
}

@Composable
fun DoseEntryList(
    entries: List<DoseEntryRecord>,
    onEdit: ((DoseEntryRecord) -> Unit)? = null,
    onDelete: ((DoseEntryRecord) -> Unit)? = null,
) {
    if (entries.isEmpty()) {
        Text("No entries for today yet.")
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            entries.forEach { entry ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = "${entry.medicationName} • ${formatDoseMg(entry.dosageMg)}",
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Text(formatInstant(entry.takenAt))
                        if (!entry.notes.isNullOrBlank()) {
                            Text(entry.notes)
                        }
                        if (onEdit != null && onDelete != null) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(onClick = { onEdit(entry) }) {
                                    Text("Edit")
                                }
                                TextButton(onClick = { onDelete(entry) }) {
                                    Text("Delete")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ScreenColumn(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        content = { content() },
    )
}
