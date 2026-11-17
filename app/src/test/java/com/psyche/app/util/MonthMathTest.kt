package com.psyche.app.util

import com.google.common.truth.Truth.assertThat
import com.psyche.app.data.LedgerEntry
import com.psyche.app.data.TransactionType
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import org.junit.Test

class MonthMathTest {

    private val zone = ZoneId.systemDefault()
    private val month = YearMonth.of(2026, 11)
    private val today = LocalDate.of(2026, 11, 14)
    private val at = month.atDay(10).atStartOfDay(zone).toInstant().toEpochMilli()

    @Test
    fun `interest in a debt payment counts as spending`() {
        val entry = LedgerEntry(
            id = 1,
            type = TransactionType.TRANSFER,
            amountCents = 10_000,
            accountId = 1,
            categoryId = null,
            transferAccountId = 2,
            goalId = null,
            description = "Debt payment",
            occurredAt = at,
            createdAt = at,
            needsAttention = false,
            interestCents = 1_000,
        )

        val summary = MonthMath.summarize(
            month = month,
            entries = listOf(entry),
            today = today,
            activeAccountIds = setOf(1, 2),
            activeGoalIds = emptySet(),
            zone = zone,
        )

        assertThat(summary.spentCents).isEqualTo(1_000)
        assertThat(summary.incomeCents).isEqualTo(0)
        assertThat(summary.changeCents).isEqualTo(-1_000)
    }

    @Test
    fun `principal in a card payment is not spending`() {
        val entry = LedgerEntry(
            id = 1,
            type = TransactionType.TRANSFER,
            amountCents = 10_000,
            accountId = 1,
            categoryId = null,
            transferAccountId = 2,
            goalId = null,
            description = "Card payment",
            occurredAt = at,
            createdAt = at,
            needsAttention = false,
        )

        val summary = MonthMath.summarize(
            month = month,
            entries = listOf(entry),
            today = today,
            activeAccountIds = setOf(1, 2),
            activeGoalIds = emptySet(),
            zone = zone,
        )

        assertThat(summary.spentCents).isEqualTo(0)
        assertThat(summary.changeCents).isEqualTo(0)
    }
}
