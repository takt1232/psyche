package com.psyche.app.util

import com.google.common.truth.Truth.assertThat
import com.psyche.app.data.Account
import com.psyche.app.data.AccountType
import com.psyche.app.data.LedgerEntry
import com.psyche.app.data.SavingsGoal
import com.psyche.app.data.TransactionType
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Test

class PositionMathTest {

    private val today = LocalDate.of(2026, 11, 12)
    private val occurredAt = today.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    private val cash = Account(
        id = 1,
        name = "Cash",
        type = AccountType.CASH,
        openingBalanceCents = 100_000,
        creditLimitCents = 0,
        lastFour = null,
        sortOrder = 0,
        archived = false,
        isDefault = true,
    )

    private val card = Account(
        id = 2,
        name = "Card",
        type = AccountType.CREDIT,
        openingBalanceCents = -50_000,
        creditLimitCents = 100_000,
        lastFour = "1234",
        sortOrder = 1,
        archived = false,
    )

    private val goal = SavingsGoal(
        id = 10,
        name = "Buffer",
        targetCents = 100_000,
        sortOrder = 0,
        archived = false,
    )

    private fun entry(
        id: Long,
        type: TransactionType,
        amountCents: Long,
        accountId: Long,
        transferAccountId: Long? = null,
        goalId: Long? = null,
    ) = LedgerEntry(
        id = id,
        type = type,
        amountCents = amountCents,
        accountId = accountId,
        categoryId = null,
        transferAccountId = transferAccountId,
        goalId = goalId,
        description = "",
        occurredAt = occurredAt,
        createdAt = occurredAt,
        needsAttention = false,
    )

    @Test
    fun `net includes savings and subtracts card outstanding`() {
        val position = PositionMath.summarize(
            accounts = listOf(cash, card),
            goals = listOf(goal),
            entries = listOf(
                entry(
                    id = 1,
                    type = TransactionType.TRANSFER,
                    amountCents = 20_000,
                    accountId = cash.id,
                    goalId = goal.id,
                ),
            ),
            today = today,
        )

        assertThat(position.cashCents).isEqualTo(80_000)
        assertThat(position.savingsCents).isEqualTo(20_000)
        assertThat(position.moneyCents).isEqualTo(100_000)
        assertThat(position.owedCents).isEqualTo(50_000)
        assertThat(position.netCents).isEqualTo(50_000)
        assertThat(position.status).isEqualTo(PositionStatus.CAN_SETTLE)
    }

    @Test
    fun `paying a card does not change net`() {
        val position = PositionMath.summarize(
            accounts = listOf(cash, card),
            goals = emptyList(),
            entries = listOf(
                entry(
                    id = 1,
                    type = TransactionType.TRANSFER,
                    amountCents = 10_000,
                    accountId = cash.id,
                    transferAccountId = card.id,
                ),
            ),
            today = today,
        )

        assertThat(position.moneyCents).isEqualTo(90_000)
        assertThat(position.owedCents).isEqualTo(40_000)
        assertThat(position.netCents).isEqualTo(50_000)
    }

    @Test
    fun `moving money to a goal does not change net`() {
        val position = PositionMath.summarize(
            accounts = listOf(cash),
            goals = listOf(goal),
            entries = listOf(
                entry(
                    id = 1,
                    type = TransactionType.TRANSFER,
                    amountCents = 30_000,
                    accountId = cash.id,
                    goalId = goal.id,
                ),
            ),
            today = today,
        )

        assertThat(position.cashCents).isEqualTo(70_000)
        assertThat(position.savingsCents).isEqualTo(30_000)
        assertThat(position.moneyCents).isEqualTo(100_000)
        assertThat(position.netCents).isEqualTo(100_000)
        assertThat(position.status).isEqualTo(PositionStatus.DEBT_FREE)
    }

    @Test
    fun `a card purchase lowers net`() {
        val position = PositionMath.summarize(
            accounts = listOf(cash, card.copy(openingBalanceCents = 0)),
            goals = emptyList(),
            entries = listOf(
                entry(
                    id = 1,
                    type = TransactionType.EXPENSE,
                    amountCents = 25_000,
                    accountId = card.id,
                ),
            ),
            today = today,
        )

        assertThat(position.moneyCents).isEqualTo(100_000)
        assertThat(position.owedCents).isEqualTo(25_000)
        assertThat(position.netCents).isEqualTo(75_000)
        assertThat(position.status).isEqualTo(PositionStatus.CAN_SETTLE)
    }

    @Test
    fun `negative net means the money cannot settle every card`() {
        val position = PositionMath.summarize(
            accounts = listOf(cash.copy(openingBalanceCents = 10_000), card),
            goals = emptyList(),
            entries = emptyList(),
            today = today,
        )

        assertThat(position.netCents).isEqualTo(-40_000)
        assertThat(position.shortfallCents).isEqualTo(40_000)
        assertThat(position.status).isEqualTo(PositionStatus.SHORT)
        assertThat(position.isNegative).isTrue()
    }

    @Test
    fun `archived accounts are excluded from totals`() {
        val archivedCash = cash.copy(
            id = 3,
            name = "Old cash",
            openingBalanceCents = 50_000,
            archived = true,
        )
        val position = PositionMath.summarize(
            accounts = listOf(cash, archivedCash),
            goals = emptyList(),
            entries = emptyList(),
            today = today,
        )

        assertThat(position.accountCount).isEqualTo(1)
        assertThat(position.moneyCents).isEqualTo(100_000)
        assertThat(position.netCents).isEqualTo(100_000)
    }
}
