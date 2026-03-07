package com.redpillz.pixelutility

import android.content.Context
import androidx.room.Room
import com.redpillz.pixelutility.data.DoseEntryValidator
import com.redpillz.pixelutility.data.TodayDoseSummaryCalculator
import com.redpillz.pixelutility.data.local.AppDatabase
import com.redpillz.pixelutility.data.repository.DoseEntryRepository
import com.redpillz.pixelutility.data.repository.MedicationRepository
import com.redpillz.pixelutility.data.repository.RoomDoseEntryRepository
import com.redpillz.pixelutility.data.repository.RoomMedicationRepository
import com.redpillz.pixelutility.overlay.DefaultOverlayController
import com.redpillz.pixelutility.overlay.OverlayController
import com.redpillz.pixelutility.overlay.OverlayPreferencesRepository
import com.redpillz.pixelutility.overlay.OverlayRuntimeStore
import java.time.Clock
import java.time.ZoneId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val clock: Clock = Clock.systemDefaultZone()
    val zoneId: ZoneId = ZoneId.systemDefault()

    private val database: AppDatabase = Room.databaseBuilder(
        appContext,
        AppDatabase::class.java,
        "redpillz.db",
    ).fallbackToDestructiveMigration().build()

    val medicationRepository: MedicationRepository = RoomMedicationRepository(
        medicationDao = database.medicationDao(),
        clock = clock,
    )

    val doseEntryRepository: DoseEntryRepository = RoomDoseEntryRepository(
        doseEntryDao = database.doseEntryDao(),
        clock = clock,
    )

    val doseEntryValidator = DoseEntryValidator()
    val todayDoseSummaryCalculator = TodayDoseSummaryCalculator(clock, zoneId)
    val overlayRuntimeStore = OverlayRuntimeStore()
    val overlayPreferencesRepository = OverlayPreferencesRepository(
        context = appContext,
        scope = applicationScope,
    )
    val overlayController: OverlayController = DefaultOverlayController(
        context = appContext,
        preferencesRepository = overlayPreferencesRepository,
        runtimeStore = overlayRuntimeStore,
    )
}

