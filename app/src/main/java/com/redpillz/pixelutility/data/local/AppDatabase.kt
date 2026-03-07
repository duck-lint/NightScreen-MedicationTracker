package com.redpillz.pixelutility.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [MedicationEntity::class, DoseEntryEntity::class],
    version = 1,
    exportSchema = false,
)
@TypeConverters(InstantConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun medicationDao(): MedicationDao
    abstract fun doseEntryDao(): DoseEntryDao
}

