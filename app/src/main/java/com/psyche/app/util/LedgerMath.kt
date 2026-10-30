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
        get() = if (account.type == AccountType.CREDIT && balanceCents < 0) -balanceCents else 0

    val availableCreditCents: Long
        get() = if (account.type == AccountType.CREDIT) {
            (account.creditLimitCents + balanceCents).coerceAtLeast(0)
        } else {
            0
        }

    /** What can leave this account without making a transfer overdraw it. */
    val availableToMoveCents: Long
        get() = when (account.type) {
            AccountType.CREDIT -> availableCreditCents
            else -> balanceCents.coerceAtLeast(0)
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

data class CategoryLine(
    val category: Category,
    val allocatedCents: Long?,
    val spentCents: Long,
) {
    val remainingCents: Long? get() = allocatedCents?.minus(spentCents)
    val isOver: Boolean
        get() {
            val remaining = remainingCents
            return remaining != null && remaining < 0
        }
    val fraction: Float
        get() = if (allocatedCents == null || allocatedCents <= 0) {
            0f
        } else {
            (spentCents.toFloat() / allocatedCents).coerceIn(0f, 1f)
        }
}

data class ActivityRow(
    val entry: LedgerEntry,
    val leftAfterCents: Long?,
    val accountName: String,
    val categoryName: String?,
    val destinationName: String?,
    val destinationType: AccountType? = null,
) {
    val isCardPayment: Boolean
        get() = entry.type == TransactionType.TRANSFER &&
            entry.transferAccountId != null &&
            destinationType == AccountType.CREDIT

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
                    else -> destinationName?.let { "To $it" } ?: "Transfer"
                }
            }
        }

    val metaLine: String
        get() = when (entry.type) {
            TransactionType.EXPENSE -> listOfNotNull(categoryName ?: "Uncategorized", accountName).joinToString(" \u00B7 ")
            TransactionType.INCOME -> accountName
            TransactionType.TRANSFER -> when {
                entry.goalId != null -> "Transfer \u00B7 $accountName"
                isCardPayment -> "Card payment \u00B7 $accountName \u2192 ${destinationName ?: "card"}"
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
                        TransactionType.TRANSFER -> -entry.amountCents
                    }
                }
                if (entry.type == TransactionType.TRANSFER && entry.transferAccountId == account.id) {
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

    fun categoryLines(
        categories: List<Category>,
        entries: List<LedgerEntry>,
        month: YearMonth,
        allocations: Map<Long, Long>,
        zone: ZoneId = ZoneId.systemDefault(),
    ): List<CategoryLine> {
        val monthExpenses = BudgetMath.entriesForMonth(entries, month, zone)
            .filter { it.type == TransactionType.EXPENSE }
        val spentByCategory = monthExpenses
            .mapNotNull { entry -> entry.categoryId?.let { it to entry.amountCents } }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, values) -> values.sum() }
        return categories.map { category ->
            CategoryLine(
                category = category,
                allocatedCents = allocations[category.id],
                spentCents = spentByCategory[category.id] ?: 0,
            )
        }
    }

    fun unallocatedSpending(
        entries: List<LedgerEntry>,
        month: YearMonth,
        zone: ZoneId = ZoneId.systemDefault(),
    ): Long = BudgetMath.entriesForMonth(entries, month, zone)
        .filter { it.type == TransactionType.EXPENSE && it.categoryId == null }
        .sumOf { it.amountCents }

    fun activityRows(
        entries: List<LedgerEntry>,
        month: YearMonth,
        capCents: Long?,
        accounts: List<Account>,
        categories: List<Category>,
        goals: List<SavingsGoal>,
        zone: ZoneId = ZoneId.systemDefault(),
    ): List<ActivityRow> {
        val accountNames = accounts.associate { it.id to it.name }
        val categoryNames = categories.associate { it.id to it.name }
        val goalNames = goals.associate { it.id to it.name }
        val monthEntries = BudgetMath.entriesForMonth(entries, month, zone)
        val ascending = monthEntries.sortedWith(compareBy({ it.occurredAt }, { it.id }))
        val leftById = mutableMapOf<Long, Long?>()
        if (capCents != null) {
            var left = capCents
            ascending.forEach { entry ->
                if (entry.type == TransactionType.EXPENSE) left -= entry.amountCents
                leftById[entry.id] = left
            }
        } else {
            ascending.forEach { leftById[it.id] = null }
        }
        return monthEntries
            .sortedWith(compareByDescending<LedgerEntry> { it.occurredAt }.thenByDescending { it.id })
            .map { entry ->
                val destination = when (entry.type) {
                    TransactionType.TRANSFER -> entry.transferAccountId?.let { accountNames[it] }
                        ?: entry.goalId?.let { goalNames[it] }
                    else -> null
                }
                val destinationType = entry.transferAccountId?.let { destinationId ->
                    accounts.firstOrNull { it.id == destinationId }?.type
                }
                ActivityRow(
                    entry = entry,
                    leftAfterCents = leftById[entry.id],
                    accountName = accountNames[entry.accountId] ?: "Account removed",
                    categoryName = entry.categoryId?.let { categoryNames[it] },
                    destinationName = destination,
                    destinationType = destinationType,
                )
            }
    }

    fun cashTotal(balances: List<AccountBalance>): Long = balances
        .filter { it.account.type != AccountType.CREDIT }
        .sumOf { it.balanceCents }

    fun savingsTotal(goals: List<GoalProgress>): Long = goals.sumOf { it.savedCents }
}
