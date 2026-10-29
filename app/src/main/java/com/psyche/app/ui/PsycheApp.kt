package com.psyche.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.psyche.app.ui.home.HomeScreen
import com.psyche.app.ui.home.HomeViewModel

@Composable
fun PsycheApp(viewModel: HomeViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    HomeScreen(
        uiState = uiState,
        onAddEntry = viewModel::addEntry,
    )
}
