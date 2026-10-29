package com.psyche.app.data

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test

class InMemoryEntryRepositoryTest {

    @Test
    fun `addEntry emits entries in insertion order`() = runTest {
        val repository = InMemoryEntryRepository(now = { 42L })

        repository.observeEntries().test {
            assertThat(awaitItem()).isEmpty()

            repository.addEntry("first")
            assertThat(awaitItem().single().text).isEqualTo("first")

            repository.addEntry("second")
            val entries = awaitItem()

            assertThat(entries.map { it.text })
                .containsExactly("first", "second")
                .inOrder()
            assertThat(entries.map { it.id })
                .containsExactly(1L, 2L)
                .inOrder()

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `addEntry trims text and ignores blank input`() = runTest {
        val repository = InMemoryEntryRepository(now = { 42L })

        repository.observeEntries().test {
            assertThat(awaitItem()).isEmpty()

            repository.addEntry("  hello  ")
            assertThat(awaitItem().single().text).isEqualTo("hello")

            repository.addEntry("   ")
            expectNoEvents()

            cancelAndIgnoreRemainingEvents()
        }
    }
}
