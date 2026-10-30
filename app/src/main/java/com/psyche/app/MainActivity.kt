package com.psyche.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.psyche.app.data.ThemeMode
import com.psyche.app.ui.FinanceViewModel
import com.psyche.app.ui.PsycheApp
import com.psyche.app.ui.theme.PsycheTheme

class MainActivity : ComponentActivity() {

    private val viewModel: FinanceViewModel by viewModels {
        FinanceViewModel.factory((application as PsycheApplication).financeRepository)
    }

    private val notificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        viewModel.onNotificationPermissionResult(granted)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            val systemDark = isSystemInDarkTheme()
            val darkTheme = when (state.settings.themeMode) {
                ThemeMode.SYSTEM -> systemDark
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            PsycheTheme(darkTheme = darkTheme) {
                PsycheApp(
                    viewModel = viewModel,
                    onRequestNotificationPermission = ::requestNotifications,
                    onOpenNotificationSettings = ::openNotificationSettings,
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        (application as PsycheApplication).budgetNotifier.refresh()
    }

    private fun openNotificationSettings() {
        startActivity(
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, packageName),
        )
    }

    private fun requestNotifications() {
        val permissionGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        val systemEnabled = NotificationManagerCompat.from(this).areNotificationsEnabled()

        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !permissionGranted -> {
                viewModel.markNotificationPromptSeen()
                notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }

            !systemEnabled -> {
                viewModel.markNotificationPromptSeen()
                // Remember the intent while Android's app-level switch is off. The
                // settings warning stays visible until the system allows it.
                viewModel.setNotificationsEnabled(true)
                openNotificationSettings()
            }

            else -> viewModel.onNotificationPermissionResult(true)
        }
    }
}
