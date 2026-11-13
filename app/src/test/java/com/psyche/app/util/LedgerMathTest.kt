package com.psyche.app.util

import com.google.common.truth.Truth.assertThat
import com.psyche.app.data.Account
import com.psyche.app.data.AccountType
import com.psyche.app.data.LedgerEntry
import com.psyche.app.data.SavingsGoal
import com.psyche.app.data.TransactionType
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import org.junit.Test

class LedgerMathTest {

    private val zone = ZoneId.systemDefault()
    private val month = YearMonth.of(2026, 11)
    private val today = LocalDate.of(2026, 11, 12)

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
        openingBalanceCents = 0,
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

    private fun at(day: Int, hour: Int): Long =
        month.atDay(day).atTime(hour, 0).atZone(zone).toInstant().toEpochMilli()

    private fun entry(
        id: Long,
        type: TransactionType,
        amountCents: Long,
        accountId: Long,
        occurredAt: Long,
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
    fun `running net matches the active position after card and goal transfers`() {
        val entries = listOf(
            entry(
                id = 1,
                type = TransactionType.EXPENSE,
                amountCents = 20_000,
                accountId = cash.id,
                occurredAt = at(1, 9),
            ),
            entry(
                id = 2,
                type = TransactionType.TRANSFER,
                amountCents = 10_000,
                accountId = cash.id,
                goalId = goal.id,
                occurredAt = at(2, 9),
            ),
            entry(
                id = 3,
                type = TransactionType.EXPENSE,
                amountCents = 5_000,
                accountId = card.id,
                occurredAt = at(3, 9),
            ),
            entry(
                id = 4,
                type = TransactionType.TRANSFER,
                amountCents = 3_000,
                accountId = cash.id,
                transferAccountId = card.id,
                occurredAt = at(4, 9),
            ),
        )

        val rows = LedgerMath.activityRows(
            entries = entries,
            month = month,
            accounts = listOf(cash, card),
            categories = emptyList(),
            goals = listOf(goal),
        )
        val netById = rows.associate { it.entry.id to it.netAfterCents }

        assertThat(netById[1]).isEqualTo(80_000)
        assertThat(netById[2]).isEqualTo(80_000)
        assertThat(netById[3]).isEqualTo(75_000)
        assertThat(netById[4]).isEqualTo(75_000)

        val position = PositionMath.summarize(
            accounts = listOf(cash, card),
            goals = listOf(goal),
            entries = entries,
            today = today,
        )
        assertThat(rows.first().netAfterCents).isEqualTo(position.netCents)
    }
}
