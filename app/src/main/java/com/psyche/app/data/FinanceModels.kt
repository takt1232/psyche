package com.psyche.app.data

import java.time.YearMonth

enum class AccountType(val storage: String, val label: String, val shortLabel: String) {
    CASH("cash", "Cash", "CASH"),
    DEBIT_EWALLET("debit", "Debit / e-wallet", "DEBIT"),
    CREDIT("credit", "Credit", "CREDIT");

    companion object {
        fun fromStorage(value: String): AccountType =
            entries.firstOrNull { it.storage == value } ?: CASH
    }
}

enum class ThemeMode(val storage: String, val label: String) {
    SYSTEM("system", "System"),
    LIGHT("light", "Light"),
    DARK("dark", "Dark");

    companion object {
        fun fromStorage(value: String?): ThemeMode =
            entries.firstOrNull { it.storage == value } ?: SYSTEM
    }
}

enum class TransactionType(val storage: String) {
    EXPENSE("expense"),
    INCOME("income"),
    TRANSFER("transfer");

    companion object {
        fun fromStorage(value: String): TransactionType =
            entries.firstOrNull { it.storage == value } ?: EXPENSE
    }
}

data class Account(
    val id: Long,
    val name: String,
    val type: AccountType,
    val openingBalanceCents: Long,
    val creditLimitCents: Long,
    val lastFour: String?,
    val sortOrder: Int,
    val archived: Boolean,
    val isDefault: Boolean = false,
)

data class Category(
    val id: Long,
    val name: String,
    val sortOrder: Int,
    val archived: Boolean,
)

data class LedgerEntry(
    val id: Long,
    val type: TransactionType,
    val amountCents: Long,
    val accountId: Long,
    val categoryId: Long?,
    val transferAccountId: Long?,
    val goalId: Long?,
    val description: String,
    val occurredAt: Long,
    val createdAt: Long,
    val needsAttention: Boolean,
)

data class Budget(val yearMonth: YearMonth, val capCents: Long)

data class CategoryAllocation(
    val yearMonth: YearMonth,
    val categoryId: Long,
    val allocatedCents: Long,
)

data class SavingsGoal(
    val id: Long,
    val name: String,
    val targetCents: Long,
    val sortOrder: Int,
    val archived: Boolean,
)

/**
 * A preset one-tap action. Tapping it logs the stored amount against the stored
 * account/category immediately, without opening the app.
 */
data class QuickTransaction(
    val id: Long,
    val label: String,
    val type: TransactionType,
    val amountCents: Long,
    val accountId: Long,
    val categoryId: Long?,
    val note: String,
    val sortOrder: Int,
    val archived: Boolean,
)

/** The widget has four buttons; keep the in-app strip aligned with it. */
const val MAX_QUICK_ACTIONS = 4

data class QuickLogResult(
    val entryId: Long,
    val label: String,
    val amountCents: Long,
    val type: TransactionType,
)

data class FinanceSettings(
    val lastAccountId: Long? = null,
    val lastCategoryId: Long? = null,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
) {
    companion object {
        fun from(values: Map<String, String>): FinanceSettings = FinanceSettings(
            lastAccountId = values[SettingsKeys.LAST_ACCOUNT_ID]?.toLongOrNull(),
            lastCategoryId = values[SettingsKeys.LAST_CATEGORY_ID]?.toLongOrNull(),
            themeMode = ThemeMode.fromStorage(values[SettingsKeys.THEME_MODE]),
        )
    }
}

object SettingsKeys {
    const val LAST_ACCOUNT_ID = "last_account_id"
    const val LAST_CATEGORY_ID = "last_category_id"
    const val THEME_MODE = "theme_mode"
}

data class FinanceSnapshot(
    val accounts: List<Account> = emptyList(),
    val categories: List<Category> = emptyList(),
    val entries: List<LedgerEntry> = emptyList(),
    val budgets: Map<YearMonth, Budget> = emptyMap(),
    val allocations: Map<YearMonth, Map<Long, Long>> = emptyMap(),
    val goals: List<SavingsGoal> = emptyList(),
    val quickTransactions: List<QuickTransaction> = emptyList(),
    val settings: FinanceSettings = FinanceSettings(),
) {
    val activeAccounts: List<Account> get() = accounts.filterNot { it.archived }
    val activeCategories: List<Category> get() = categories.filterNot { it.archived }
    val activeGoals: List<SavingsGoal> get() = goals.filterNot { it.archived }
    val activeQuickTransactions: List<QuickTransaction>
        get() = quickTransactions.filterNot { it.archived }
}

fun AccountEntity.toDomain() = Account(
    id = id,
    name = name,
    type = AccountType.fromStorage(type),
    openingBalanceCents = openingBalanceCents,
    creditLimitCents = creditLimitCents,
    lastFour = lastFour,
    sortOrder = sortOrder,
    archived = archived,
    isDefault = isDefault,
)

fun CategoryEntity.toDomain() = Category(
    id = id,
    name = name,
    sortOrder = sortOrder,
    archived = archived,
)

fun LedgerEntryEntity.toDomain() = LedgerEntry(
    id = id,
    type = TransactionType.fromStorage(type),
    amountCents = amountCents,
    accountId = accountId,
    categoryId = categoryId,
    transferAccountId = transferAccountId,
    goalId = goalId,
    description = description,
    occurredAt = occurredAt,
    createdAt = createdAt,
    needsAttention = needsAttention,
)

fun BudgetEntity.toDomain() = Budget(YearMonth.parse(yearMonth), capCents)

fun AllocationEntity.toDomain() = CategoryAllocation(
    yearMonth = YearMonth.parse(yearMonth),
    categoryId = categoryId,
    allocatedCents = allocatedCents,
)

fun GoalEntity.toDomain() = SavingsGoal(
    id = id,
    name = name,
    targetCents = targetCents,
    sortOrder = sortOrder,
    archived = archived,
)

fun QuickTransactionEntity.toDomain() = QuickTransaction(
    id = id,
    label = label,
    type = TransactionType.fromStorage(type),
    amountCents = amountCents,
    accountId = accountId,
    categoryId = categoryId,
    note = note,
    sortOrder = sortOrder,
    archived = archived,
)

fun Category.toEntity() = CategoryEntity(
    id = id,
    name = name,
    sortOrder = sortOrder,
    archived = archived,
)

fun Account.toEntity() = AccountEntity(
    id = id,
    name = name,
    type = type.storage,
    openingBalanceCents = openingBalanceCents,
    creditLimitCents = creditLimitCents,
    lastFour = lastFour,
    sortOrder = sortOrder,
    archived = archived,
    isDefault = isDefault,
)

fun LedgerEntry.toEntity() = LedgerEntryEntity(
    id = id,
    type = type.storage,
    amountCents = amountCents,
    accountId = accountId,
    categoryId = categoryId,
    transferAccountId = transferAccountId,
    goalId = goalId,
    description = description,
    occurredAt = occurredAt,
    createdAt = createdAt,
    needsAttention = needsAttention,
)

fun SavingsGoal.toEntity() = GoalEntity(
    id = id,
    name = name,
    targetCents = targetCents,
    sortOrder = sortOrder,
    archived = archived,
)

fun QuickTransaction.toEntity() = QuickTransactionEntity(
    id = id,
    label = label,
    type = type.storage,
    amountCents = amountCents,
    accountId = accountId,
    categoryId = categoryId,
    note = note,
    sortOrder = sortOrder,
    archived = archived,
)
