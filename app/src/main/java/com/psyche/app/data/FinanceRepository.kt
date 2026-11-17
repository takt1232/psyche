package com.psyche.app.data

import com.psyche.app.util.AccountBalance
import com.psyche.app.util.LedgerMath
import com.psyche.app.util.Money
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private data class LedgerPart(
    val accounts: List<AccountEntity>,
    val categories: List<CategoryEntity>,
    val entries: List<LedgerEntryEntity>,
    val debtTerms: List<DebtTermsEntity>,
    val settings: Map<String, String>,
)

class FinanceRepository(
    private val dao: FinanceDao,
    private val now: () -> Long = System::currentTimeMillis,
) {

    /** Serialises balance-checked writes so two fast widget taps cannot overdraw. */
    private val writeMutex = Mutex()

    val snapshot: Flow<FinanceSnapshot> = combine(
        combine(
            dao.observeAccounts(),
            dao.observeCategories(),
            dao.observeEntries(),
            dao.observeDebtTerms(),
            dao.observeSettings(),
        ) { accounts, categories, entries, debtTerms, settings ->
            LedgerPart(
                accounts = accounts,
                categories = categories,
                entries = entries,
                debtTerms = debtTerms,
                settings = settings.associate { it.key to it.value },
            )
        },
        dao.observeGoals(),
        dao.observeQuickTransactions(),
    ) { ledger, goals, quickTransactions ->
        FinanceSnapshot(
            accounts = ledger.accounts.map { it.toDomain() },
            categories = ledger.categories.map { it.toDomain() },
            entries = ledger.entries.map { it.toDomain() },
            goals = goals.map { it.toDomain() },
            quickTransactions = quickTransactions.map { it.toDomain() },
            debtTerms = ledger.debtTerms.map { it.toDomain() },
            settings = FinanceSettings.from(ledger.settings),
        )
    }

    suspend fun addExpense(
        amountCents: Long,
        accountId: Long,
        categoryId: Long?,
        description: String,
        occurredAt: Long = now(),
        lightRow: Boolean = true,
    ): Long = writeMutex.withLock {
        val source = accountBalanceFor(accountId)
        require(!source.account.type.isReceivable) {
            "Money owed to you cannot fund an expense."
        }
        require(!source.account.isScheduledDebt) {
            "Scheduled debts cannot fund everyday expenses."
        }
        require(amountCents <= source.availableToSpendCents) {
            availableMessage(source, source.availableToSpendCents)
        }
        addEntry(
            type = TransactionType.EXPENSE,
            amountCents = amountCents,
            accountId = accountId,
            categoryId = categoryId,
            transferAccountId = null,
            goalId = null,
            description = description,
            occurredAt = occurredAt,
            lightRow = lightRow,
        )
    }

    suspend fun addIncome(
        amountCents: Long,
        accountId: Long,
        description: String,
        occurredAt: Long = now(),
        lightRow: Boolean = true,
    ): Long = writeMutex.withLock {
        val destination = accountBalanceFor(accountId)
        require(!destination.account.type.isReceivable) {
            "Money owed to you cannot receive income. Collect it instead."
        }
        addEntry(
            type = TransactionType.INCOME,
            amountCents = amountCents,
            accountId = accountId,
            categoryId = null,
            transferAccountId = null,
            goalId = null,
            description = description,
            occurredAt = occurredAt,
            lightRow = lightRow,
        )
    }

    suspend fun addTransfer(
        amountCents: Long,
        fromAccountId: Long,
        toAccountId: Long?,
        goalId: Long?,
        description: String,
        occurredAt: Long = now(),
        lightRow: Boolean = true,
    ): Long = writeMutex.withLock {
        require((toAccountId == null) != (goalId == null)) {
            "A transfer needs one destination."
        }
        require(toAccountId == null || toAccountId != fromAccountId) {
            "Choose two different accounts."
        }
        val source = accountBalanceFor(fromAccountId)
        require(source.account.type.canFundTransfers) {
            "Choose a cash, bank, or receivable account."
        }
        require(amountCents <= source.availableToMoveCents) {
            availableMessage(source, source.availableToMoveCents)
        }
        if (goalId != null) {
            require(source.account.type.isLiquid) {
                "Contribute to a goal from cash or a bank account."
            }
            val remaining = goalRemaining(goalId)
            require(amountCents <= remaining) {
                "This goal only needs ${Money.formatPeso(remaining)} more. Raise the target to save more."
            }
        }
        if (toAccountId != null) {
            val destination = accountBalanceFor(toAccountId)
            require(!(source.account.isLiability && destination.account.isLiability)) {
                "Choose a cash or bank account to receive a drawdown."
            }
            require(!(source.account.isLiability && destination.account.type.isReceivable)) {
                "Money from a debt cannot fund money you lend."
            }
            require(!(source.account.type.isReceivable && destination.account.isLiability)) {
                "Collect the money before using it to pay a debt."
            }
            if (destination.account.isLiability) {
                require(destination.outstandingCents > 0) {
                    "This debt has no outstanding balance to pay."
                }
                require(amountCents <= destination.outstandingCents) {
                    "Only ${Money.formatPeso(destination.outstandingCents)} is outstanding on ${destination.account.name}."
                }
            }
            if (source.account.type.isReceivable) {
                require(destination.account.type.isLiquid) {
                    "Collecting must land in cash or a bank account."
                }
            }
        }
        addEntry(
            type = TransactionType.TRANSFER,
            amountCents = amountCents,
            accountId = fromAccountId,
            categoryId = null,
            transferAccountId = toAccountId,
            goalId = goalId,
            description = description,
            occurredAt = occurredAt,
            lightRow = lightRow,
        )
    }

    suspend fun contributeToGoal(
        amountCents: Long,
        accountId: Long,
        goalId: Long,
        description: String,
        occurredAt: Long = now(),
        lightRow: Boolean = true,
    ): Long {
        val goal = dao.allGoals().firstOrNull { it.id == goalId } ?: error("Savings goal not found.")
        return addTransfer(
            amountCents = amountCents,
            fromAccountId = accountId,
            toAccountId = null,
            goalId = goalId,
            description = description.ifBlank { "To ${goal.name}" },
            occurredAt = occurredAt,
            lightRow = lightRow,
        )
    }

    /**
     * Records a debt payment split into principal and interest. The principal is
     * a transfer, the interest is an expense, and only the interest moves the
     * net position. An interest-only payment is stored as an expense.
     */
    suspend fun addDebtPayment(
        debtAccountId: Long,
        fromAccountId: Long,
        principalCents: Long,
        interestCents: Long,
        description: String,
        occurredAt: Long = now(),
        lightRow: Boolean = true,
    ): Long = writeMutex.withLock {
        val debt = accountBalanceFor(debtAccountId)
        require(debt.account.isLiability) { "Choose a debt to pay." }
        require(debt.outstandingCents > 0) { "This debt has no outstanding balance." }
        require(principalCents in 0..debt.outstandingCents) {
            "Only ${Money.formatPeso(debt.outstandingCents)} is outstanding on ${debt.account.name}."
        }
        require(interestCents in 0..Money.MAX_AMOUNT_CENTS) { "Enter a valid interest amount." }
        val total = principalCents + interestCents
        require(total in 1..Money.MAX_AMOUNT_CENTS) {
            "Enter an amount between \u20B10.01 and \u20B1999,999.99."
        }
        val source = accountBalanceFor(fromAccountId)
        require(source.account.type.isLiquid) {
            "A debt payment must come from cash or a bank account."
        }
        require(total <= source.availableToMoveCents) {
            availableMessage(source, source.availableToMoveCents)
        }
        if (principalCents > 0) {
            addEntry(
                type = TransactionType.TRANSFER,
                amountCents = principalCents,
                accountId = fromAccountId,
                categoryId = null,
                transferAccountId = debtAccountId,
                goalId = null,
                description = description,
                occurredAt = occurredAt,
                lightRow = lightRow,
                interestCents = interestCents,
            )
        } else {
            addEntry(
                type = TransactionType.EXPENSE,
                amountCents = interestCents,
                accountId = fromAccountId,
                categoryId = null,
                transferAccountId = null,
                goalId = null,
                description = description,
                occurredAt = occurredAt,
                lightRow = lightRow,
            )
        }
    }

    /**
     * Creates a debt or receivable. The origin decides how the opening balance
     * enters the ledger so a purchase, a cash borrowing, and an existing balance
     * never double count.
     */
    suspend fun addDebt(
        name: String,
        type: AccountType,
        amountCents: Long,
        originalPrincipalCents: Long,
        scheduleStartBalanceCents: Long = 0L,
        downPaymentCents: Long = 0L,
        annualRateBps: Int,
        interestMethod: InterestMethod,
        termMonths: Int,
        firstDueAt: Long,
        counterparty: String?,
        origin: DebtOrigin,
        fundingAccountId: Long?,
        existingAccountId: Long? = null,
        categoryId: Long?,
        description: String = "",
        occurredAt: Long = now(),
    ): Long = writeMutex.withLock {
        require(type.isLiability || type.isReceivable) { "Choose a debt type." }
        require(type != AccountType.CREDIT) { "Add credit cards from Accounts." }
        val clean = name.trim()
        require(clean.isNotEmpty()) { "Name the debt." }
        require(amountCents in 1..Money.MAX_DEBT_CENTS) {
            "Enter a debt amount up to \u20B1999,999,999.99."
        }
        require(originalPrincipalCents in 0..Money.MAX_DEBT_CENTS) {
            "Enter an original principal up to \u20B1999,999,999.99."
        }
        require(scheduleStartBalanceCents in 0..Money.MAX_DEBT_CENTS) {
            "Enter a schedule amount up to \u20B1999,999,999.99."
        }
        require(downPaymentCents in 0..Money.MAX_DEBT_CENTS) {
            "Enter a downpayment up to \u20B1999,999,999.99."
        }
        require(annualRateBps in 0..100_000) { "Enter a valid annual rate." }
        if (type.hasSchedule) {
            require(termMonths in 1..600) { "Enter a term between 1 and 600 months." }
            require(firstDueAt > 0) { "Choose the first due date." }
        }
        require(occurredAt > 0) { "Choose a valid date." }
        when (origin) {
            DebtOrigin.PURCHASE -> require(type == AccountType.BNPL) {
                "Only an installment uses a new purchase."
            }

            DebtOrigin.BORROWED -> require(type.isLiability && !type.isReceivable) {
                "Only a debt can be borrowed."
            }

            DebtOrigin.LENT -> require(type.isReceivable) {
                "Choose money owed to you to record a loan you gave."
            }

            DebtOrigin.IN_PROGRESS -> require(type.hasSchedule) {
                "Only a loan or installment can be already in progress."
            }

            DebtOrigin.EXISTING -> Unit
        }
        if (origin != DebtOrigin.EXISTING && origin != DebtOrigin.IN_PROGRESS) {
            require(fundingAccountId != null || origin == DebtOrigin.PURCHASE) {
                "Choose the account for the money."
            }
        }
        val existingBalance = existingAccountId?.let { accountBalanceFor(it) }
        if (existingBalance != null) {
            require(existingBalance.account.isLiability && !existingBalance.account.isScheduledDebt) {
                "Choose an active credit or informal-debt account to convert."
            }
            require(existingBalance.outstandingCents > 0) {
                "This account has no outstanding balance to convert."
            }
            require(origin == DebtOrigin.IN_PROGRESS || origin == DebtOrigin.EXISTING) {
                "Only an existing balance can use an existing account."
            }
            if (existingBalance.account.isCredit) {
                require(dao.activeQuickTransactionsForAccount(existingBalance.account.id).isEmpty()) {
                    "This card is used by a one-tap action. Archive or edit that action first."
                }
            }
        }
        val effectiveAmount = existingBalance?.outstandingCents ?: amountCents
        if (existingBalance != null && origin == DebtOrigin.IN_PROGRESS) {
            require(amountCents == existingBalance.outstandingCents) {
                "The selected account balance is ${Money.formatPeso(existingBalance.outstandingCents)}, " +
                    "but the plan calculates ${Money.formatPeso(amountCents)}. " +
                    "Adjust the payments made or use a new account."
            }
        }
        val scheduleStart = scheduleStartBalanceCents.takeIf { it > 0 } ?: effectiveAmount
        require(scheduleStart >= effectiveAmount) {
            "The financed amount must be at least the remaining balance."
        }
        if (origin == DebtOrigin.IN_PROGRESS) {
            require(scheduleStart > effectiveAmount) {
                "An in-progress plan needs a financed amount above the remaining balance."
            }
        }
        val fundingBalance = when (origin) {
            DebtOrigin.BORROWED -> {
                val destination = accountBalanceFor(requireNotNull(fundingAccountId))
                require(destination.account.type.isLiquid) {
                    "Loan proceeds must land in cash or a bank account."
                }
                destination
            }

            DebtOrigin.LENT -> {
                val source = accountBalanceFor(requireNotNull(fundingAccountId))
                require(source.account.type.isLiquid) {
                    "Money you lend must come from cash or a bank account."
                }
                require(amountCents <= source.availableToMoveCents) {
                    availableMessage(source, source.availableToMoveCents)
                }
                source
            }

            DebtOrigin.PURCHASE, DebtOrigin.EXISTING, DebtOrigin.IN_PROGRESS -> null
        }

        val accountId = if (existingBalance != null) {
            dao.upsertAccount(
                existingBalance.account.copy(
                    name = clean,
                    type = type,
                    creditLimitCents = 0,
                    lastFour = null,
                    archived = false,
                    isDefault = false,
                ).toEntity(),
            )
            existingBalance.account.id
        } else {
            val opening = when {
                origin == DebtOrigin.EXISTING && type.isLiability -> -effectiveAmount
                origin == DebtOrigin.EXISTING && type.isReceivable -> effectiveAmount
                origin == DebtOrigin.IN_PROGRESS -> -effectiveAmount
                else -> 0L
            }
            val all = dao.allAccounts()
            dao.upsertAccount(
                AccountEntity(
                    name = clean,
                    type = type.storage,
                    openingBalanceCents = opening,
                    creditLimitCents = 0,
                    lastFour = null,
                    sortOrder = (all.maxOfOrNull { it.sortOrder } ?: -1) + 1,
                    archived = false,
                ),
            )
        }
        if (type.isLiability || type.isReceivable) {
            dao.upsertDebtTerms(
                DebtTermsEntity(
                    accountId = accountId,
                    originalPrincipalCents = originalPrincipalCents.takeIf { it > 0 } ?: scheduleStart,
                    annualRateBps = if (type.hasSchedule && interestMethod != InterestMethod.ZERO) {
                        annualRateBps
                    } else {
                        0
                    },
                    interestMethod = if (type.hasSchedule) {
                        interestMethod.storage
                    } else {
                        InterestMethod.ZERO.storage
                    },
                    termMonths = if (type.hasSchedule) termMonths else 0,
                    firstDueAt = if (type.hasSchedule && firstDueAt > 0) firstDueAt else 0,
                    paymentsPerYear = 12,
                    scheduleStartBalanceCents = scheduleStart,
                    scheduleStartAt = occurredAt,
                    counterparty = counterparty?.trim()?.takeIf { it.isNotBlank() },
                    downPaymentCents = downPaymentCents,
                ),
            )
        }

        val note = description.trim().ifBlank { clean }
        when (origin) {
            DebtOrigin.PURCHASE -> {
                dao.insertLightingEntry(
                    LedgerEntryEntity(
                        type = TransactionType.EXPENSE.storage,
                        amountCents = effectiveAmount,
                        accountId = accountId,
                        categoryId = categoryId,
                        transferAccountId = null,
                        goalId = null,
                        description = note,
                        occurredAt = occurredAt,
                        createdAt = now(),
                        needsAttention = true,
                    ),
                )
            }

            DebtOrigin.BORROWED -> {
                val destinationId = requireNotNull(fundingBalance).account.id
                dao.insertLightingEntry(
                    LedgerEntryEntity(
                        type = TransactionType.TRANSFER.storage,
                        amountCents = effectiveAmount,
                        accountId = accountId,
                        categoryId = null,
                        transferAccountId = destinationId,
                        goalId = null,
                        description = note,
                        occurredAt = occurredAt,
                        createdAt = now(),
                        needsAttention = true,
                    ),
                )
            }

            DebtOrigin.LENT -> {
                val sourceId = requireNotNull(fundingBalance).account.id
                dao.insertLightingEntry(
                    LedgerEntryEntity(
                        type = TransactionType.TRANSFER.storage,
                        amountCents = effectiveAmount,
                        accountId = sourceId,
                        categoryId = null,
                        transferAccountId = accountId,
                        goalId = null,
                        description = note,
                        occurredAt = occurredAt,
                        createdAt = now(),
                        needsAttention = true,
                    ),
                )
            }

            DebtOrigin.EXISTING, DebtOrigin.IN_PROGRESS -> Unit
        }
        return accountId
    }

    suspend fun updateDebt(account: Account, terms: DebtTerms?) {
        val clean = account.name.trim()
        require(clean.isNotEmpty()) { "Name the debt." }
        require(account.type.isLiability || account.type.isReceivable) { "Choose a debt account." }
        require(!account.isDefault) { "A debt cannot be the default account." }
        dao.upsertAccount(
            account.copy(
                name = clean,
                creditLimitCents = 0,
                lastFour = null,
                isDefault = false,
            ).toEntity(),
        )
        if (terms != null) {
            require(terms.termMonths in 0..600) { "Enter a term between 0 and 600 months." }
            require(terms.annualRateBps in 0..100_000) { "Enter a valid annual rate." }
            dao.upsertDebtTerms(terms.copy(accountId = account.id).toEntity())
        }
    }

    private fun availableMessage(balance: AccountBalance, availableCents: Long): String =
        if (balance.account.isCredit) {
            "Only ${Money.formatPeso(availableCents)} of credit is available on ${balance.account.name}."
        } else {
            "Only ${Money.formatPeso(availableCents)} is available in ${balance.account.name}."
        }

    private suspend fun addEntry(
        type: TransactionType,
        amountCents: Long,
        accountId: Long,
        categoryId: Long?,
        transferAccountId: Long?,
        goalId: Long?,
        description: String,
        occurredAt: Long,
        lightRow: Boolean,
        interestCents: Long = 0,
    ): Long {
        require(amountCents in 1..Money.MAX_AMOUNT_CENTS) {
            "Enter an amount between \u20B10.01 and \u20B1999,999.99."
        }
        require(interestCents in 0..Money.MAX_AMOUNT_CENTS) {
            "Enter a valid interest amount."
        }
        require(accountId > 0) { "Choose an account." }
        require(occurredAt > 0) { "Choose a valid date." }
        val entry = LedgerEntryEntity(
            type = type.storage,
            amountCents = amountCents,
            accountId = accountId,
            categoryId = categoryId,
            transferAccountId = transferAccountId,
            goalId = goalId,
            description = description.trim(),
            occurredAt = occurredAt,
            createdAt = now(),
            needsAttention = lightRow,
            interestCents = interestCents,
        )
        val id = if (lightRow) dao.insertLightingEntry(entry) else dao.insertEntry(entry)
        setSetting(SettingsKeys.LAST_ACCOUNT_ID, accountId.toString())
        categoryId?.let { setSetting(SettingsKeys.LAST_CATEGORY_ID, it.toString()) }
        return id
    }

    suspend fun updateEntry(entry: LedgerEntry) = writeMutex.withLock {
        require(entry.amountCents in 1..Money.MAX_AMOUNT_CENTS) {
            "Enter an amount between \u20B10.01 and \u20B1999,999.99."
        }
        require(entry.interestCents in 0..Money.MAX_AMOUNT_CENTS) {
            "Enter a valid interest amount."
        }
        require(entry.accountId > 0) { "Choose an account." }
        require(entry.occurredAt > 0) { "Choose a valid date." }
        if (entry.type == TransactionType.TRANSFER) {
            require((entry.transferAccountId == null) xor (entry.goalId == null)) {
                "A transfer needs one destination."
            }
            require(entry.transferAccountId == null || entry.transferAccountId != entry.accountId) {
                "Choose two different accounts."
            }
            require(entry.goalId == null || entry.interestCents == 0L) {
                "A goal transfer cannot carry interest."
            }
            require(entry.interestCents == 0L || entry.transferAccountId != null) {
                "Interest only applies to a debt payment."
            }
            entry.transferAccountId?.takeIf { entry.interestCents > 0 }?.let { destinationId ->
                val destination = accountBalanceFor(destinationId, excludingEntryId = entry.id)
                require(destination.account.isLiability) {
                    "Interest only applies to a debt payment."
                }
            }
            val source = accountBalanceFor(entry.accountId, excludingEntryId = entry.id)
            val total = entry.amountCents + entry.interestCents
            if (source.account.isLiability) {
                require(!source.account.isCredit) {
                    "A credit card cannot be the source of a transfer."
                }
                val destinationId = entry.transferAccountId
                    ?: error("A drawdown needs a destination account.")
                val destination = accountBalanceFor(destinationId, excludingEntryId = entry.id)
                require(destination.account.type.isLiquid) {
                    "A drawdown must land in cash or a bank account."
                }
            } else {
                require(total <= source.availableToMoveCents) {
                    availableMessage(source, source.availableToMoveCents)
                }
            }
            entry.goalId?.let { goalId ->
                require(source.account.type.isLiquid) {
                    "Contribute to a goal from cash or a bank account."
                }
                val remaining = goalRemaining(goalId, excludingEntryId = entry.id)
                require(entry.amountCents <= remaining) {
                    "This goal only needs ${Money.formatPeso(remaining)} more. Raise the target to save more."
                }
            }
            entry.transferAccountId?.let { destinationId ->
                val destination = accountBalanceFor(destinationId, excludingEntryId = entry.id)
                require(!(source.account.isLiability && destination.account.isLiability)) {
                    "Choose a cash or bank account to receive a drawdown."
                }
                if (destination.account.isLiability) {
                    require(destination.outstandingCents > 0) {
                        "This debt has no outstanding balance to pay."
                    }
                    require(entry.amountCents <= destination.outstandingCents) {
                        "Only ${Money.formatPeso(destination.outstandingCents)} is outstanding on ${destination.account.name}."
                    }
                }
                if (source.account.type.isReceivable) {
                    require(destination.account.type.isLiquid) {
                        "Collecting must land in cash or a bank account."
                    }
                }
            }
        } else {
            require(entry.categoryId == null || entry.type == TransactionType.EXPENSE) {
                "Only expenses use a category."
            }
            require(entry.interestCents == 0L) {
                "Only a debt payment carries interest."
            }
            if (entry.type == TransactionType.EXPENSE) {
                val source = accountBalanceFor(entry.accountId, excludingEntryId = entry.id)
                require(!source.account.type.isReceivable) {
                    "Money owed to you cannot fund an expense."
                }
                require(entry.amountCents <= source.availableToSpendCents) {
                    availableMessage(source, source.availableToSpendCents)
                }
            }
        }
        dao.updateEntry(entry.toEntity())
    }

    suspend fun deleteEntry(id: Long) {
        dao.deleteEntry(id)
    }

    suspend fun restoreEntry(entry: LedgerEntry) {
        dao.restoreEntry(entry.toEntity())
    }

    suspend fun clearHighlight(id: Long) {
        dao.clearHighlight(id)
    }

    suspend fun clearAllHighlights() {
        dao.clearHighlights()
    }

    private suspend fun accountBalanceFor(
        accountId: Long,
        excludingEntryId: Long? = null,
    ): AccountBalance {
        val current = snapshot.first()
        val entries = if (excludingEntryId == null) {
            current.entries
        } else {
            current.entries.filterNot { it.id == excludingEntryId }
        }
        return LedgerMath.accountBalances(current.accounts, entries)
            .firstOrNull { it.account.id == accountId }
            ?: error("Account not found.")
    }

    private suspend fun goalRemaining(
        goalId: Long,
        excludingEntryId: Long? = null,
    ): Long {
        val current = snapshot.first()
        val goal = current.goals.firstOrNull { it.id == goalId }
            ?: error("Savings goal not found.")
        val entries = if (excludingEntryId == null) {
            current.entries
        } else {
            current.entries.filterNot { it.id == excludingEntryId }
        }
        val saved = LedgerMath.goalProgress(listOf(goal), entries)
            .firstOrNull()
            ?.savedCents
            ?: 0L
        return (goal.targetCents - saved).coerceAtLeast(0)
    }

    suspend fun addCategory(name: String): Long {
        val clean = name.trim()
        require(clean.isNotEmpty()) { "Name the category." }
        val existing = dao.allCategories()
        require(existing.none { it.name.equals(clean, ignoreCase = true) }) {
            "A category with that name already exists."
        }
        return dao.upsertCategory(
            CategoryEntity(
                name = clean,
                sortOrder = (existing.maxOfOrNull { it.sortOrder } ?: -1) + 1,
                archived = false,
            ),
        )
    }

    suspend fun updateCategory(category: Category) {
        val clean = category.name.trim()
        require(clean.isNotEmpty()) { "Name the category." }
        val existing = dao.allCategories()
        require(existing.none { it.id != category.id && it.name.equals(clean, ignoreCase = true) }) {
            "A category with that name already exists."
        }
        dao.upsertCategory(category.copy(name = clean).toEntity())
    }

    suspend fun setCategoryArchived(id: Long, archived: Boolean) {
        val current = dao.allCategories().firstOrNull { it.id == id } ?: return
        dao.upsertCategory(current.copy(archived = archived))
    }

    suspend fun addAccount(
        name: String,
        type: AccountType,
        openingBalanceCents: Long,
        creditLimitCents: Long,
        lastFour: String?,
    ): Long {
        val clean = name.trim()
        val cleanLastFour = lastFour?.takeIf { it.isNotBlank() }?.takeLast(4)
        require(clean.isNotEmpty()) { "Name the account." }
        require(type == AccountType.CASH || type == AccountType.DEBIT_EWALLET || type == AccountType.CREDIT) {
            "Add loans, installments, informal debt, and receivables from the Debts tab."
        }
        require(
            if (type == AccountType.CREDIT) {
                openingBalanceCents in -Money.MAX_AMOUNT_CENTS..Money.MAX_AMOUNT_CENTS
            } else {
                openingBalanceCents in 0..Money.MAX_AMOUNT_CENTS
            },
        ) {
            "Enter an opening balance up to \u20B1999,999.99."
        }
        require(
            if (type == AccountType.CREDIT) {
                creditLimitCents in 1..Money.MAX_AMOUNT_CENTS
            } else {
                creditLimitCents == 0L
            },
        ) {
            "Enter a credit limit up to \u20B1999,999.99."
        }
        require(cleanLastFour == null || (cleanLastFour.length == 4 && cleanLastFour.all(Char::isDigit))) {
            "Enter the last four card digits, or leave it blank."
        }
        val all = dao.allAccounts()
        return dao.upsertAccount(
            AccountEntity(
                name = clean,
                type = type.storage,
                openingBalanceCents = openingBalanceCents,
                creditLimitCents = if (type == AccountType.CREDIT) creditLimitCents else 0,
                lastFour = cleanLastFour,
                sortOrder = (all.maxOfOrNull { it.sortOrder } ?: -1) + 1,
                archived = false,
            ),
        )
    }

    suspend fun updateAccount(account: Account) {
        val clean = account.name.trim()
        val cleanLastFour = account.lastFour?.takeIf { it.isNotBlank() }?.takeLast(4)
        require(clean.isNotEmpty()) { "Name the account." }
        require(account.type == AccountType.CASH || account.type == AccountType.DEBIT_EWALLET || account.type == AccountType.CREDIT) {
            "Edit loans, installments, informal debt, and receivables from the Debts tab."
        }
        require(!account.isDefault || account.type == AccountType.CASH) {
            "The default account must stay a Cash account."
        }
        require(!account.isDefault || !account.archived) {
            "The default Cash account cannot be archived."
        }
        require(
            if (account.type == AccountType.CREDIT) {
                account.openingBalanceCents in -Money.MAX_AMOUNT_CENTS..Money.MAX_AMOUNT_CENTS
            } else {
                account.openingBalanceCents in 0..Money.MAX_AMOUNT_CENTS
            },
        ) {
            "Enter an opening balance up to \u20B1999,999.99."
        }
        require(
            if (account.type == AccountType.CREDIT) {
                account.creditLimitCents in 1..Money.MAX_AMOUNT_CENTS
            } else {
                account.creditLimitCents == 0L
            },
        ) {
            "Enter a credit limit up to \u20B1999,999.99."
        }
        require(cleanLastFour == null || (cleanLastFour.length == 4 && cleanLastFour.all(Char::isDigit))) {
            "Enter the last four card digits, or leave it blank."
        }
        dao.upsertAccount(
            account.copy(
                name = clean,
                lastFour = cleanLastFour,
                creditLimitCents = if (account.type == AccountType.CREDIT) account.creditLimitCents else 0,
            ).toEntity(),
        )
    }

    suspend fun setAccountArchived(id: Long, archived: Boolean) {
        val current = dao.allAccounts().firstOrNull { it.id == id } ?: return
        require(!current.isDefault || !archived) {
            "The default Cash account cannot be archived."
        }
        if (archived) {
            require(dao.activeQuickTransactionsForAccount(id).isEmpty()) {
                "This account is used by a one-tap action. Archive or edit that action first."
            }
        }
        dao.upsertAccount(current.copy(archived = archived))
    }

    suspend fun addGoal(name: String, targetCents: Long): Long {
        val clean = name.trim()
        require(clean.isNotEmpty()) { "Name the goal." }
        require(targetCents in 1..Money.MAX_AMOUNT_CENTS) {
            "Enter a target up to \u20B1999,999.99."
        }
        val all = dao.allGoals()
        return dao.upsertGoal(
            GoalEntity(
                name = clean,
                targetCents = targetCents,
                sortOrder = (all.maxOfOrNull { it.sortOrder } ?: -1) + 1,
                archived = false,
            ),
        )
    }

    suspend fun updateGoal(goal: SavingsGoal) {
        require(goal.targetCents in 1..Money.MAX_AMOUNT_CENTS) {
            "Enter a target up to \u20B1999,999.99."
        }
        dao.upsertGoal(goal.copy(name = goal.name.trim()).toEntity())
    }

    suspend fun setGoalArchived(id: Long, archived: Boolean) {
        val current = dao.allGoals().firstOrNull { it.id == id } ?: return
        dao.upsertGoal(current.copy(archived = archived))
    }

    suspend fun addQuickTransaction(
        label: String,
        type: TransactionType,
        amountCents: Long,
        accountId: Long,
        categoryId: Long?,
        note: String,
    ): Long {
        validateQuickTransaction(type, amountCents, accountId, categoryId, label)
        val all = dao.allQuickTransactions()
        require(all.count { !it.archived } < MAX_QUICK_ACTIONS) {
            "The widget has four slots. Archive one action first."
        }
        return dao.upsertQuickTransaction(
            QuickTransactionEntity(
                label = label.trim(),
                type = type.storage,
                amountCents = amountCents,
                accountId = accountId,
                categoryId = categoryId?.takeIf { type == TransactionType.EXPENSE },
                note = note.trim().take(80),
                sortOrder = (all.maxOfOrNull { it.sortOrder } ?: -1) + 1,
                archived = false,
            ),
        )
    }

    suspend fun updateQuickTransaction(quick: QuickTransaction) {
        require(quick.id > 0) { "Choose a one-tap action." }
        validateQuickTransaction(
            type = quick.type,
            amountCents = quick.amountCents,
            accountId = quick.accountId,
            categoryId = quick.categoryId,
            label = quick.label,
        )
        val all = dao.allQuickTransactions()
        val activeOthers = all.count { !it.archived && it.id != quick.id }
        if (!quick.archived) {
            require(activeOthers < MAX_QUICK_ACTIONS) {
                "The widget has four slots. Archive one action first."
            }
        }
        dao.upsertQuickTransaction(
            quick.copy(
                label = quick.label.trim(),
                categoryId = quick.categoryId?.takeIf { quick.type == TransactionType.EXPENSE },
                note = quick.note.trim().take(80),
            ).toEntity(),
        )
    }

    suspend fun setQuickTransactionArchived(id: Long, archived: Boolean) {
        val current = dao.allQuickTransactions().firstOrNull { it.id == id } ?: return
        if (!archived) {
            val activeOthers = dao.allQuickTransactions()
                .count { !it.archived && it.id != id }
            require(activeOthers < MAX_QUICK_ACTIONS) {
                "The widget has four slots. Archive one action first."
            }
        }
        dao.upsertQuickTransaction(current.copy(archived = archived))
    }

    suspend fun runQuickTransaction(id: Long): QuickLogResult {
        val quick = dao.allQuickTransactions()
            .firstOrNull { it.id == id && !it.archived }
            ?.toDomain()
            ?: error("This one-tap action is no longer available.")
        val account = dao.allAccounts().firstOrNull { it.id == quick.accountId && !it.archived }
            ?: error("The account for ${quick.label} is archived. Edit the action first.")
        val description = quick.note.ifBlank { quick.label }
        val entryId = when (quick.type) {
            TransactionType.EXPENSE -> addExpense(
                amountCents = quick.amountCents,
                accountId = account.id,
                categoryId = quick.categoryId,
                description = description,
                occurredAt = now(),
                lightRow = true,
            )

            TransactionType.INCOME -> addIncome(
                amountCents = quick.amountCents,
                accountId = account.id,
                description = description,
                occurredAt = now(),
                lightRow = true,
            )

            TransactionType.TRANSFER -> error("One-tap actions are expenses or income.")
        }
        return QuickLogResult(
            entryId = entryId,
            label = quick.label,
            amountCents = quick.amountCents,
            type = quick.type,
        )
    }

    private suspend fun validateQuickTransaction(
        type: TransactionType,
        amountCents: Long,
        accountId: Long,
        categoryId: Long?,
        label: String,
    ) {
        require(type == TransactionType.EXPENSE || type == TransactionType.INCOME) {
            "One-tap actions are expenses or income."
        }
        val clean = label.trim()
        require(clean.isNotEmpty()) { "Name the one-tap action." }
        require(clean.length <= 24) { "Keep the one-tap label under 24 characters." }
        require(amountCents in 1..Money.MAX_AMOUNT_CENTS) {
            "Enter an amount between \u20B10.01 and \u20B1999,999.99."
        }
        require(dao.allAccounts().any { account ->
            account.id == accountId &&
                !account.archived &&
                (AccountType.fromStorage(account.type).isLiquid ||
                    AccountType.fromStorage(account.type) == AccountType.CREDIT)
        }) {
            "Choose an active cash, bank, or card account."
        }
        if (categoryId != null) {
            require(type == TransactionType.EXPENSE) { "Only expenses use a category." }
            require(dao.allCategories().any { it.id == categoryId }) {
                "Choose a category from the list."
            }
        }
    }

    suspend fun setSetting(key: String, value: String) {
        dao.upsertSetting(SettingEntity(key, value))
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        setSetting(SettingsKeys.THEME_MODE, mode.storage)
    }

}
