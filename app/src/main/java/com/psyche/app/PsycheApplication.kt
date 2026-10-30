package com.psyche.app

import android.app.Application
import com.psyche.app.data.FinanceDatabase
import com.psyche.app.data.FinanceRepository
import com.psyche.app.notifications.BudgetNotifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class PsycheApplication : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val financeRepository: FinanceRepository by lazy {
        FinanceRepository(FinanceDatabase.build(this).financeDao())
    }

    val budgetNotifier: BudgetNotifier by lazy {
        BudgetNotifier(this, financeRepository)
    }

    override fun onCreate() {
        super.onCreate()
        budgetNotifier.start(appScope)
    }
}
