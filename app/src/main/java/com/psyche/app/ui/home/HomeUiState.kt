package com.psyche.app.ui.home

import com.psyche.app.data.Entry

data class HomeUiState(
    val entries: List<Entry> = emptyList(),
    val isLoading: Boolean = true,
)
