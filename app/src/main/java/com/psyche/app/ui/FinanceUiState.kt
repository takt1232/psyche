package com.psyche.app.ui

import com.psyche.app.data.Category
import com.psyche.app.data.FinanceSettings
import com.psyche.app.data.FinanceSnapshot
import com.psyche.app.data.LedgerEntry
import com.psyche.app.data.QuickTransaction
import com.psyche.app.util.AccountBalance
import com.psyche.app.util.ActivityRow
import com.psyche.app.util.BudgetMath
import com.psyche.app.util.CategoryLine
import com.psyche.app.util.GoalProgress
import com.psyche.app.util.LedgerMath
import com.psyche.app.util.MonthSummary
import java.time.LocalDate
import java.time.YearMonth

data class MonthView(
    val month: YearMonth,
    val summary: MonthSummary,
    val rows: List<ActivityRow>,
    val categories: List<CategoryLine>,
    val allocations: Map<Long, Long>,
    val uncategorizedSpentCents: Long,
    val previousCapCents: Long?,
) {
    val capCents: Long? get() = summary.capCents
    val hasCategories: Boolean get() = categories.isNotEmpty()
    val allocatedCents: Long get() = allocations.values.sum()
    val unallocatedCents: Long? get() = capCents?.minus(allocatedCents)
    val litRow: ActivityRow? get() = rows.firstOrNull { it.entry.needsAttention }
}

data class FinanceUiState(
    val isLoading: Boolean = true,
    val snapshot: FinanceSnapshot = FinanceSnapshot(),
    val today: LocalDate = LocalDate.now(),
    val homeMonth: YearMonth = YearMonth.now(),
    val home: MonthView? = null,
    val activityMonth: YearMonth = YearMonth.now(),
    val activity: MonthView? = null,
    val budgetMonth: YearMonth = YearMonth.now(),
    val budget: MonthView? = null,
    val accountBalances: List<AccountBalance> = emptyList(),
    val goals: List<GoalProgress> = emptyList(),
    val cashTotalCents: Long = 0,
    val savingsTotalCents: Long = 0,
) {
    val settings: FinanceSettings get() = snapshot.settings
    val needsSetup: Boolean
        get() = home != null && (home.capCents == null || snapshot.activeAccounts.isEmpty())
    val activeCategories: List<Category> get() = snapshot.activeCategories
    val quickTransactions: List<QuickTransaction> get() = snapshot.activeQuickTransactions
}

object FinanceStateBuilder {
    fun build(
        snapshot: FinanceSnapshot,
        today: LocalDate,
        activityMonth: YearMonth = YearMonth.from(today),
        budgetMonth: YearMonth = YearMonth.from(today),
    ): FinanceUiState {
        val homeMonth = YearMonth.from(today)
        val balances = LedgerMath.accountBalances(snapshot.accounts, snapshot.entries)
            .filterNot { it.account.archived }
        val goals = LedgerMath.goalProgress(snapshot.activeGoals, snapshot.entries)
        return FinanceUiState(
            isLoading = false,
            snapshot = snapshot,
            today = today,
            homeMonth = homeMonth,
            home = monthView(snapshot, homeMonth, today),
            activityMonth = activityMonth,
            activity = monthView(snapshot, activityMonth, today),
            budgetMonth = budgetMonth,
            budget = monthView(snapshot, budgetMonth, today),
            accountBalances = balances,
            goals = goals,
            cashTotalCents = LedgerMath.cashTotal(balances),
            savingsTotalCents = LedgerMath.savingsTotal(goals),
        )
    }

    private fun monthView(
        snapshot: FinanceSnapshot,
        month: YearMonth,
        today: LocalDate,
    ): MonthView {
        val cap = snapshot.budgets[month]?.capCents
        val allocations = snapshot.allocations[month].orEmpty()
        val summary = BudgetMath.summarize(month, cap, snapshot.entries, today)
        val rows = LedgerMath.activityRows(
            entries = snapshot.entries,
            month = month,
            capCents = cap,
            accounts = snapshot.accounts,
            categories = snapshot.categories,
            goals = snapshot.goals,
        )
        return MonthView(
            month = month,
            summary = summary,
            rows = rows,
            categories = LedgerMath.categoryLines(
                categories = snapshot.categories.filter { category ->
                    !category.archived || allocations.containsKey(category.id)
                },
                entries = snapshot.entries,
                month = month,
                allocations = allocations,
            ),
            allocations = allocations,
            uncategorizedSpentCents = LedgerMath.unallocatedSpending(snapshot.entries, month),
            previousCapCents = snapshot.budgets[month.minusMonths(1)]?.capCents,
        )
    }
}

data class CaptureDraft(
    val type: CaptureType,
    val amountCents: Long,
    val accountId: Long,
    val categoryId: Long?,
    val note: String,
    val occurredAt: Long,
)

enum class CaptureType { EXPENSE, INCOME }

sealed interface SnackbarFollowUp {
    data class UndoCreate(val entryId: Long) : SnackbarFollowUp
    data object UndoDelete : SnackbarFollowUp
    data class OpenEntry(val entry: LedgerEntry) : SnackbarFollowUp
}

data class UiEvent(
    val message: String,
    val actionLabel: String? = null,
    val followUp: SnackbarFollowUp? = null,
)
