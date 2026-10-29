package com.psyche.app.data

import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * In-memory implementation for the first iteration.
 * Swap this for a Room-backed repository later; the ViewModel only sees [EntryRepository].
 */
class InMemoryEntryRepository(
    private val now: () -> Long = System::currentTimeMillis,
) : EntryRepository {

    private val entries = MutableStateFlow<List<Entry>>(emptyList())
    private val nextId = AtomicLong(1L)

    override fun observeEntries(): Flow<List<Entry>> = entries.asStateFlow()

    override suspend fun addEntry(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return

        entries.update { current ->
            current + Entry(
                id = nextId.getAndIncrement(),
                text = trimmed,
                createdAt = now(),
            )
        }
    }
}
