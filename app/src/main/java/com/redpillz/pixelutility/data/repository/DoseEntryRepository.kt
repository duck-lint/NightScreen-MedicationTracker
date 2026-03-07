package com.redpillz.pixelutility.data.repository

import com.redpillz.pixelutility.data.model.DoseEntryDraft
import com.redpillz.pixelutility.data.model.DoseEntryRecord
import kotlinx.coroutines.flow.Flow

interface DoseEntryRepository {
    fun observeAllEntries(): Flow<List<DoseEntryRecord>>
    suspend fun createEntry(draft: DoseEntryDraft): Result<Unit>
    suspend fun updateEntry(id: Long, draft: DoseEntryDraft): Result<Unit>
    suspend fun deleteEntry(id: Long): Result<Unit>
}

