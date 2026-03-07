package com.redpillz.pixelutility.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.redpillz.pixelutility.AppContainer
import com.redpillz.pixelutility.overlay.OverlayStatusResolver
import com.redpillz.pixelutility.ui.components.DoseEditorCard
import com.redpillz.pixelutility.ui.components.DoseEntryList
import com.redpillz.pixelutility.ui.components.InfoMessage
import com.redpillz.pixelutility.ui.components.QuickDoseCard
import com.redpillz.pixelutility.ui.components.ScreenColumn
import com.redpillz.pixelutility.ui.components.SectionCard
import com.redpillz.pixelutility.ui.components.TotalsList
import com.redpillz.pixelutility.viewmodel.HomeViewModel
import com.redpillz.pixelutility.viewmodel.OverlayViewModel
import com.redpillz.pixelutility.viewmodel.PillLogViewModel
import com.redpillz.pixelutility.viewmodel.SettingsViewModel
import com.redpillz.pixelutility.viewmodel.ViewModelFactory

private enum class AppDestination(
    val route: String,
    val label: String,
) {
    Home("home", "Home"),
    PillLog("pill_log", "Pill Log"),
    Overlay("overlay", "Overlay"),
    Settings("settings", "Settings"),
}

@Composable
fun RedPillzRoot(appContainer: AppContainer) {
    val navController = rememberNavController()
    val items = AppDestination.entries

    Scaffold(
        bottomBar = {
            NavigationBar {
                val currentBackStack by navController.currentBackStackEntryAsState()
                val destination = currentBackStack?.destination
                items.forEach { item ->
                    NavigationBarItem(
                        selected = destination?.hierarchy?.any { it.route == item.route } == true,
                        onClick = {
                            navController.navigate(item.route) {
                                launchSingleTop = true
                                restoreState = true
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                }
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = when (item) {
                                    AppDestination.Home -> Icons.Outlined.Home
                                    AppDestination.PillLog -> Icons.Outlined.Medication
                                    AppDestination.Overlay -> Icons.Outlined.Visibility
                                    AppDestination.Settings -> Icons.Outlined.Settings
                                },
                                contentDescription = item.label,
                            )
                        },
                        label = { Text(item.label) },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = AppDestination.Home.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(AppDestination.Home.route) {
                val viewModel: HomeViewModel = viewModel(
                    factory = ViewModelFactory {
                        HomeViewModel(
                            medicationRepository = appContainer.medicationRepository,
                            doseEntryRepository = appContainer.doseEntryRepository,
                            validator = appContainer.doseEntryValidator,
                            calculator = appContainer.todayDoseSummaryCalculator,
                            overlayPreferencesRepository = appContainer.overlayPreferencesRepository,
                            overlayController = appContainer.overlayController,
                            overlayStatusResolver = OverlayStatusResolver(),
                            clock = appContainer.clock,
                        )
                    },
                )
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                LifecycleResumeEffect(viewModel::refreshOverlayPermission)
                ScreenColumn {
                    InfoMessage(state.infoMessage)
                    QuickDoseCard(
                        medications = state.activeMedications,
                        selectedMedicationId = state.quickLog.selectedMedicationId,
                        dosageInput = state.quickLog.dosageInput,
                        onMedicationSelected = viewModel::updateMedicationSelection,
                        onDosageChanged = viewModel::updateDosageInput,
                        onSave = viewModel::quickLogNow,
                        onOpenSettings = { navController.navigate(AppDestination.Settings.route) },
                    )
                    SectionCard(title = "Today's totals") {
                        TotalsList(
                            entries = state.todayTotals.map { it.medicationName to formatDoseMg(it.totalMg) },
                        )
                    }
                    SectionCard(title = "Today's recent entries") {
                        DoseEntryList(entries = state.todayEntries)
                    }
                    SectionCard(title = "Overlay status") {
                        Text(state.overlayHeadline)
                        Text(state.overlayDetail)
                        Text("Intended behavior: a true system-wide red overlay across other apps, not an in-app theme.")
                        Button(
                            onClick = { navController.navigate(AppDestination.Overlay.route) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Open overlay controls")
                        }
                    }
                }
            }

            composable(AppDestination.PillLog.route) {
                val viewModel: PillLogViewModel = viewModel(
                    factory = ViewModelFactory {
                        PillLogViewModel(
                            medicationRepository = appContainer.medicationRepository,
                            doseEntryRepository = appContainer.doseEntryRepository,
                            validator = appContainer.doseEntryValidator,
                            calculator = appContainer.todayDoseSummaryCalculator,
                            clock = appContainer.clock,
                        )
                    },
                )
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                ScreenColumn {
                    InfoMessage(state.infoMessage)
                    DoseEditorCard(
                        medications = state.medicationOptions,
                        selectedMedicationId = state.form.selectedMedicationId,
                        dosageInput = state.form.dosageInput,
                        notesInput = state.form.notesInput,
                        takenAt = state.form.takenAt ?: appContainer.clock.instant(),
                        isEditing = state.form.editingEntryId != null,
                        onMedicationSelected = viewModel::updateMedicationSelection,
                        onDosageChanged = viewModel::updateDosageInput,
                        onNotesChanged = viewModel::updateNotes,
                        onTakenAtChanged = viewModel::updateTakenAt,
                        onSave = viewModel::saveEntry,
                        onCancel = viewModel::cancelEditing,
                    )
                    SectionCard(title = "Today's log") {
                        DoseEntryList(
                            entries = state.todayEntries,
                            onEdit = viewModel::startEditing,
                            onDelete = { viewModel.deleteEntry(it.id) },
                        )
                    }
                }
            }

            composable(AppDestination.Overlay.route) {
                val context = LocalContext.current
                val viewModel: OverlayViewModel = viewModel(
                    factory = ViewModelFactory {
                        OverlayViewModel(
                            preferencesRepository = appContainer.overlayPreferencesRepository,
                            overlayController = appContainer.overlayController,
                            statusResolver = OverlayStatusResolver(),
                        )
                    },
                )
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                LifecycleResumeEffect(viewModel::refreshPermissionState)
                ScreenColumn {
                    InfoMessage(state.infoMessage)
                    SectionCard(title = "Overlay controls") {
                        Text("This feature targets a true system-wide red overlay across other apps.")
                        Text("It is not a red app theme, dark mode, wallpaper tint, or preview mockup.")
                        Text("Overlay service may only be started from a visible in-app screen in v1.")
                        Text("On Android 11 and newer, the permission intent typically opens the top-level Display over other apps settings flow instead of a guaranteed direct app detail page.")
                        Text("Single overlay alpha is kept at or below 0.8.")
                    }
                    SectionCard(title = "Overlay state") {
                        Text(state.statusHeadline)
                        Text(state.statusDetail)
                        Switch(
                            checked = state.enabled,
                            onCheckedChange = viewModel::setEnabled,
                        )
                        Text(if (state.enabled) "Overlay preference enabled" else "Overlay preference disabled")
                        Text("Intensity: ${(state.alpha * 100).toInt()}%")
                        Slider(
                            value = state.alpha,
                            onValueChange = viewModel::setAlpha,
                            valueRange = 0.1f..0.8f,
                        )
                        Button(
                            onClick = {
                                val intent = Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:${context.packageName}"),
                                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                context.startActivity(intent)
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Open overlay permission settings")
                        }
                        Button(
                            onClick = viewModel::startOverlay,
                            enabled = state.permissionGranted && state.enabled && !state.isRunning,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Start overlay now")
                        }
                        OutlinedButton(
                            onClick = viewModel::stopOverlay,
                            enabled = state.isRunning,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Stop overlay")
                        }
                    }
                }
            }

            composable(AppDestination.Settings.route) {
                val viewModel: SettingsViewModel = viewModel(
                    factory = ViewModelFactory {
                        SettingsViewModel(
                            medicationRepository = appContainer.medicationRepository,
                        )
                    },
                )
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                ScreenColumn {
                    InfoMessage(state.infoMessage)
                    SectionCard(title = "Add medication") {
                        OutlinedTextField(
                            value = state.newMedicationName,
                            onValueChange = viewModel::updateNewMedicationName,
                            label = { Text("Medication name") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Button(
                            onClick = viewModel::saveMedication,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Add medication")
                        }
                    }
                    if (state.renameMedicationId != null) {
                        SectionCard(title = "Rename medication") {
                            OutlinedTextField(
                                value = state.renameMedicationName,
                                onValueChange = viewModel::updateRenameMedicationName,
                                label = { Text("Medication name") },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Button(
                                onClick = viewModel::saveRename,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("Save name")
                            }
                            OutlinedButton(
                                onClick = viewModel::cancelRename,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("Cancel rename")
                            }
                        }
                    }
                    SectionCard(title = "Medications") {
                        if (state.medications.isEmpty()) {
                            Text("No medications added yet.")
                        } else {
                            state.medications.forEach { medication ->
                                SectionCard(
                                    title = medication.name + if (medication.isActive) "" else " (archived)",
                                ) {
                                    Button(
                                        onClick = { viewModel.beginRename(medication) },
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        Text("Rename")
                                    }
                                    OutlinedButton(
                                        onClick = { viewModel.toggleMedication(medication) },
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        Text(if (medication.isActive) "Archive" else "Reactivate")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
