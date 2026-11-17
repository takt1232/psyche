package com.psyche.app.util

import com.psyche.app.data.Account
import com.psyche.app.data.AccountType
import com.psyche.app.data.Category
import com.psyche.app.data.LedgerEntry
import com.psyche.app.data.SavingsGoal
import com.psyche.app.data.TransactionType
import java.time.YearMonth
import java.time.ZoneId

data class AccountBalance(
    val account: Account,
    val balanceCents: Long,
) {
    val outstandingCents: Long
        get() = if (account.isLiability && balanceCents < 0) -balanceCents else 0

    val availableCreditCents: Long
        get() = if (account.isCredit) {
            (account.creditLimitCents + balanceCents).coerceAtLeast(0)
        } else {
            0
        }

    /** What can leave this account as a transfer without overdrawing it. */
    val availableToMoveCents: Long
        get() = when {
            account.type.canFundTransfers -> balanceCents.coerceAtLeast(0)
            else -> 0
        }

    /**
     * What can be charged to this account as an expense. A card is capped by
     * available credit; a non-revolving liability has no hard cap.
     */
    val availableToSpendCents: Long
        get() = when {
            account.isCredit -> availableCreditCents
            account.type.isLiquid -> balanceCents.coerceAtLeast(0)
            account.isLiability -> Money.MAX_AMOUNT_CENTS
            else -> 0
        }
}

data class GoalProgress(
    val goal: SavingsGoal,
    val savedCents: Long,
) {
    val fraction: Float
        get() = if (goal.targetCents <= 0) 0f else (savedCents.toFloat() / goal.targetCents).coerceIn(0f, 1f)

    val remainingCents: Long get() = (goal.targetCents - savedCents).coerceAtLeast(0)
}

data class ActivityRow(
    val entry: LedgerEntry,
    val netAfterCents: Long?,
    val accountName: String,
    val categoryName: String?,
    val destinationName: String?,
    val destinationType: AccountType? = null,
    val sourceType: AccountType? = null,
) {
    val isCardPayment: Boolean
        get() = entry.type == TransactionType.TRANSFER &&
            entry.transferAccountId != null &&
            destinationType == AccountType.CREDIT

    val isDebtPayment: Boolean
        get() = entry.type == TransactionType.TRANSFER &&
            entry.transferAccountId != null &&
            destinationType?.isLiability == true

    val isReceivableCollection: Boolean
        get() = entry.type == TransactionType.TRANSFER && sourceType?.isReceivable == true

    val isLoanDrawdown: Boolean
        get() = entry.type == TransactionType.TRANSFER && sourceType?.isLiability == true

    /** The cash that actually left the account for this row. */
    val displayAmountCents: Long
        get() = if (entry.type == TransactionType.TRANSFER) entry.totalCashCents else entry.amountCents

    val marker: TransactionMarker
        get() = when (entry.type) {
            TransactionType.EXPENSE -> TransactionMarker.EXPENSE
            TransactionType.INCOME -> TransactionMarker.INCOME
            TransactionType.TRANSFER -> TransactionMarker.TRANSFER
        }

    val displayDescription: String
        get() = entry.description.ifBlank {
            when (entry.type) {
                TransactionType.EXPENSE -> categoryName ?: "Expense"
                TransactionType.INCOME -> "Income"
                TransactionType.TRANSFER -> when {
                    isCardPayment -> destinationName?.let { "Card payment to $it" } ?: "Card payment"
                    isDebtPayment ->
                        destinationName?.let { "Payment to $it" } ?: "Debt payment"

                    isReceivableCollection ->
                        accountName.let { "Collected from $it" }

                    isLoanDrawdown ->
                        accountName.let { "Borrowed from $it" }

                    destinationType?.isReceivable == true ->
                        destinationName?.let { "Lent to $it" } ?: "Lent"

                    else -> destinationName?.let { "To $it" } ?: "Transfer"
                }
            }
        }

    val metaLine: String
        get() = when (entry.type) {
            TransactionType.EXPENSE ->
                listOfNotNull(categoryName ?: "Uncategorized", accountName).joinToString(" \u00B7 ")

            TransactionType.INCOME -> accountName
            TransactionType.TRANSFER -> when {
                entry.goalId != null -> "Transfer \u00B7 $accountName"
                isCardPayment -> "Card payment \u00B7 $accountName \u2192 ${destinationName ?: "card"}"
                isDebtPayment -> buildString {
                    append("Debt payment \u00B7 ")
                    append(accountName)
                    append(" \u2192 ")
                    append(destinationName ?: "debt")
                    if (entry.interestCents > 0) {
                        append(" \u00B7 interest ")
                        append(Money.formatPeso(entry.interestCents))
                    }
                }

                isReceivableCollection -> "Collection \u00B7 $accountName"
                isLoanDrawdown -> "Borrowed \u00B7 $accountName"
                destinationType?.isReceivable == true ->
                    "Lent \u00B7 $accountName \u2192 ${destinationName ?: "receivable"}"

                else -> "Transfer \u00B7 $accountName \u2192 ${destinationName ?: "account"}"
            }
        }
}

object LedgerMath {

    fun accountBalances(accounts: List<Account>, entries: List<LedgerEntry>): List<AccountBalance> =
        accounts.map { account ->
            var balance = account.openingBalanceCents
            entries.forEach { entry ->
                if (entry.accountId == account.id) {
                    balance += when (entry.type) {
                        TransactionType.EXPENSE -> -entry.amountCents
                        TransactionType.INCOME -> entry.amountCents
                        // Interest leaves the source but never reduces principal.
                        TransactionType.TRANSFER -> -(entry.amountCents + entry.interestCents)
                    }
                }
                if (entry.type == TransactionType.TRANSFER && entry.transferAccountId == account.id) {
                    // Only the principal portion reaches the liability.
                    balance += entry.amountCents
                }
            }
            AccountBalance(account, balance)
        }

    fun goalProgress(goals: List<SavingsGoal>, entries: List<LedgerEntry>): List<GoalProgress> =
        goals.map { goal ->
            val saved = entries
                .filter { it.type == TransactionType.TRANSFER && it.goalId == goal.id }
                .sumOf { it.amountCents }
            GoalProgress(goal, saved)
        }

    fun activityRows(
        entries: List<LedgerEntry>,
        month: YearMonth,
        accounts: List<Account>,
        categories: List<Category>,
        goals: List<SavingsGoal>,
        zone: ZoneId = ZoneId.systemDefault(),
    ): List<ActivityRow> {
        val accountNames = accounts.associate { it.id to it.name }
        val accountTypes = accounts.associate { it.id to it.type }
        val categoryNames = categories.associate { it.id to it.name }
        val goalNames = goals.associate { it.id to it.name }
        val activeAccountIds = accounts
            .filterNot { it.archived }
            .filterNot { it.type.isReceivable }
            .map { it.id }
            .toSet()
        val activeGoalIds = goals.filterNot { it.archived }.map { it.id }.toSet()
        val monthEntries = MonthMath.entriesForMonth(entries, month, zone)
        val monthStart = month.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        var runningNet = accounts
            .filterNot { it.archived }
            .filterNot { it.type.isReceivable }
            .sumOf { it.openingBalanceCents }
        entries.forEach { entry ->
            if (entry.occurredAt < monthStart) {
                runningNet += PositionMath.netEffectCents(entry, activeAccountIds, activeGoalIds)
            }
        }
        val netById = mutableMapOf<Long, Long>()
        monthEntries
            .sortedWith(compareBy({ it.occurredAt }, { it.id }))
            .forEach { entry ->
                runningNet += PositionMath.netEffectCents(entry, activeAccountIds, activeGoalIds)
                netById[entry.id] = runningNet
            }
        return monthEntries
            .sortedWith(compareByDescending<LedgerEntry> { it.occurredAt }.thenByDescending { it.id })
            .map { entry ->
                val destination = when (entry.type) {
                    TransactionType.TRANSFER -> entry.transferAccountId?.let { accountNames[it] }
                        ?: entry.goalId?.let { goalNames[it] }
                    else -> null
                }
                val destinationType = entry.transferAccountId?.let { accountTypes[it] }
                ActivityRow(
                    entry = entry,
                    netAfterCents = netById[entry.id],
                    accountName = accountNames[entry.accountId] ?: "Account removed",
                    categoryName = entry.categoryId?.let { categoryNames[it] },
                    destinationName = destination,
                    destinationType = destinationType,
                    sourceType = accountTypes[entry.accountId],
                )
            }
    }
}
