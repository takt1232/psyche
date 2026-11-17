package com.psyche.app.ui

import com.psyche.app.data.Category
import com.psyche.app.data.DebtOrigin
import com.psyche.app.data.DebtTerms
import com.psyche.app.data.FinanceSettings
import com.psyche.app.data.FinanceSnapshot
import com.psyche.app.data.InterestMethod
import com.psyche.app.data.LedgerEntry
import com.psyche.app.data.QuickTransaction
import com.psyche.app.data.AccountType
import com.psyche.app.util.AccountBalance
import com.psyche.app.util.ActivityRow
import com.psyche.app.util.DebtMath
import com.psyche.app.util.DebtSummary
import com.psyche.app.util.GoalProgress
import com.psyche.app.util.LedgerMath
import com.psyche.app.util.MonthMath
import com.psyche.app.util.MonthSummary
import com.psyche.app.util.PositionMath
import com.psyche.app.util.PositionSummary
import java.time.LocalDate
import java.time.YearMonth

data class ActivityMonthView(
    val month: YearMonth,
    val summary: MonthSummary,
    val rows: List<ActivityRow>,
) {
    val litRow: ActivityRow? get() = rows.firstOrNull { it.entry.needsAttention }
}

data class FinanceUiState(
    val isLoading: Boolean = true,
    val snapshot: FinanceSnapshot = FinanceSnapshot(),
    val today: LocalDate = LocalDate.now(),
    val activityMonth: YearMonth = YearMonth.now(),
    val homeActivity: ActivityMonthView? = null,
    val activity: ActivityMonthView? = null,
    val position: PositionSummary = PositionSummary.empty(),
    val accountBalances: List<AccountBalance> = emptyList(),
    val goals: List<GoalProgress> = emptyList(),
    val debts: List<DebtSummary> = emptyList(),
    val receivables: List<DebtSummary> = emptyList(),
) {
    val settings: FinanceSettings get() = snapshot.settings
    val isFirstRun: Boolean
        get() = snapshot.entries.isEmpty() &&
            accountBalances.all { it.balanceCents == 0L } &&
            goals.all { it.savedCents == 0L }
    val activeCategories: List<Category> get() = snapshot.activeCategories
    val quickTransactions: List<QuickTransaction> get() = snapshot.activeQuickTransactions
    val activeDebts: List<DebtSummary> get() = debts + receivables
    val debtAccountCount: Int get() = debts.size
    val receivableAccountCount: Int get() = receivables.size
}

object FinanceStateBuilder {
    fun build(
        snapshot: FinanceSnapshot,
        today: LocalDate,
        activityMonth: YearMonth = YearMonth.from(today),
    ): FinanceUiState {
        val homeMonth = YearMonth.from(today)
        val balances = LedgerMath.accountBalances(snapshot.activeAccounts, snapshot.entries)
        val goals = LedgerMath.goalProgress(snapshot.activeGoals, snapshot.entries)
        val activeAccountIds = snapshot.activeAccounts
            .filterNot { it.type.isReceivable }
            .map { it.id }
            .toSet()
        val activeGoalIds = snapshot.activeGoals.map { it.id }.toSet()
        val debtSummaries = snapshot.activeDebts.map { account ->
            DebtMath.summarize(
                account = account,
                terms = snapshot.termsFor(account.id),
                entries = snapshot.entries,
                today = today,
            )
        }
        return FinanceUiState(
            isLoading = false,
            snapshot = snapshot,
            today = today,
            activityMonth = activityMonth,
            homeActivity = activityView(
                snapshot = snapshot,
                month = homeMonth,
                today = today,
                activeAccountIds = activeAccountIds,
                activeGoalIds = activeGoalIds,
            ),
            activity = activityView(
                snapshot = snapshot,
                month = activityMonth,
                today = today,
                activeAccountIds = activeAccountIds,
                activeGoalIds = activeGoalIds,
            ),
            position = PositionMath.summarize(
                accounts = snapshot.accounts,
                goals = snapshot.goals,
                entries = snapshot.entries,
                debtTerms = snapshot.debtTerms,
                today = today,
            ),
            accountBalances = balances,
            goals = goals,
            debts = debtSummaries
                .filterNot { it.isReceivable }
                .sortedWith(
                    compareBy<DebtSummary> { it.nextDueAt ?: Long.MAX_VALUE }
                        .thenBy { it.account.name.lowercase() },
                ),
            receivables = debtSummaries
                .filter { it.isReceivable }
                .sortedBy { it.account.name.lowercase() },
        )
    }

    private fun activityView(
        snapshot: FinanceSnapshot,
        month: YearMonth,
        today: LocalDate,
        activeAccountIds: Set<Long>,
        activeGoalIds: Set<Long>,
    ): ActivityMonthView {
        val summary = MonthMath.summarize(
            month = month,
            entries = snapshot.entries,
            today = today,
            activeAccountIds = activeAccountIds,
            activeGoalIds = activeGoalIds,
        )
        val rows = LedgerMath.activityRows(
            entries = snapshot.entries,
            month = month,
            accounts = snapshot.accounts,
            categories = snapshot.categories,
            goals = snapshot.goals,
        )
        return ActivityMonthView(
            month = month,
            summary = summary,
            rows = rows,
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

data class DebtDraft(
    val name: String,
    val type: AccountType,
    val amountCents: Long,
    val originalPrincipalCents: Long,
    val scheduleStartBalanceCents: Long,
    val downPaymentCents: Long,
    val annualRateBps: Int,
    val interestMethod: InterestMethod,
    val termMonths: Int,
    val firstDueAt: Long,
    val counterparty: String?,
    val origin: DebtOrigin,
    val fundingAccountId: Long?,
    val existingAccountId: Long?,
    val categoryId: Long?,
    val note: String,
    val occurredAt: Long,
)

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
