package com.psyche.app.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: String,
    val openingBalanceCents: Long,
    val creditLimitCents: Long,
    val lastFour: String?,
    val sortOrder: Int,
    val archived: Boolean,
    @ColumnInfo(defaultValue = "0") val isDefault: Boolean = false,
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val sortOrder: Int,
    val archived: Boolean,
)

@Entity(
    tableName = "ledger_entries",
    indices = [
        Index("occurredAt"),
        Index("accountId"),
        Index("categoryId"),
        Index("goalId"),
    ],
)
data class LedgerEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val amountCents: Long,
    val accountId: Long,
    val categoryId: Long?,
    val transferAccountId: Long?,
    val goalId: Long?,
    val description: String,
    val occurredAt: Long,
    val createdAt: Long,
    val needsAttention: Boolean,
    @ColumnInfo(defaultValue = "0") val interestCents: Long = 0,
)

/**
 * One schedule row per debt account. The outstanding balance is still derived
 * from the account and ledger; this table only holds the terms used to project
 * the amortization.
 */
@Entity(tableName = "debt_terms")
data class DebtTermsEntity(
    @PrimaryKey val accountId: Long,
    val originalPrincipalCents: Long,
    val annualRateBps: Int,
    val interestMethod: String,
    val termMonths: Int,
    val firstDueAt: Long,
    val paymentsPerYear: Int,
    val scheduleStartBalanceCents: Long,
    val scheduleStartAt: Long,
    val counterparty: String?,
    @ColumnInfo(defaultValue = "0") val downPaymentCents: Long = 0,
)

@Entity(tableName = "savings_goals")
data class GoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val targetCents: Long,
    val sortOrder: Int,
    val archived: Boolean,
)

@Entity(tableName = "quick_transactions")
data class QuickTransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String,
    val type: String,
    val amountCents: Long,
    val accountId: Long,
    val categoryId: Long?,
    val note: String,
    val sortOrder: Int,
    val archived: Boolean,
)

@Entity(tableName = "app_settings")
data class SettingEntity(
    @PrimaryKey val key: String,
    val value: String,
)
