package com.psyche.app.util

import com.google.common.truth.Truth.assertThat
import com.psyche.app.data.Account
import com.psyche.app.data.AccountType
import com.psyche.app.data.DebtTerms
import com.psyche.app.data.InterestMethod
import com.psyche.app.data.LedgerEntry
import com.psyche.app.data.TransactionType
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Test

class DebtMathTest {

    private val zone = ZoneId.systemDefault()
    private val today = LocalDate.of(2026, 11, 14)
    private val firstDue = LocalDate.of(2026, 12, 15)
        .atStartOfDay(zone)
        .toInstant()
        .toEpochMilli()

    private fun loan(
        id: Long = 2,
        openingCents: Long = -100_000,
        type: AccountType = AccountType.LOAN,
    ) = Account(
        id = id,
        name = "Loan",
        type = type,
        openingBalanceCents = openingCents,
        creditLimitCents = 0,
        lastFour = null,
        sortOrder = 1,
        archived = false,
    )

    private fun terms(
        principalCents: Long = 100_000,
        rateBps: Int = 0,
        method: InterestMethod = InterestMethod.ZERO,
        termMonths: Int = 4,
    ) = DebtTerms(
        accountId = 2,
        originalPrincipalCents = principalCents,
        annualRateBps = rateBps,
        interestMethod = method,
        termMonths = termMonths,
        firstDueAt = firstDue,
        paymentsPerYear = 12,
        scheduleStartBalanceCents = principalCents,
        scheduleStartAt = firstDue,
    )

    private fun entry(
        id: Long = 1,
        type: TransactionType,
        amountCents: Long,
        accountId: Long,
        transferAccountId: Long? = null,
        interestCents: Long = 0,
    ) = LedgerEntry(
        id = id,
        type = type,
        amountCents = amountCents,
        accountId = accountId,
        categoryId = null,
        transferAccountId = transferAccountId,
        goalId = null,
        description = "",
        occurredAt = firstDue,
        createdAt = firstDue,
        needsAttention = false,
        interestCents = interestCents,
    )

    @Test
    fun `zero interest installment splits principal evenly`() {
        val summary = DebtMath.summarize(
            account = loan(type = AccountType.BNPL),
            terms = terms(method = InterestMethod.ZERO, termMonths = 4),
            entries = emptyList(),
            today = today,
            zone = zone,
        )

        assertThat(summary.installmentCents).isEqualTo(25_000)
        assertThat(summary.installments).hasSize(4)
        assertThat(summary.installments.map { it.principalCents }.sum()).isEqualTo(100_000)
        assertThat(summary.installments.last().balanceCents).isEqualTo(0)
        assertThat(summary.interestToComeCents).isEqualTo(0)
        assertThat(summary.balanceCents).isEqualTo(100_000)
    }

    @Test
    fun `reducing balance computes interest on the falling balance`() {
        val summary = DebtMath.summarize(
            account = loan(),
            terms = terms(rateBps = 1_200, method = InterestMethod.REDUCING, termMonths = 12),
            entries = emptyList(),
            today = today,
            zone = zone,
        )

        val first = summary.installments.first()
        assertThat(first.paymentCents).isEqualTo(8_885)
        assertThat(first.interestCents).isEqualTo(1_000)
        assertThat(first.principalCents).isEqualTo(7_885)
        assertThat(summary.installments.map { it.principalCents }.sum()).isEqualTo(100_000)
        assertThat(summary.installments.last().balanceCents).isEqualTo(0)
        assertThat(summary.interestToComeCents).isGreaterThan(0)
    }

    @Test
    fun `add-on interest is flat on the original principal`() {
        val summary = DebtMath.summarize(
            account = loan(),
            terms = terms(rateBps = 1_200, method = InterestMethod.ADD_ON, termMonths = 12),
            entries = emptyList(),
            today = today,
            zone = zone,
        )

        assertThat(summary.installments.first().interestCents).isEqualTo(1_000)
        assertThat(summary.interestToComeCents).isEqualTo(12_000)
        assertThat(summary.installments.map { it.principalCents }.sum()).isEqualTo(100_000)
        assertThat(summary.installments.last().balanceCents).isEqualTo(0)
    }

    @Test
    fun `future interest is not part of the outstanding balance`() {
        val summary = DebtMath.summarize(
            account = loan(),
            terms = terms(rateBps = 1_200, method = InterestMethod.REDUCING, termMonths = 12),
            entries = emptyList(),
            today = today,
            zone = zone,
        )

        assertThat(summary.balanceCents).isEqualTo(100_000)
        assertThat(summary.interestToComeCents).isGreaterThan(0)
        assertThat(summary.balanceCents).isLessThan(100_000 + summary.interestToComeCents)
    }

    @Test
    fun `a principal payment marks the first installment paid`() {
        val summary = DebtMath.summarize(
            account = loan(),
            terms = terms(rateBps = 1_200, method = InterestMethod.REDUCING, termMonths = 12),
            entries = listOf(
                entry(
                    type = TransactionType.TRANSFER,
                    amountCents = 7_885,
                    accountId = 1,
                    transferAccountId = 2,
                    interestCents = 1_000,
                ),
            ),
            today = today,
            zone = zone,
        )

        assertThat(summary.balanceCents).isEqualTo(92_115)
        assertThat(summary.installments.first().status).isEqualTo(InstallmentStatus.PAID)
        assertThat(summary.paidCount).isEqualTo(1)
        assertThat(summary.remainingCount).isEqualTo(11)
    }

    @Test
    fun `due soon counts only unpaid installments inside the window`() {
        val soon = today.plusDays(10)
            .atStartOfDay(zone)
            .toInstant()
            .toEpochMilli()
        val summary = DebtMath.summarize(
            account = loan(type = AccountType.BNPL),
            terms = terms(method = InterestMethod.ZERO, termMonths = 4).copy(firstDueAt = soon),
            entries = emptyList(),
            today = today,
            zone = zone,
        )

        assertThat(summary.due30Cents).isEqualTo(25_000)
        assertThat(summary.due7Cents).isEqualTo(0)
    }

    @Test
    fun `overdue status appears after the due date`() {
        val summary = DebtMath.summarize(
            account = loan(),
            terms = terms(method = InterestMethod.ZERO, termMonths = 4).copy(
                firstDueAt = today.minusMonths(1)
                    .atStartOfDay(zone)
                    .toInstant()
                    .toEpochMilli(),
            ),
            entries = emptyList(),
            today = today,
            zone = zone,
        )

        assertThat(summary.overdue).isTrue()
        assertThat(summary.installments.first().status).isEqualTo(InstallmentStatus.OVERDUE)
    }

    @Test
    fun `an informal balance keeps payoff progress without a schedule`() {
        val informal = loan(type = AccountType.INFORMAL_DEBT, openingCents = -60_000)
        val summary = DebtMath.summarize(
            account = informal,
            terms = terms(method = InterestMethod.ZERO, termMonths = 4).copy(
                termMonths = 0,
                originalPrincipalCents = 100_000,
            ),
            entries = emptyList(),
            today = today,
            zone = zone,
        )

        assertThat(summary.hasSchedule).isFalse()
        assertThat(summary.balanceCents).isEqualTo(60_000)
        assertThat(summary.payoffProgress).isWithin(0.001f).of(0.4f)
    }

    @Test
    fun `an in-progress zero-interest installment knows eleven of eighteen are paid`() {
        val monthlyPayment = 235_700L
        val financed = DebtMath.principalFromPayment(
            monthlyPaymentCents = monthlyPayment,
            termMonths = 18,
            annualRateBps = 0,
            interestMethod = InterestMethod.ZERO,
        )
        val firstDue = LocalDate.of(2025, 12, 14)
            .atStartOfDay(zone)
            .toInstant()
            .toEpochMilli()
        val plan = DebtTerms(
            accountId = 2,
            originalPrincipalCents = financed,
            annualRateBps = 0,
            interestMethod = InterestMethod.ZERO,
            termMonths = 18,
            firstDueAt = firstDue,
            paymentsPerYear = 12,
            scheduleStartBalanceCents = financed,
            scheduleStartAt = LocalDate.of(2025, 11, 14)
                .atStartOfDay(zone)
                .toInstant()
                .toEpochMilli(),
            counterparty = "Home Credit",
            downPaymentCents = 1_000_000,
        )
        val remaining = DebtMath.balanceAfter(plan, paidInstallments = 11)
        val phone = loan(
            type = AccountType.BNPL,
            openingCents = -remaining,
        )

        val summary = DebtMath.summarize(
            account = phone,
            terms = plan,
            entries = emptyList(),
            today = today,
            zone = zone,
        )

        assertThat(financed).isEqualTo(4_242_600)
        assertThat(remaining).isEqualTo(1_649_900)
        assertThat(summary.balanceCents).isEqualTo(1_649_900)
        assertThat(summary.installmentCents).isEqualTo(235_700)
        assertThat(summary.paidCount).isEqualTo(11)
        assertThat(summary.remainingCount).isEqualTo(7)
        assertThat(summary.installments[10].status).isEqualTo(InstallmentStatus.PAID)
        assertThat(summary.installments[11].status).isEqualTo(InstallmentStatus.DUE)
        assertThat(summary.scheduleLabel).isEqualTo("11/18 PAID")
        assertThat(summary.nextDueAt).isEqualTo(
            LocalDate.of(2026, 11, 14)
                .atStartOfDay(zone)
                .toInstant()
                .toEpochMilli(),
        )
    }

    @Test
    fun `principal from payment inverts the interest formulas`() {
        val zero = DebtMath.principalFromPayment(235_700, 18, 0, InterestMethod.ZERO)
        assertThat(zero).isEqualTo(4_242_600)

        val reducing = DebtMath.principalFromPayment(888_488, 12, 1_200, InterestMethod.REDUCING)
        assertThat(reducing).isWithin(200L).of(10_000_000L)

        val addOn = DebtMath.principalFromPayment(933_333, 12, 1_200, InterestMethod.ADD_ON)
        assertThat(addOn).isWithin(200L).of(10_000_000L)
    }
}
