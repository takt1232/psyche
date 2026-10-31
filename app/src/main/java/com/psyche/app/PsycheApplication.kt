package com.psyche.app

import android.app.Application
import android.app.NotificationManager
import android.os.Build
import com.psyche.app.data.FinanceDatabase
import com.psyche.app.data.FinanceRepository
import com.psyche.app.widget.QuickWidgetRenderer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.launch

class PsycheApplication : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val financeRepository: FinanceRepository by lazy {
        FinanceRepository(FinanceDatabase.build(this).financeDao())
    }

    override fun onCreate() {
        super.onCreate()
        removeLegacyNotificationChannel()
        // Keep the home-screen widget in step with the ledger while the process
        // is alive. The widget receiver also refreshes immediately after a tap.
        appScope.launch {
            financeRepository.snapshot
                .conflate()
                .collect {
                    delay(200)
                    runCatching {
                        QuickWidgetRenderer.updateAll(this@PsycheApplication)
                    }
                }
        }
    }

    /** Removes the channel left behind by the retired ongoing notification. */
    private fun removeLegacyNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getSystemService(NotificationManager::class.java)
                ?.deleteNotificationChannel("budget_pass")
        }
    }
}
