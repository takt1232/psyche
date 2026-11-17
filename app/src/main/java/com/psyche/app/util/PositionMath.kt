package com.psyche.app.util

import com.psyche.app.data.Account
import com.psyche.app.data.DebtTerms
import com.psyche.app.data.LedgerEntry
import com.psyche.app.data.SavingsGoal
import com.psyche.app.data.TransactionType
import java.time.LocalDate
import java.time.ZoneId

enum class PositionStatus(val label: String) {
    NO_ACCOUNTS("NO ACCOUNTS"),
    DEBT_FREE("DEBT-FREE"),
    CAN_SETTLE("CAN SETTLE"),
    SHORT("SHORT"),
}

/**
 * The owner's current position across active accounts, debts, and savings.
 *
 * [netCents] is honest about every liability: if every card, loan, and BNPL
 * were paid in full today, would any liquid money be left? Receivables are
 * tracked separately because a promise cannot settle a debt today.
 */
data class PositionSummary(
    val netCents: Long,
    val moneyCents: Long,
    val owedCents: Long,
    val cashCents: Long,
    val savingsCents: Long,
    val cardOwedCents: Long,
    val debtOwedCents: Long,
    val receivableCents: Long,
    val creditLimitCents: Long,
    val availableCreditCents: Long,
    val accountCount: Int,
    val creditAccountCount: Int,
    val debtCount: Int,
    val receivableCount: Int,
    val due7Cents: Long,
    val due30Cents: Long,
    val nextDueAt: Long?,
    val status: PositionStatus,
    val lastEntryAt: Long?,
    val snapshotRef: String,
) {
    val isNegative: Boolean get() = netCents < 0
    val shortfallCents: Long get() = if (netCents < 0) -netCents else 0L
    val coveragePercent: Double?
        get() = if (owedCents > 0) moneyCents.toDouble() / owedCents.toDouble() * 100.0 else null
    val hasDebt: Boolean get() = owedCents > 0
    val hasReceivables: Boolean get() = receivableCents > 0

    companion object {
        fun empty(today: LocalDate = LocalDate.now()): PositionSummary = PositionSummary(
            netCents = 0,
            moneyCents = 0,
            owedCents = 0,
            cashCents = 0,
            savingsCents = 0,
            cardOwedCents = 0,
            debtOwedCents = 0,
            receivableCents = 0,
            creditLimitCents = 0,
            availableCreditCents = 0,
            accountCount = 0,
            creditAccountCount = 0,
            debtCount = 0,
            receivableCount = 0,
            due7Cents = 0,
            due30Cents = 0,
            nextDueAt = null,
            status = PositionStatus.NO_ACCOUNTS,
            lastEntryAt = null,
            snapshotRef = Formats.snapshotReference(today),
        )
    }
}

object PositionMath {

    fun summarize(
        accounts: List<Account>,
        goals: List<SavingsGoal>,
        entries: List<LedgerEntry>,
        debtTerms: List<DebtTerms> = emptyList(),
        today: LocalDate = LocalDate.now(),
        zone: ZoneId = ZoneId.systemDefault(),
    ): PositionSummary {
        val activeAccounts = accounts.filterNot { it.archived }
        val activeGoals = goals.filterNot { it.archived }
        val balances = LedgerMath.accountBalances(activeAccounts, entries)
        val liquid = balances.filter { it.account.type.isLiquid }
        val cards = balances.filter { it.account.isCredit }
        val debts = balances.filter { it.account.isLiability && !it.account.isCredit }
        val receivables = balances.filter { it.account.type.isReceivable }

        val cash = liquid.sumOf { it.balanceCents }
        val cardOwed = cards.sumOf { it.outstandingCents }
        val debtOwed = debts.sumOf { it.outstandingCents }
        val owed = cardOwed + debtOwed
        val prepaid = cards.sumOf { balance -> balance.balanceCents.coerceAtLeast(0L) }
        val savings = LedgerMath.goalProgress(activeGoals, entries).sumOf { it.savedCents }
        val receivable = receivables.sumOf { it.balanceCents.coerceAtLeast(0L) }
        val money = cash + savings + prepaid
        val net = money - owed
        val summaries = (debts + receivables).map { balance ->
            DebtMath.summarize(
                account = balance.account,
                terms = debtTerms.firstOrNull { it.accountId == balance.account.id },
                entries = entries,
                today = today,
                zone = zone,
            )
        }
        val due7 = DebtMath.dueWithin(summaries, 7, today, zone)
        val due30 = DebtMath.dueWithin(summaries, 30, today, zone)
        val nextDue = summaries
            .filterNot { it.isReceivable }
            .mapNotNull { it.nextDueAt }
            .minOrNull()
        val status = when {
            activeAccounts.isEmpty() -> PositionStatus.NO_ACCOUNTS
            owed == 0L -> PositionStatus.DEBT_FREE
            net >= 0L -> PositionStatus.CAN_SETTLE
            else -> PositionStatus.SHORT
        }
        return PositionSummary(
            netCents = net,
            moneyCents = money,
            owedCents = owed,
            cashCents = cash,
            savingsCents = savings,
            cardOwedCents = cardOwed,
            debtOwedCents = debtOwed,
            receivableCents = receivable,
            creditLimitCents = cards.sumOf { it.account.creditLimitCents },
            availableCreditCents = cards.sumOf { it.availableCreditCents },
            accountCount = activeAccounts.count { it.type.isLiquid || it.isCredit },
            creditAccountCount = cards.size,
            debtCount = debts.size,
            receivableCount = receivables.size,
            due7Cents = due7,
            due30Cents = due30,
            nextDueAt = nextDue,
            status = status,
            lastEntryAt = entries.maxOfOrNull { it.occurredAt },
            snapshotRef = Formats.snapshotReference(today),
        )
    }

    /**
     * How an entry changes the active-account position. Transfers between two
     * active accounts, card payments, debt principal payments, and
     * contributions to active goals are neutral; interest is the only part of a
     * debt payment that moves the net. Receivables are excluded from the net by
     * design, so lending cash lowers it and collecting raises it.
     */
    fun netEffectCents(
        entry: LedgerEntry,
        activeAccountIds: Set<Long>,
        activeGoalIds: Set<Long>,
    ): Long {
        val sourceActive = entry.accountId in activeAccountIds
        return when (entry.type) {
            TransactionType.INCOME -> if (sourceActive) entry.amountCents else 0L
            TransactionType.EXPENSE -> if (sourceActive) -entry.amountCents else 0L
            TransactionType.TRANSFER -> {
                val source = if (sourceActive) -entry.totalCashCents else 0L
                val destination = when {
                    entry.transferAccountId != null ->
                        if (entry.transferAccountId in activeAccountIds) entry.amountCents else 0L

                    entry.goalId != null ->
                        if (entry.goalId in activeGoalIds) entry.amountCents else 0L

                    else -> 0L
                }
                source + destination
            }
        }
    }

    fun summaryText(
        position: PositionSummary,
        balances: List<AccountBalance>,
        goals: List<GoalProgress>,
        today: LocalDate = LocalDate.now(),
    ): String = buildString {
        appendLine("PSYCHE \u00B7 BALANCE SUMMARY")
        appendLine("Ref: ${position.snapshotRef}")
        appendLine("As of: ${Formats.fullDate(today)}")
        if (position.lastEntryAt != null) {
            appendLine(
                "Last entry: ${Formats.fullDate(position.lastEntryAt)} \u00B7 " +
                    Formats.time(position.lastEntryAt),
            )
        }
        appendLine("Money: ${Money.formatPeso(position.moneyCents, dropZeroCents = false)}")
        appendLine("Card outstanding: ${Money.formatPeso(position.cardOwedCents, dropZeroCents = false)}")
        appendLine("Other debt: ${Money.formatPeso(position.debtOwedCents, dropZeroCents = false)}")
        appendLine("Total owed: ${Money.formatPeso(position.owedCents, dropZeroCents = false)}")
        appendLine("Net position: ${Money.formatPeso(position.netCents, dropZeroCents = false)}")
        appendLine("Status: ${position.status.label}")
        if (position.shortfallCents > 0) {
            appendLine("Shortfall: ${Money.formatPeso(position.shortfallCents, dropZeroCents = false)}")
        }
        if (position.due30Cents > 0) {
            appendLine("Due in 30 days: ${Money.formatPeso(position.due30Cents, dropZeroCents = false)}")
        }
        if (position.receivableCents > 0) {
            appendLine("Owed to you: ${Money.formatPeso(position.receivableCents, dropZeroCents = false)}")
        }
        if (balances.isNotEmpty()) {
            appendLine()
            appendLine("Accounts")
            balances.forEach { balance ->
                val account = balance.account
                val suffix = account.lastFour?.let { " \u00B7 \u2022\u2022$it" } ?: ""
                appendLine(
                    "${account.name}$suffix: " +
                        Money.formatPeso(balance.balanceCents, dropZeroCents = false),
                )
            }
        }
        if (goals.isNotEmpty()) {
            appendLine()
            appendLine("Savings goals")
            goals.forEach { goal ->
                appendLine(
                    "${goal.goal.name}: " +
                        "${Money.formatPeso(goal.savedCents, dropZeroCents = false)} of " +
                        Money.formatPeso(goal.goal.targetCents, dropZeroCents = false),
                )
            }
        }
    }
}
