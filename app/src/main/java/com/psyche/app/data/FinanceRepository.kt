package com.psyche.app.data

import com.psyche.app.util.AccountBalance
import com.psyche.app.util.LedgerMath
import com.psyche.app.util.Money
import java.time.YearMonth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private data class LedgerPart(
    val accounts: List<AccountEntity>,
    val categories: List<CategoryEntity>,
    val entries: List<LedgerEntryEntity>,
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
            dao.observeSettings(),
        ) { accounts, categories, entries, settings ->
            LedgerPart(
                accounts = accounts,
                categories = categories,
                entries = entries,
                settings = settings.associate { it.key to it.value },
            )
        },
        dao.observeBudgets(),
        dao.observeAllocations(),
        dao.observeGoals(),
        dao.observeQuickTransactions(),
    ) { ledger, budgets, allocations, goals, quickTransactions ->
        FinanceSnapshot(
            accounts = ledger.accounts.map { it.toDomain() },
            categories = ledger.categories.map { it.toDomain() },
            entries = ledger.entries.map { it.toDomain() },
            budgets = budgets.map { it.toDomain() }.associateBy { it.yearMonth },
            allocations = allocations
                .map { it.toDomain() }
                .groupBy { it.yearMonth }
                .mapValues { (_, values) -> values.associate { it.categoryId to it.allocatedCents } },
            goals = goals.map { it.toDomain() },
            quickTransactions = quickTransactions.map { it.toDomain() },
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
        require(amountCents <= source.availableToMoveCents) {
            if (source.account.type == AccountType.CREDIT) {
                "Only ${Money.formatPeso(source.availableToMoveCents)} of credit is available on ${source.account.name}."
            } else {
                "Only ${Money.formatPeso(source.availableToMoveCents)} is available in ${source.account.name}."
            }
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
    ): Long = addEntry(
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
        require(amountCents <= source.availableToMoveCents) {
            if (source.account.type == AccountType.CREDIT) {
                "Only ${Money.formatPeso(source.availableToMoveCents)} of credit is available on ${source.account.name}."
            } else {
                "Only ${Money.formatPeso(source.availableToMoveCents)} is available in ${source.account.name}."
            }
        }
        if (goalId != null) {
            val remaining = goalRemaining(goalId)
            require(amountCents <= remaining) {
                "This goal only needs ${Money.formatPeso(remaining)} more. Raise the target to save more."
            }
        }
        if (toAccountId != null) {
            require(source.account.type != AccountType.CREDIT) {
                "A card payment must come from cash or debit."
            }
            val destination = accountBalanceFor(toAccountId)
            if (destination.account.type == AccountType.CREDIT) {
                require(destination.outstandingCents > 0) {
                    "This card has no outstanding balance to pay."
                }
                require(amountCents <= destination.outstandingCents) {
                    "Only ${Money.formatPeso(destination.outstandingCents)} is outstanding on ${destination.account.name}."
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
    ): Long {
        require(amountCents in 1..Money.MAX_AMOUNT_CENTS) {
            "Enter an amount between \u20B10.01 and \u20B1999,999.99."
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
        require(entry.accountId > 0) { "Choose an account." }
        require(entry.occurredAt > 0) { "Choose a valid date." }
        if (entry.type == TransactionType.TRANSFER) {
            require((entry.transferAccountId == null) xor (entry.goalId == null)) {
                "A transfer needs one destination."
            }
            require(entry.transferAccountId == null || entry.transferAccountId != entry.accountId) {
                "Choose two different accounts."
            }
            val source = accountBalanceFor(entry.accountId, excludingEntryId = entry.id)
            require(entry.amountCents <= source.availableToMoveCents) {
                if (source.account.type == AccountType.CREDIT) {
                    "Only ${Money.formatPeso(source.availableToMoveCents)} of credit is available on ${source.account.name}."
                } else {
                    "Only ${Money.formatPeso(source.availableToMoveCents)} is available in ${source.account.name}."
                }
            }
            entry.goalId?.let { goalId ->
                val remaining = goalRemaining(goalId, excludingEntryId = entry.id)
                require(entry.amountCents <= remaining) {
                    "This goal only needs ${Money.formatPeso(remaining)} more. Raise the target to save more."
                }
            }
            entry.transferAccountId?.let { destinationId ->
                require(source.account.type != AccountType.CREDIT) {
                    "A card payment must come from cash or debit."
                }
                val destination = accountBalanceFor(destinationId, excludingEntryId = entry.id)
                if (destination.account.type == AccountType.CREDIT) {
                    require(destination.outstandingCents > 0) {
                        "This card has no outstanding balance to pay."
                    }
                    require(entry.amountCents <= destination.outstandingCents) {
                        "Only ${Money.formatPeso(destination.outstandingCents)} is outstanding on ${destination.account.name}."
                    }
                }
            }
        } else {
            require(entry.categoryId == null || entry.type == TransactionType.EXPENSE) {
                "Only expenses use a category."
            }
            if (entry.type == TransactionType.EXPENSE) {
                val source = accountBalanceFor(entry.accountId, excludingEntryId = entry.id)
                require(entry.amountCents <= source.availableToMoveCents) {
                    if (source.account.type == AccountType.CREDIT) {
                        "Only ${Money.formatPeso(source.availableToMoveCents)} of credit is available on ${source.account.name}."
                    } else {
                        "Only ${Money.formatPeso(source.availableToMoveCents)} is available in ${source.account.name}."
                    }
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

    suspend fun setBudget(month: YearMonth, capCents: Long) {
        require(capCents in 1..Money.MAX_AMOUNT_CENTS) {
            "Enter a cap between \u20B10.01 and \u20B1999,999.99."
        }
        val allocated = snapshotBudgetAllocations(month)
        require(allocated.values.sum() <= capCents) {
            "Category allocations add up to ${Money.formatPeso(allocated.values.sum())}. Raise the cap or lower an allocation first."
        }
        dao.upsertBudget(BudgetEntity(month.toString(), capCents))
    }

    suspend fun setAllocations(month: YearMonth, allocations: Map<Long, Long>) {
        val positive = allocations.filterValues { it > 0 }
        require(positive.values.all { it in 1..Money.MAX_AMOUNT_CENTS }) {
            "Enter an allocation between \u20B10.01 and \u20B1999,999.99."
        }
        val cap = dao.budget(month.toString())?.capCents
        require(cap != null || positive.isEmpty()) { "Set a monthly cap before allocating categories." }
        require(cap == null || positive.values.sum() <= cap) {
            "Allocations cannot exceed the ${Money.formatPeso(cap ?: 0)} cap."
        }
        dao.replaceAllocations(
            month.toString(),
            positive.map { (categoryId, cents) ->
                AllocationEntity(month.toString(), categoryId, cents)
            },
        )
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

    private suspend fun snapshotBudgetAllocations(month: YearMonth): Map<Long, Long> =
        dao.allocations(month.toString()).associate { it.categoryId to it.allocatedCents }

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
        require(dao.allAccounts().any { it.id == accountId && !it.archived }) {
            "Choose an active account."
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
