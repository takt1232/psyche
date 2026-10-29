package com.psyche.app

import android.app.Application
import com.psyche.app.data.EntryRepository
import com.psyche.app.data.InMemoryEntryRepository

class PsycheApplication : Application() {
    val entryRepository: EntryRepository by lazy { InMemoryEntryRepository() }
}
