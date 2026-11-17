package com.psyche.app.data

/**
 * Account behaviour is driven by these flags, never by a hard-coded
 * `type == CREDIT` check. A card is a revolving liability; a loan or BNPL is a
 * scheduled liability; a receivable is money owed to the owner.
 */
enum class AccountType(
    val storage: String,
    val label: String,
    val shortLabel: String,
    val isLiability: Boolean = false,
    val isLiquid: Boolean = false,
    val isReceivable: Boolean = false,
    val canFundTransfers: Boolean = false,
    val requiresLimit: Boolean = false,
    val hasSchedule: Boolean = false,
) {
    CASH("cash", "Cash", "CASH", isLiquid = true, canFundTransfers = true),
    DEBIT_EWALLET("debit", "Debit / e-wallet", "DEBIT", isLiquid = true, canFundTransfers = true),
    CREDIT(
        storage = "credit",
        label = "Credit",
        shortLabel = "CREDIT",
        isLiability = true,
        requiresLimit = true,
    ),
    LOAN(
        storage = "loan",
        label = "Loan",
        shortLabel = "LOAN",
        isLiability = true,
        hasSchedule = true,
    ),
    BNPL(
        storage = "bnpl",
        label = "Installment",
        shortLabel = "INSTALLMENT",
        isLiability = true,
        hasSchedule = true,
    ),
    INFORMAL_DEBT(
        storage = "utang",
        label = "Informal debt",
        shortLabel = "UTANG",
        isLiability = true,
    ),
    RECEIVABLE(
        storage = "receivable",
        label = "Owed to you",
        shortLabel = "TO COLLECT",
        isReceivable = true,
        canFundTransfers = true,
    );

    companion object {
        fun fromStorage(value: String): AccountType =
            entries.firstOrNull { it.storage == value } ?: CASH
    }
}

enum class InterestMethod(val storage: String, val label: String) {
    REDUCING("reducing", "Reducing balance"),
    ADD_ON("add_on", "Add-on / flat"),
    ZERO("zero", "Zero interest");

    companion object {
        fun fromStorage(value: String): InterestMethod =
            entries.firstOrNull { it.storage == value } ?: ZERO
    }
}

enum class DebtOrigin(val storage: String, val label: String) {
    EXISTING("existing", "Already owe it"),
    IN_PROGRESS("in_progress", "Already paying it"),
    PURCHASE("purchase", "Buying it now"),
    BORROWED("borrowed", "Borrowed cash"),
    LENT("lent", "Lent money"),
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
) {
    val isLiability: Boolean get() = type.isLiability
    val isCredit: Boolean get() = type == AccountType.CREDIT
    val isScheduledDebt: Boolean get() = type.hasSchedule
}

/**
 * Schedule metadata for a loan, BNPL, or receivable. The outstanding balance
 * still lives in the account's opening balance and ledger entries; these are
 * the terms used to derive the amortization projection.
 */
data class DebtTerms(
    val accountId: Long,
    val originalPrincipalCents: Long,
    val annualRateBps: Int,
    val interestMethod: InterestMethod,
    val termMonths: Int,
    val firstDueAt: Long,
    val paymentsPerYear: Int = 12,
    val scheduleStartBalanceCents: Long,
    val scheduleStartAt: Long,
    val counterparty: String? = null,
    val downPaymentCents: Long = 0,
) {
    val hasSchedule: Boolean get() = termMonths > 0 && firstDueAt > 0 && scheduleStartBalanceCents > 0
}

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
    /**
     * For a debt payment transfer, [amountCents] is the principal portion and
     * this is the interest/fee portion. Cash leaves by amount + interest, the
     * liability falls by amount, and only the interest moves the net position.
     */
    val interestCents: Long = 0,
) {
    val totalCashCents: Long get() = amountCents + interestCents
}

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
    val goals: List<SavingsGoal> = emptyList(),
    val quickTransactions: List<QuickTransaction> = emptyList(),
    val debtTerms: List<DebtTerms> = emptyList(),
    val settings: FinanceSettings = FinanceSettings(),
) {
    val activeAccounts: List<Account> get() = accounts.filterNot { it.archived }
    val activeCategories: List<Category> get() = categories.filterNot { it.archived }
    val activeGoals: List<SavingsGoal> get() = goals.filterNot { it.archived }
    val activeQuickTransactions: List<QuickTransaction>
        get() = quickTransactions.filterNot { it.archived }

    /** Cash, bank, and e-wallet accounts. */
    val activeLiquidAccounts: List<Account>
        get() = activeAccounts.filter { it.type.isLiquid }

    /** Revolving credit lines. */
    val activeCreditAccounts: List<Account>
        get() = activeAccounts.filter { it.isCredit }

    /** Loans, BNPL, and informal debt. */
    val activeDebtAccounts: List<Account>
        get() = activeAccounts.filter { it.type.isLiability && !it.isCredit }

    /** Money owed to the owner. */
    val activeReceivableAccounts: List<Account>
        get() = activeAccounts.filter { it.type.isReceivable }

    /** Accounts that can fund an everyday expense. */
    val activeSpendAccounts: List<Account>
        get() = activeAccounts.filter { it.type.isLiquid || it.isCredit }

    /** Accounts that can be the source of a transfer. */
    val activeTransferSourceAccounts: List<Account>
        get() = activeAccounts.filter { it.type.canFundTransfers }

    val activeDebts: List<Account>
        get() = activeDebtAccounts + activeReceivableAccounts

    fun termsFor(accountId: Long): DebtTerms? = debtTerms.firstOrNull { it.accountId == accountId }
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
    interestCents = interestCents,
)

fun DebtTermsEntity.toDomain() = DebtTerms(
    accountId = accountId,
    originalPrincipalCents = originalPrincipalCents,
    annualRateBps = annualRateBps,
    interestMethod = InterestMethod.fromStorage(interestMethod),
    termMonths = termMonths,
    firstDueAt = firstDueAt,
    paymentsPerYear = paymentsPerYear,
    scheduleStartBalanceCents = scheduleStartBalanceCents,
    scheduleStartAt = scheduleStartAt,
    counterparty = counterparty,
    downPaymentCents = downPaymentCents,
)

fun DebtTerms.toEntity() = DebtTermsEntity(
    accountId = accountId,
    originalPrincipalCents = originalPrincipalCents,
    annualRateBps = annualRateBps,
    interestMethod = interestMethod.storage,
    termMonths = termMonths,
    firstDueAt = firstDueAt,
    paymentsPerYear = paymentsPerYear,
    scheduleStartBalanceCents = scheduleStartBalanceCents,
    scheduleStartAt = scheduleStartAt,
    counterparty = counterparty,
    downPaymentCents = downPaymentCents,
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
    interestCents = interestCents,
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
