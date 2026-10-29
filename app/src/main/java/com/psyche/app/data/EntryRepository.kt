package com.psyche.app.data

import kotlinx.coroutines.flow.Flow

interface EntryRepository {
    fun observeEntries(): Flow<List<Entry>>

    suspend fun addEntry(text: String)
}
