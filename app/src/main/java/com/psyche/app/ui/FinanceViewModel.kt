package com.psyche.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.psyche.app.data.Account
import com.psyche.app.data.AccountType
import com.psyche.app.data.Category
import com.psyche.app.data.FinanceRepository
import com.psyche.app.data.LedgerEntry
import com.psyche.app.data.QuickLogResult
import com.psyche.app.data.QuickTransaction
import com.psyche.app.data.SavingsGoal
import com.psyche.app.data.SettingsKeys
import com.psyche.app.data.ThemeMode
import com.psyche.app.data.TransactionType
import com.psyche.app.util.Money
import java.time.Clock
import java.time.Duration
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZonedDateTime
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class FinanceViewModel(
    private val repository: FinanceRepository,
    private val clock: Clock = Clock.systemDefaultZone(),
) : ViewModel() {

    private val activityMonth = MutableStateFlow(YearMonth.now(clock))
    private val budgetMonth = MutableStateFlow(YearMonth.now(clock))
    private val today = MutableStateFlow(LocalDate.now(clock))

    private val _events = MutableSharedFlow<UiEvent>(extraBufferCapacity = 8)
    val events = _events.asSharedFlow()

    private var lastDeleted: LedgerEntry? = null

    init {
        viewModelScope.launch {
            while (isActive) {
                val now = ZonedDateTime.now(clock)
                val nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay(now.zone)
                val waitMillis = Duration.between(now, nextMidnight).toMillis().coerceAtLeast(1_000L)
                delay(waitMillis + 1_000L)
                refreshToday()
            }
        }
    }

    val uiState: StateFlow<FinanceUiState> = combine(
        repository.snapshot,
        activityMonth,
        budgetMonth,
        today,
    ) { snapshot, activity, budget, currentDay ->
        FinanceStateBuilder.build(
            snapshot = snapshot,
            today = currentDay,
            activityMonth = activity,
            budgetMonth = budget,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = FinanceUiState(
            isLoading = true,
            today = LocalDate.now(clock),
            homeMonth = YearMonth.now(clock),
            activityMonth = YearMonth.now(clock),
            budgetMonth = YearMonth.now(clock),
        ),
    )

    fun refreshToday() {
        val current = LocalDate.now(clock)
        val previous = today.value
        if (current == previous) return
        val previousMonth = YearMonth.from(previous)
        if (activityMonth.value == previousMonth) {
            activityMonth.value = YearMonth.from(current)
        }
        if (budgetMonth.value == previousMonth) {
            budgetMonth.value = YearMonth.from(current)
        }
        today.value = current
    }

    fun shiftActivityMonth(months: Long) {
        activityMonth.update { it.plusMonths(months) }
    }

    fun shiftBudgetMonth(months: Long) {
        budgetMonth.update { it.plusMonths(months) }
    }

    fun resetActivityMonth() {
        activityMonth.value = YearMonth.now(clock)
    }

    fun resetBudgetMonth() {
        budgetMonth.value = YearMonth.now(clock)
    }

    suspend fun recordCapture(
        draft: CaptureDraft,
        announce: Boolean = true,
    ): Result<Long> = runCatching {
        val id = when (draft.type) {
            CaptureType.EXPENSE -> repository.addExpense(
                amountCents = draft.amountCents,
                accountId = draft.accountId,
                categoryId = draft.categoryId,
                description = draft.note,
                occurredAt = draft.occurredAt,
                lightRow = true,
            )

            CaptureType.INCOME -> repository.addIncome(
                amountCents = draft.amountCents,
                accountId = draft.accountId,
                description = draft.note,
                occurredAt = draft.occurredAt,
                lightRow = true,
            )
        }
        if (announce) {
            val label = if (draft.type == CaptureType.EXPENSE) "Expense" else "Income"
            _events.emit(
                UiEvent(
                    message = "$label logged",
                    actionLabel = "UNDO",
                    followUp = SnackbarFollowUp.UndoCreate(id),
                ),
            )
        }
        id
    }

    suspend fun undoCaptured(entryId: Long): Result<Unit> = runCatching {
        repository.deleteEntry(entryId)
    }

    suspend fun changeCapturedCategory(entryId: Long, categoryId: Long?): Result<Unit> =
        runCatching {
            val entry = repository.snapshot
                .first()
                .entries
                .firstOrNull { it.id == entryId }
                ?: error("Entry not found.")
            repository.updateEntry(entry.copy(categoryId = categoryId))
        }

    suspend fun recordTransfer(
        amountCents: Long,
        fromAccountId: Long,
        toAccountId: Long?,
        goalId: Long?,
        note: String,
        occurredAt: Long = System.currentTimeMillis(),
    ): Result<Long> = runCatching {
        val id = repository.addTransfer(
            amountCents = amountCents,
            fromAccountId = fromAccountId,
            toAccountId = toAccountId,
            goalId = goalId,
            description = note,
            occurredAt = occurredAt,
            lightRow = true,
        )
        val cardPayment = toAccountId?.let { destinationId ->
            repository.snapshot.first().accounts
                .firstOrNull { it.id == destinationId }
                ?.type == AccountType.CREDIT
        } == true
        _events.emit(
            UiEvent(
                message = if (cardPayment) "Card payment recorded" else "Transfer recorded",
                actionLabel = "UNDO",
                followUp = SnackbarFollowUp.UndoCreate(id),
            ),
        )
        id
    }

    suspend fun updateEntry(
        entry: LedgerEntry,
        announce: Boolean = true,
    ): Result<Unit> = runCatching {
        repository.updateEntry(entry)
        if (announce) _events.emit(UiEvent("Entry updated"))
    }

    fun markSeen(entry: LedgerEntry) {
        if (!entry.needsAttention) return
        viewModelScope.launch {
            repository.clearHighlight(entry.id)
        }
    }

    fun deleteEntry(entry: LedgerEntry) {
        viewModelScope.launch {
            runCatching { repository.deleteEntry(entry.id) }
                .onSuccess {
                    lastDeleted = entry
                    _events.emit(
                        UiEvent(
                            message = "Entry deleted",
                            actionLabel = "UNDO",
                            followUp = SnackbarFollowUp.UndoDelete,
                        ),
                    )
                }
                .onFailure {
                    _events.emit(UiEvent("Couldn't delete. Try again."))
                }
        }
    }

    fun handleFollowUp(followUp: SnackbarFollowUp) {
        when (followUp) {
            is SnackbarFollowUp.UndoCreate -> viewModelScope.launch {
                runCatching { repository.deleteEntry(followUp.entryId) }
                _events.emit(UiEvent("Entry removed"))
            }

            SnackbarFollowUp.UndoDelete -> viewModelScope.launch {
                val deleted = lastDeleted
                if (deleted == null) {
                    _events.emit(UiEvent("Nothing to restore"))
                } else {
                    runCatching { repository.restoreEntry(deleted) }
                        .onSuccess {
                            lastDeleted = null
                            _events.emit(UiEvent("Entry restored"))
                        }
                        .onFailure { _events.emit(UiEvent("Couldn't restore. Try again.")) }
                }
            }

            is SnackbarFollowUp.OpenEntry -> Unit
        }
    }

    suspend fun saveBudget(month: YearMonth, capCents: Long): Result<Unit> = runCatching {
        repository.setBudget(month, capCents)
        _events.emit(UiEvent("Cap set for ${month.month.name.lowercase().replaceFirstChar { it.uppercase() }}"))
    }

    suspend fun saveAllocations(month: YearMonth, allocations: Map<Long, Long>): Result<Unit> = runCatching {
        repository.setAllocations(month, allocations)
        _events.emit(UiEvent("Allocations updated"))
    }

    suspend fun createCategory(name: String): Result<Unit> = runCatching {
        repository.addCategory(name)
        _events.emit(UiEvent("Category added"))
    }

    suspend fun updateCategory(category: Category): Result<Unit> = runCatching {
        repository.updateCategory(category)
        _events.emit(UiEvent("Category updated"))
    }

    suspend fun archiveCategory(category: Category): Result<Unit> = runCatching {
        repository.setCategoryArchived(category.id, true)
        _events.emit(UiEvent("Category archived"))
    }

    suspend fun restoreCategory(category: Category): Result<Unit> = runCatching {
        repository.setCategoryArchived(category.id, false)
        _events.emit(UiEvent("Category restored"))
    }

    suspend fun createAccount(
        name: String,
        type: AccountType,
        openingBalanceCents: Long,
        creditLimitCents: Long,
        lastFour: String?,
    ): Result<Unit> = runCatching {
        repository.addAccount(name, type, openingBalanceCents, creditLimitCents, lastFour)
        _events.emit(UiEvent("Account added"))
    }

    suspend fun updateAccount(account: Account): Result<Unit> = runCatching {
        repository.updateAccount(account)
        _events.emit(UiEvent("Account updated"))
    }

    suspend fun archiveAccount(account: Account): Result<Unit> = runCatching {
        repository.setAccountArchived(account.id, true)
        _events.emit(UiEvent("Account archived"))
    }

    suspend fun restoreAccount(account: Account): Result<Unit> = runCatching {
        repository.setAccountArchived(account.id, false)
        _events.emit(UiEvent("Account restored"))
    }

    suspend fun createGoal(name: String, targetCents: Long): Result<Unit> = runCatching {
        repository.addGoal(name, targetCents)
        _events.emit(UiEvent("Savings goal added"))
    }

    suspend fun updateGoal(goal: SavingsGoal): Result<Unit> = runCatching {
        repository.updateGoal(goal)
        _events.emit(UiEvent("Savings goal updated"))
    }

    suspend fun archiveGoal(goal: SavingsGoal): Result<Unit> = runCatching {
        repository.setGoalArchived(goal.id, true)
        _events.emit(UiEvent("Savings goal archived"))
    }

    suspend fun restoreGoal(goal: SavingsGoal): Result<Unit> = runCatching {
        repository.setGoalArchived(goal.id, false)
        _events.emit(UiEvent("Savings goal restored"))
    }

    suspend fun contributeToGoal(
        amountCents: Long,
        accountId: Long,
        goalId: Long,
        note: String,
    ): Result<Long> = runCatching {
        val id = repository.contributeToGoal(amountCents, accountId, goalId, note)
        _events.emit(
            UiEvent(
                message = "Saved to goal",
                actionLabel = "UNDO",
                followUp = SnackbarFollowUp.UndoCreate(id),
            ),
        )
        id
    }

    suspend fun createQuickTransaction(
        label: String,
        type: TransactionType,
        amountCents: Long,
        accountId: Long,
        categoryId: Long?,
        note: String,
    ): Result<Unit> = runCatching {
        repository.addQuickTransaction(label, type, amountCents, accountId, categoryId, note)
        _events.emit(UiEvent("One-tap action added"))
    }

    suspend fun updateQuickTransaction(quick: QuickTransaction): Result<Unit> = runCatching {
        repository.updateQuickTransaction(quick)
        _events.emit(UiEvent("One-tap action updated"))
    }

    suspend fun archiveQuickTransaction(quick: QuickTransaction): Result<Unit> = runCatching {
        repository.setQuickTransactionArchived(quick.id, true)
        _events.emit(UiEvent("One-tap action archived"))
    }

    suspend fun restoreQuickTransaction(quick: QuickTransaction): Result<Unit> = runCatching {
        repository.setQuickTransactionArchived(quick.id, false)
        _events.emit(UiEvent("One-tap action restored"))
    }

    suspend fun runQuickTransaction(quick: QuickTransaction): Result<QuickLogResult> = runCatching {
        repository.runQuickTransaction(quick.id)
    }

    fun runQuickTransactionFromUi(quick: QuickTransaction) {
        viewModelScope.launch {
            runQuickTransaction(quick)
                .onSuccess { result ->
                    _events.emit(
                        UiEvent(
                            message = "${result.label} \u00B7 ${Money.formatPeso(result.amountCents)} logged",
                            actionLabel = "UNDO",
                            followUp = SnackbarFollowUp.UndoCreate(result.entryId),
                        ),
                    )
                }
                .onFailure {
                    _events.emit(UiEvent(it.message ?: "Couldn't log that one-tap action."))
                }
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { repository.setThemeMode(mode) }
    }

    fun notify(message: String) {
        viewModelScope.launch { _events.emit(UiEvent(message)) }
    }

    fun requireEntryDefaults(): Pair<Long?, Long?> = Pair(
        uiState.value.settings.lastAccountId,
        uiState.value.settings.lastCategoryId,
    )

    fun setLastDefaults(accountId: Long, categoryId: Long?) {
        viewModelScope.launch {
            repository.setSetting(SettingsKeys.LAST_ACCOUNT_ID, accountId.toString())
            categoryId?.let { repository.setSetting(SettingsKeys.LAST_CATEGORY_ID, it.toString()) }
        }
    }

    fun transactionTypeForDisplay(entry: LedgerEntry): TransactionType = entry.type

    companion object {
        fun factory(repository: FinanceRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer { FinanceViewModel(repository) }
        }
    }
}
