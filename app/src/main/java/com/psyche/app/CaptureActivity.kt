package com.psyche.app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.psyche.app.data.ThemeMode
import com.psyche.app.ui.FinanceViewModel
import com.psyche.app.ui.capture.CaptureContent
import com.psyche.app.ui.theme.LocalPsychePalette
import com.psyche.app.ui.theme.PsycheTheme

class CaptureActivity : ComponentActivity() {

    private val viewModel: FinanceViewModel by viewModels {
        FinanceViewModel.factory((application as PsycheApplication).financeRepository)
    }

    companion object {
        const val ACTION_CAPTURE = "com.psyche.app.action.CAPTURE"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setFinishOnTouchOutside(true)
        setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            val systemDark = isSystemInDarkTheme()
            val darkTheme = when (state.settings.themeMode) {
                ThemeMode.SYSTEM -> systemDark
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            PsycheTheme(darkTheme = darkTheme) {
                val palette = LocalPsychePalette.current
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(ComposeColor(0x99000000))
                        .pointerInput(Unit) {
                            detectTapGestures { finish() }
                        },
                ) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .widthIn(max = 560.dp)
                            .fillMaxWidth()
                            .pointerInput(Unit) {
                                detectTapGestures { }
                            },
                        shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
                        color = palette.ground,
                        contentColor = palette.onGround,
                    ) {
                        CaptureContent(
                            viewModel = viewModel,
                            state = state,
                            fromNotification = true,
                            onDismiss = { finish() },
                            modifier = Modifier.navigationBarsPadding(),
                        )
                    }
                }
            }
        }
    }
}
