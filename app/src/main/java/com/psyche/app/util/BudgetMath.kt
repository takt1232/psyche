package com.psyche.app.util

import com.psyche.app.data.LedgerEntry
import com.psyche.app.data.TransactionType
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.ChronoUnit

enum class BudgetStatus { NO_CAP, ON_PLAN, TIGHT, OVER, CLOSED }

data class MonthSummary(
    val month: YearMonth,
    val capCents: Long?,
    val spentCents: Long,
    val incomeCents: Long,
    val remainingCents: Long?,
    val percentLeft: Double?,
    val daysLeft: Int,
    val dailyAllowanceCents: Long?,
    val weekIndex: Int,
    val weekCount: Int,
    val status: BudgetStatus,
    val isCurrentMonth: Boolean,
    val updatedAt: Long?,
    val entryCount: Int,
    val sequence: String,
    val periodRef: String,
) {
    val hasCap: Boolean get() = capCents != null
    val isOver: Boolean get() = status == BudgetStatus.OVER
    val overByCents: Long get() = if (remainingCents != null && remainingCents < 0) -remainingCents else 0
    val statusLabel: String
        get() = when (status) {
            BudgetStatus.NO_CAP -> "NO CAP"
            BudgetStatus.ON_PLAN -> "ON PLAN"
            BudgetStatus.TIGHT -> "TIGHT"
            BudgetStatus.OVER -> "OVER"
            BudgetStatus.CLOSED -> "CLOSED"
        }
    val daysLabel: String
        get() = when {
            status == BudgetStatus.NO_CAP -> "SET YOUR CAP"
            !isCurrentMonth && daysLeft == 0 -> "MONTH CLOSED"
            daysLeft == 0 -> "LAST DAY"
            daysLeft == 1 -> "1 DAY REMAINING"
            else -> "$daysLeft DAYS REMAINING"
        }
}

object BudgetMath {

    fun summarize(
        month: YearMonth,
        capCents: Long?,
        entries: List<LedgerEntry>,
        today: LocalDate,
        zone: ZoneId = ZoneId.systemDefault(),
    ): MonthSummary {
        val monthEntries = entriesForMonth(entries, month, zone)
        val expenses = monthEntries.filter { it.type == TransactionType.EXPENSE }
        val spent = expenses.sumOf { it.amountCents }
        val income = monthEntries.filter { it.type == TransactionType.INCOME }.sumOf { it.amountCents }
        val remaining = capCents?.minus(spent)
        val current = YearMonth.from(today)
        val isCurrent = month == current
        val start = month.atDay(1)
        val end = month.atEndOfMonth()
        val daysLeft = when {
            isCurrent -> ChronoUnit.DAYS.between(today, end).toInt().plus(1).coerceAtLeast(0)
            month.isAfter(current) -> month.lengthOfMonth()
            else -> 0
        }
        val anchor = when {
            isCurrent -> today
            month.isBefore(current) -> end
            else -> start
        }
        val firstOffset = start.dayOfWeek.value - 1
        val weekIndex = ((anchor.dayOfMonth + firstOffset - 1) / 7) + 1
        val weekCount = ((month.lengthOfMonth() + firstOffset + 6) / 7)
        val status = when {
            capCents == null -> BudgetStatus.NO_CAP
            remaining != null && remaining < 0 -> BudgetStatus.OVER
            month.isBefore(current) -> BudgetStatus.CLOSED
            isCurrent && remaining != null && remaining * 100 <= capCents * 15 -> BudgetStatus.TIGHT
            else -> BudgetStatus.ON_PLAN
        }
        val percentLeft = if (capCents != null && capCents > 0) {
            (remaining ?: capCents).toDouble() / capCents.toDouble() * 100.0
        } else {
            null
        }
        val dailyAllowance = when {
            capCents == null -> null
            remaining == null -> null
            remaining <= 0 -> 0
            daysLeft <= 0 -> 0
            else -> remaining / daysLeft
        }
        return MonthSummary(
            month = month,
            capCents = capCents,
            spentCents = spent,
            incomeCents = income,
            remainingCents = remaining,
            percentLeft = percentLeft,
            daysLeft = daysLeft,
            dailyAllowanceCents = dailyAllowance,
            weekIndex = weekIndex,
            weekCount = weekCount,
            status = status,
            isCurrentMonth = isCurrent,
            updatedAt = monthEntries.maxOfOrNull { it.occurredAt },
            entryCount = monthEntries.size,
            sequence = Formats.sequence(month),
            periodRef = Formats.periodReference(month),
        )
    }

    fun entriesForMonth(
        entries: List<LedgerEntry>,
        month: YearMonth,
        zone: ZoneId = ZoneId.systemDefault(),
    ): List<LedgerEntry> = entries.filter { entry ->
        val date = java.time.Instant.ofEpochMilli(entry.occurredAt).atZone(zone).toLocalDate()
        YearMonth.from(date) == month
    }

    fun periodSummary(summary: MonthSummary): String = buildString {
        appendLine("PSYCHE \u00B7 ${Formats.monthYear(summary.month)}")
        appendLine("Period ref: ${summary.periodRef}")
        if (summary.capCents == null) {
            appendLine("Cap: not set")
        } else {
            appendLine("Cap: ${Money.formatPeso(summary.capCents, dropZeroCents = false)}")
            appendLine("Left: ${Money.formatPeso(summary.remainingCents ?: 0, dropZeroCents = false)}")
            appendLine("Days left: ${summary.daysLeft}")
            appendLine("Status: ${summary.statusLabel}")
        }
        appendLine("Spent: ${Money.formatPeso(summary.spentCents, dropZeroCents = false)}")
        appendLine("Income: ${Money.formatPeso(summary.incomeCents, dropZeroCents = false)}")
        append("Entries: ${summary.entryCount}")
    }
}
