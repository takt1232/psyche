package com.psyche.app.ui.home

import com.google.common.truth.Truth.assertThat
import com.psyche.app.data.InMemoryEntryRepository
import com.psyche.app.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = InMemoryEntryRepository(now = { 1_000L })
    private lateinit var viewModel: HomeViewModel

    @Before
    fun setUp() {
        viewModel = HomeViewModel(repository)
    }

    @Test
    fun `initial state is loading`() {
        assertThat(viewModel.uiState.value).isEqualTo(HomeUiState())
    }

    @Test
    fun `emits empty content once observed`() = runTest {
        viewModel.uiState.launchIn(backgroundScope)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.isLoading).isFalse()
        assertThat(state.entries).isEmpty()
    }

    @Test
    fun `addEntry appends a trimmed entry`() = runTest {
        viewModel.uiState.launchIn(backgroundScope)
        advanceUntilIdle()

        viewModel.addEntry("  first thought  ")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.entries).hasSize(1)
        assertThat(state.entries.single().text).isEqualTo("first thought")
        assertThat(state.entries.single().createdAt).isEqualTo(1_000L)
    }

    @Test
    fun `addEntry ignores blank text`() = runTest {
        viewModel.uiState.launchIn(backgroundScope)
        advanceUntilIdle()

        viewModel.addEntry("   ")
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.entries).isEmpty()
    }
}
