package com.psyche.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.psyche.app.ui.PsycheApp
import com.psyche.app.ui.home.HomeViewModel
import com.psyche.app.ui.theme.PsycheTheme

class MainActivity : ComponentActivity() {

    private val viewModel: HomeViewModel by viewModels {
        HomeViewModel.factory((application as PsycheApplication).entryRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PsycheTheme {
                PsycheApp(viewModel = viewModel)
            }
        }
    }
}
