package com.psyche.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
abstract class FinanceDao {

    @Query("SELECT * FROM accounts ORDER BY archived ASC, sortOrder ASC, id ASC")
    abstract fun observeAccounts(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM categories ORDER BY archived ASC, sortOrder ASC, id ASC")
    abstract fun observeCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM ledger_entries ORDER BY occurredAt DESC, id DESC")
    abstract fun observeEntries(): Flow<List<LedgerEntryEntity>>

    @Query("SELECT * FROM monthly_budgets ORDER BY yearMonth ASC")
    abstract fun observeBudgets(): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM category_allocations")
    abstract fun observeAllocations(): Flow<List<AllocationEntity>>

    @Query("SELECT * FROM savings_goals ORDER BY archived ASC, sortOrder ASC, id ASC")
    abstract fun observeGoals(): Flow<List<GoalEntity>>

    @Query("SELECT * FROM quick_transactions ORDER BY archived ASC, sortOrder ASC, id ASC")
    abstract fun observeQuickTransactions(): Flow<List<QuickTransactionEntity>>

    @Query("SELECT * FROM app_settings")
    abstract fun observeSettings(): Flow<List<SettingEntity>>

    @Query("SELECT * FROM monthly_budgets WHERE yearMonth = :yearMonth")
    abstract suspend fun budget(yearMonth: String): BudgetEntity?

    @Query("SELECT * FROM category_allocations WHERE yearMonth = :yearMonth")
    abstract suspend fun allocations(yearMonth: String): List<AllocationEntity>

    @Query("SELECT * FROM accounts")
    abstract suspend fun allAccounts(): List<AccountEntity>

    @Query("SELECT * FROM categories")
    abstract suspend fun allCategories(): List<CategoryEntity>

    @Query("SELECT * FROM savings_goals")
    abstract suspend fun allGoals(): List<GoalEntity>

    @Query("SELECT * FROM quick_transactions")
    abstract suspend fun allQuickTransactions(): List<QuickTransactionEntity>

    @Query("SELECT * FROM quick_transactions WHERE accountId = :accountId AND archived = 0")
    abstract suspend fun activeQuickTransactionsForAccount(accountId: Long): List<QuickTransactionEntity>

    @Upsert
    abstract suspend fun upsertAccount(account: AccountEntity): Long

    @Upsert
    abstract suspend fun upsertQuickTransaction(quickTransaction: QuickTransactionEntity): Long

    @Upsert
    abstract suspend fun upsertCategory(category: CategoryEntity): Long

    @Upsert
    abstract suspend fun upsertGoal(goal: GoalEntity): Long

    @Upsert
    abstract suspend fun upsertBudget(budget: BudgetEntity)

    @Upsert
    abstract suspend fun upsertSetting(setting: SettingEntity)

    @Insert
    abstract suspend fun insertEntry(entry: LedgerEntryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun restoreEntry(entry: LedgerEntryEntity): Long

    @Update
    abstract suspend fun updateEntry(entry: LedgerEntryEntity)

    @Insert
    abstract suspend fun insertAllocations(allocations: List<AllocationEntity>)

    @Query("DELETE FROM category_allocations WHERE yearMonth = :yearMonth")
    abstract suspend fun deleteAllocations(yearMonth: String)

    @Query("DELETE FROM ledger_entries WHERE id = :id")
    abstract suspend fun deleteEntry(id: Long)

    @Query("UPDATE ledger_entries SET needsAttention = 0")
    abstract suspend fun clearHighlights()

    @Query("UPDATE ledger_entries SET needsAttention = 0 WHERE id = :id")
    abstract suspend fun clearHighlight(id: Long)

    @Transaction
    open suspend fun insertLightingEntry(entry: LedgerEntryEntity): Long {
        clearHighlights()
        return insertEntry(entry.copy(needsAttention = true))
    }

    @Transaction
    open suspend fun replaceAllocations(yearMonth: String, allocations: List<AllocationEntity>) {
        deleteAllocations(yearMonth)
        if (allocations.isNotEmpty()) insertAllocations(allocations)
    }
}
