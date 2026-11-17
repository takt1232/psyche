package com.psyche.app.util

import com.psyche.app.data.Account
import com.psyche.app.data.DebtTerms
import com.psyche.app.data.InterestMethod
import com.psyche.app.data.LedgerEntry
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.ceil
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToLong

enum class InstallmentStatus {
    PAID,
    DUE,
    OVERDUE,
    UPCOMING,
}

data class Installment(
    val index: Int,
    val dueAt: Long,
    val paymentCents: Long,
    val principalCents: Long,
    val interestCents: Long,
    val balanceCents: Long,
    val status: InstallmentStatus,
)

data class DebtSummary(
    val account: Account,
    val terms: DebtTerms?,
    val balanceCents: Long,
    val installmentCents: Long,
    val nextDueAt: Long?,
    val installments: List<Installment>,
    val paidCount: Int,
    val remainingCount: Int,
    val interestToComeCents: Long,
    val payoffProgress: Float?,
    val aheadMonths: Int,
    val projectedPayoffAt: Long?,
    val overdue: Boolean,
    val due7Cents: Long,
    val due30Cents: Long,
) {
    val isReceivable: Boolean get() = account.type.isReceivable
    val hasSchedule: Boolean get() = installments.isNotEmpty()
    val scheduleLabel: String
        get() = when {
            isReceivable -> "TO COLLECT"
            !hasSchedule -> "BALANCE"
            overdue -> "OVERDUE"
            remainingCount == 0 -> "SETTLED"
            aheadMonths > 0 -> "AHEAD $aheadMonths MO"
            paidCount > 0 -> "$paidCount/${installments.size} PAID"
            else -> "$remainingCount LEFT"
        }
}

/**
 * Derives the amortization projection and the actual progress of a debt from
 * its terms and the ledger. Only principal is a liability; future interest is
 * always a projection, never part of the pass's owed amount.
 */
object DebtMath {

    fun summarize(
        account: Account,
        terms: DebtTerms?,
        entries: List<LedgerEntry>,
        today: LocalDate = LocalDate.now(),
        zone: ZoneId = ZoneId.systemDefault(),
    ): DebtSummary {
        val rawBalance = LedgerMath.accountBalances(listOf(account), entries)
            .firstOrNull()
            ?.balanceCents
            ?: 0L
        val balance = when {
            account.isLiability -> (-rawBalance).coerceAtLeast(0L)
            account.type.isReceivable -> rawBalance.coerceAtLeast(0L)
            else -> 0L
        }
        if (terms == null || !account.type.hasSchedule || !terms.hasSchedule) {
            val original = terms?.let {
                it.originalPrincipalCents.takeIf { value -> value > 0 }
                    ?: it.scheduleStartBalanceCents
            } ?: 0L
            val progress = if (original > 0) {
                ((original - balance).toDouble() / original.toDouble()).toFloat().coerceIn(0f, 1f)
            } else {
                null
            }
            return DebtSummary(
                account = account,
                terms = terms,
                balanceCents = balance,
                installmentCents = 0L,
                nextDueAt = null,
                installments = emptyList(),
                paidCount = 0,
                remainingCount = 0,
                interestToComeCents = 0L,
                payoffProgress = progress,
                aheadMonths = 0,
                projectedPayoffAt = null,
                overdue = false,
                due7Cents = 0L,
                due30Cents = 0L,
            )
        }

        val principalPaid = (terms.scheduleStartBalanceCents - balance).coerceAtLeast(0L)
        val todayStart = today.atStartOfDay(zone).toInstant().toEpochMilli()
        val raw = rawSchedule(terms, zone)
        var remainingPaid = principalPaid
        val covered = raw.map { row ->
            val isCovered = remainingPaid >= row.principalCents
            if (isCovered) remainingPaid -= row.principalCents
            isCovered
        }
        val firstUncovered = covered.indexOfFirst { !it }
        val installments = raw.mapIndexed { index, row ->
            val status = when {
                covered[index] -> InstallmentStatus.PAID
                row.dueAt < todayStart -> InstallmentStatus.OVERDUE
                index == firstUncovered && isDueSoon(row.dueAt, today, zone) -> InstallmentStatus.DUE
                else -> InstallmentStatus.UPCOMING
            }
            Installment(
                index = index,
                dueAt = row.dueAt,
                paymentCents = row.paymentCents,
                principalCents = row.principalCents,
                interestCents = row.interestCents,
                balanceCents = row.balanceCents,
                status = status,
            )
        }

        val firstPending = installments.firstOrNull { it.status != InstallmentStatus.PAID }
        val nextDueAt = firstPending?.dueAt
        val paidCount = installments.count { it.status == InstallmentStatus.PAID }
        val remainingCount = installments.size - paidCount
        val interestToCome = installments
            .filter { it.status != InstallmentStatus.PAID }
            .sumOf { it.interestCents }
        val overdue = installments.any { it.status == InstallmentStatus.OVERDUE }
        val due7 = dueBy(installments, today, 7, zone)
        val due30 = dueBy(installments, today, 30, zone)
        val original = when {
            terms.originalPrincipalCents > 0 -> terms.originalPrincipalCents
            terms.scheduleStartBalanceCents > 0 -> terms.scheduleStartBalanceCents
            else -> 0L
        }
        val progress = if (original > 0) {
            ((original - balance).toDouble() / original.toDouble()).toFloat().coerceIn(0f, 1f)
        } else {
            null
        }
        val expected = expectedBalance(raw, today, zone)
        val ahead = if (balance < expected) monthsAhead(balance, expected, raw, today, zone) else 0
        val payoff = projectedPayoff(terms, balance, raw, nextDueAt, zone)

        return DebtSummary(
            account = account,
            terms = terms,
            balanceCents = balance,
            installmentCents = raw.firstOrNull()?.paymentCents ?: 0L,
            nextDueAt = nextDueAt,
            installments = installments,
            paidCount = paidCount,
            remainingCount = remainingCount,
            interestToComeCents = interestToCome,
            payoffProgress = progress,
            aheadMonths = ahead,
            projectedPayoffAt = payoff,
            overdue = overdue,
            due7Cents = due7,
            due30Cents = due30,
        )
    }

    fun dueWithin(
        debts: List<DebtSummary>,
        days: Long,
        today: LocalDate = LocalDate.now(),
        zone: ZoneId = ZoneId.systemDefault(),
    ): Long = debts
        .filterNot { it.isReceivable }
        .sumOf { debt ->
            when (days) {
                7L -> debt.due7Cents
                30L -> debt.due30Cents
                else -> dueBy(debt.installments, today, days.toInt(), zone)
            }
        }

    /**
     * The financed principal implied by an installment amount. This is the
     * inverse of the schedule formula and lets an in-progress plan start from
     * the monthly payment the owner actually sees.
     */
    fun principalFromPayment(
        monthlyPaymentCents: Long,
        termMonths: Int,
        annualRateBps: Int,
        interestMethod: InterestMethod,
    ): Long {
        val n = termMonths.coerceAtLeast(1)
        val monthlyRate = annualRateBps.toDouble() / 10_000.0 / 12.0
        val totalPayments = monthlyPaymentCents.toDouble() * n
        val principal = when (interestMethod) {
            InterestMethod.ZERO -> totalPayments
            InterestMethod.ADD_ON -> totalPayments / (1.0 + monthlyRate * n)
            InterestMethod.REDUCING -> if (monthlyRate <= 0.0) {
                totalPayments
            } else {
                monthlyPaymentCents * (1.0 - (1.0 + monthlyRate).pow(-n)) / monthlyRate
            }
        }
        return principal.roundToLong().coerceAtLeast(1L)
    }

    /** The schedule balance after [paidInstallments] rows have been settled. */
    fun balanceAfter(
        terms: DebtTerms,
        paidInstallments: Int,
        zone: ZoneId = ZoneId.systemDefault(),
    ): Long {
        if (!terms.hasSchedule) return terms.scheduleStartBalanceCents
        if (paidInstallments <= 0) return terms.scheduleStartBalanceCents
        val rows = rawSchedule(terms, zone)
        return rows.getOrNull(paidInstallments - 1)?.balanceCents ?: 0L
    }

    private data class RawInstallment(
        val dueAt: Long,
        val paymentCents: Long,
        val principalCents: Long,
        val interestCents: Long,
        val balanceCents: Long,
    )

    private fun rawSchedule(terms: DebtTerms, zone: ZoneId): List<RawInstallment> {
        val n = terms.termMonths.coerceAtLeast(1)
        val principal = terms.scheduleStartBalanceCents.coerceAtLeast(0L)
        if (principal <= 0L) return emptyList()
        val monthlyRate = terms.annualRateBps.toDouble() / 10_000.0 / 12.0
        val firstDate = Instant.ofEpochMilli(terms.firstDueAt).atZone(zone).toLocalDate()
        val rows = mutableListOf<RawInstallment>()
        when (terms.interestMethod) {
            InterestMethod.ZERO -> {
                val base = principal / n
                var remaining = principal
                for (i in 0 until n) {
                    val part = if (i == n - 1) remaining else base
                    remaining -= part
                    rows += RawInstallment(
                        dueAt = dueDate(firstDate, i, zone),
                        paymentCents = part,
                        principalCents = part,
                        interestCents = 0L,
                        balanceCents = remaining,
                    )
                }
            }

            InterestMethod.ADD_ON -> {
                val monthlyInterest = (principal * monthlyRate).roundToLong()
                val basePrincipal = principal / n
                var remaining = principal
                for (i in 0 until n) {
                    val part = if (i == n - 1) remaining else basePrincipal
                    remaining -= part
                    rows += RawInstallment(
                        dueAt = dueDate(firstDate, i, zone),
                        paymentCents = part + monthlyInterest,
                        principalCents = part,
                        interestCents = monthlyInterest,
                        balanceCents = remaining,
                    )
                }
            }

            InterestMethod.REDUCING -> {
                val payment = if (monthlyRate == 0.0) {
                    principal.toDouble() / n
                } else {
                    principal * monthlyRate / (1.0 - (1.0 + monthlyRate).pow(-n))
                }.roundToLong().coerceAtLeast(1L)
                var remaining = principal
                for (i in 0 until n) {
                    val interest = (remaining * monthlyRate).roundToLong()
                    val part = if (i == n - 1) {
                        remaining
                    } else {
                        (payment - interest).coerceIn(0L, remaining)
                    }
                    remaining -= part
                    rows += RawInstallment(
                        dueAt = dueDate(firstDate, i, zone),
                        paymentCents = part + interest,
                        principalCents = part,
                        interestCents = interest,
                        balanceCents = remaining,
                    )
                }
            }
        }
        return rows
    }

    private fun dueDate(first: LocalDate, index: Int, zone: ZoneId): Long =
        first.plusMonths(index.toLong()).atStartOfDay(zone).toInstant().toEpochMilli()

    private fun isDueSoon(dueAt: Long, today: LocalDate, zone: ZoneId): Boolean {
        val due = Instant.ofEpochMilli(dueAt).atZone(zone).toLocalDate()
        return !due.isBefore(today) && due.isBefore(today.plusDays(8))
    }

    private fun dueBy(
        rows: List<Installment>,
        today: LocalDate,
        days: Int,
        zone: ZoneId,
    ): Long {
        val start = today.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = today.plusDays(days.toLong()).atStartOfDay(zone).toInstant().toEpochMilli()
        return rows
            .filter {
                it.status != InstallmentStatus.PAID &&
                    (it.status == InstallmentStatus.OVERDUE || it.dueAt in start until end)
            }
            .sumOf { it.paymentCents }
    }

    private fun expectedBalance(
        rows: List<RawInstallment>,
        today: LocalDate,
        zone: ZoneId,
    ): Long {
        if (rows.isEmpty()) return 0L
        val todayStart = today.atStartOfDay(zone).toInstant().toEpochMilli()
        val start = rows.first().balanceCents + rows.first().principalCents
        val duePrincipal = rows.filter { it.dueAt <= todayStart }.sumOf { it.principalCents }
        return (start - duePrincipal).coerceAtLeast(0L)
    }

    private fun monthsAhead(
        balance: Long,
        expected: Long,
        rows: List<RawInstallment>,
        today: LocalDate,
        zone: ZoneId,
    ): Int {
        var advantage = expected - balance
        if (advantage <= 0) return 0
        val todayStart = today.atStartOfDay(zone).toInstant().toEpochMilli()
        var count = 0
        rows.filter { it.dueAt > todayStart }.forEach { row ->
            if (advantage >= row.principalCents) {
                advantage -= row.principalCents
                count += 1
            }
        }
        return count
    }

    private fun projectedPayoff(
        terms: DebtTerms,
        balance: Long,
        rows: List<RawInstallment>,
        nextDueAt: Long?,
        zone: ZoneId,
    ): Long? {
        if (balance <= 0L || rows.isEmpty()) return null
        val payment = rows.first().paymentCents
        if (payment <= 0L) return null
        val monthlyRate = terms.annualRateBps.toDouble() / 10_000.0 / 12.0
        val periods = when (terms.interestMethod) {
            InterestMethod.REDUCING -> {
                if (monthlyRate <= 0.0) {
                    ceil(balance.toDouble() / payment.toDouble()).toInt()
                } else {
                    val interest = balance * monthlyRate
                    if (payment <= interest) return null
                    ceil(-ln(1.0 - interest / payment) / ln(1.0 + monthlyRate)).toInt()
                }
            }

            InterestMethod.ADD_ON, InterestMethod.ZERO -> {
                val principalPer = terms.scheduleStartBalanceCents / terms.termMonths.coerceAtLeast(1)
                if (principalPer <= 0L) return null
                ceil(balance.toDouble() / principalPer.toDouble()).toInt()
            }
        }.coerceAtLeast(1)
        val start = nextDueAt?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() }
            ?: Instant.ofEpochMilli(terms.firstDueAt).atZone(zone).toLocalDate()
        return start.plusMonths((periods - 1).toLong())
            .atStartOfDay(zone)
            .toInstant()
            .toEpochMilli()
    }
}
