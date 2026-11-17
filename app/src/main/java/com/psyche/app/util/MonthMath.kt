package com.psyche.app.util

import com.psyche.app.data.LedgerEntry
import com.psyche.app.data.TransactionType
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

data class MonthSummary(
    val month: YearMonth,
    val spentCents: Long,
    val incomeCents: Long,
    val changeCents: Long,
    val entryCount: Int,
    val updatedAt: Long?,
    val isCurrentMonth: Boolean,
) {
    val isNegativeChange: Boolean get() = changeCents < 0
}

object MonthMath {

    fun entriesForMonth(
        entries: List<LedgerEntry>,
        month: YearMonth,
        zone: ZoneId = ZoneId.systemDefault(),
    ): List<LedgerEntry> = entries.filter { entry ->
        val date = Instant.ofEpochMilli(entry.occurredAt).atZone(zone).toLocalDate()
        YearMonth.from(date) == month
    }

    fun summarize(
        month: YearMonth,
        entries: List<LedgerEntry>,
        today: LocalDate,
        activeAccountIds: Set<Long>,
        activeGoalIds: Set<Long>,
        zone: ZoneId = ZoneId.systemDefault(),
    ): MonthSummary {
        val monthEntries = entriesForMonth(entries, month, zone)
        val spentCents = monthEntries.sumOf { entry ->
            when (entry.type) {
                TransactionType.EXPENSE -> entry.amountCents
                TransactionType.TRANSFER -> entry.interestCents
                TransactionType.INCOME -> 0L
            }
        }
        val incomeCents = monthEntries
            .filter { it.type == TransactionType.INCOME }
            .sumOf { it.amountCents }
        val changeCents = monthEntries.sumOf { entry ->
            PositionMath.netEffectCents(entry, activeAccountIds, activeGoalIds)
        }
        return MonthSummary(
            month = month,
            spentCents = spentCents,
            incomeCents = incomeCents,
            changeCents = changeCents,
            entryCount = monthEntries.size,
            updatedAt = monthEntries.maxOfOrNull { it.occurredAt },
            isCurrentMonth = month == YearMonth.from(today),
        )
    }
}
