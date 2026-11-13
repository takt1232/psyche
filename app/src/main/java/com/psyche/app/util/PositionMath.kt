package com.psyche.app.util

import com.psyche.app.data.Account
import com.psyche.app.data.AccountType
import com.psyche.app.data.LedgerEntry
import com.psyche.app.data.SavingsGoal
import com.psyche.app.data.TransactionType
import java.time.LocalDate

enum class PositionStatus(val label: String) {
    NO_ACCOUNTS("NO ACCOUNTS"),
    DEBT_FREE("DEBT-FREE"),
    CAN_SETTLE("CAN SETTLE"),
    SHORT("SHORT"),
}

/**
 * The owner's current position across active accounts and savings goals.
 *
 * [netCents] answers the only solvency question the pass asks: if every
 * active credit card were paid in full today, would any money be left?
 */
data class PositionSummary(
    val netCents: Long,
    val moneyCents: Long,
    val owedCents: Long,
    val cashCents: Long,
    val savingsCents: Long,
    val creditLimitCents: Long,
    val availableCreditCents: Long,
    val accountCount: Int,
    val creditAccountCount: Int,
    val status: PositionStatus,
    val lastEntryAt: Long?,
    val snapshotRef: String,
) {
    val isNegative: Boolean get() = netCents < 0
    val shortfallCents: Long get() = if (netCents < 0) -netCents else 0L
    val coveragePercent: Double?
        get() = if (owedCents > 0) moneyCents.toDouble() / owedCents.toDouble() * 100.0 else null

    companion object {
        fun empty(today: LocalDate = LocalDate.now()): PositionSummary = PositionSummary(
            netCents = 0,
            moneyCents = 0,
            owedCents = 0,
            cashCents = 0,
            savingsCents = 0,
            creditLimitCents = 0,
            availableCreditCents = 0,
            accountCount = 0,
            creditAccountCount = 0,
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
        today: LocalDate = LocalDate.now(),
    ): PositionSummary {
        val activeAccounts = accounts.filterNot { it.archived }
        val activeGoals = goals.filterNot { it.archived }
        val balances = LedgerMath.accountBalances(activeAccounts, entries)
        val cash = balances
            .filter { it.account.type != AccountType.CREDIT }
            .sumOf { it.balanceCents }
        val creditBalances = balances.filter { it.account.type == AccountType.CREDIT }
        val owed = creditBalances.sumOf { it.outstandingCents }
        val prepaid = creditBalances.sumOf { balance -> balance.balanceCents.coerceAtLeast(0L) }
        val savings = LedgerMath.goalProgress(activeGoals, entries).sumOf { it.savedCents }
        val money = cash + savings + prepaid
        val net = money - owed
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
            creditLimitCents = creditBalances.sumOf { it.account.creditLimitCents },
            availableCreditCents = creditBalances.sumOf { it.availableCreditCents },
            accountCount = activeAccounts.size,
            creditAccountCount = creditBalances.size,
            status = status,
            lastEntryAt = entries.maxOfOrNull { it.occurredAt },
            snapshotRef = Formats.snapshotReference(today),
        )
    }

    /**
     * How an entry changes the active-account position. Transfers between two
     * active accounts, card payments, and contributions to active goals are
     * neutral; moving money to an archived account or goal removes it.
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
                val source = if (sourceActive) -entry.amountCents else 0L
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
        appendLine("Credit outstanding: ${Money.formatPeso(position.owedCents, dropZeroCents = false)}")
        appendLine("Net position: ${Money.formatPeso(position.netCents, dropZeroCents = false)}")
        appendLine("Status: ${position.status.label}")
        if (position.shortfallCents > 0) {
            appendLine("Shortfall: ${Money.formatPeso(position.shortfallCents, dropZeroCents = false)}")
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
