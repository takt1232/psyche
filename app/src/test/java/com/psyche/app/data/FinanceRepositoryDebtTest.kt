package com.psyche.app.data

import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

class FinanceRepositoryDebtTest {

    private val dao = mockk<FinanceDao>(relaxed = true)
    private val inserted = mutableListOf<LedgerEntryEntity>()

    private val cash = AccountEntity(
        id = 1,
        name = "Cash",
        type = "cash",
        openingBalanceCents = 100_000,
        creditLimitCents = 0,
        lastFour = null,
        sortOrder = 0,
        archived = false,
        isDefault = true,
    )

    private val loan = AccountEntity(
        id = 2,
        name = "Salary loan",
        type = "loan",
        openingBalanceCents = -50_000,
        creditLimitCents = 0,
        lastFour = null,
        sortOrder = 1,
        archived = false,
    )

    private val homeCredit = AccountEntity(
        id = 3,
        name = "Home Credit",
        type = "credit",
        openingBalanceCents = -1_649_900,
        creditLimitCents = 50_000_000,
        lastFour = null,
        sortOrder = 2,
        archived = false,
    )

    private fun repository(
        accounts: List<AccountEntity> = listOf(cash, loan),
    ): FinanceRepository {
        every { dao.observeAccounts() } returns flowOf(accounts)
        every { dao.observeCategories() } returns flowOf(emptyList())
        every { dao.observeEntries() } returns flowOf(emptyList())
        every { dao.observeSettings() } returns flowOf(emptyList())
        every { dao.observeGoals() } returns flowOf(emptyList())
        every { dao.observeQuickTransactions() } returns flowOf(emptyList())
        every { dao.observeDebtTerms() } returns flowOf(emptyList())
        coEvery { dao.allAccounts() } returns accounts
        coEvery { dao.allGoals() } returns emptyList()
        coEvery { dao.allCategories() } returns emptyList()
        coEvery { dao.upsertSetting(any()) } returns Unit
        coEvery { dao.upsertAccount(any()) } returns 99L
        coEvery { dao.upsertDebtTerms(any()) } returns Unit
        coEvery { dao.insertLightingEntry(any()) } answers {
            inserted += firstArg<LedgerEntryEntity>()
            42L
        }
        return FinanceRepository(dao, now = { 1_000L })
    }

    @Test
    fun `a debt payment stores principal and interest separately`() = runTest {
        repository().addDebtPayment(
            debtAccountId = 2,
            fromAccountId = 1,
            principalCents = 10_000,
            interestCents = 1_000,
            description = "Monthly payment",
        )

        val entry = inserted.single()
        assertThat(entry.type).isEqualTo("transfer")
        assertThat(entry.amountCents).isEqualTo(10_000)
        assertThat(entry.interestCents).isEqualTo(1_000)
        assertThat(entry.accountId).isEqualTo(1)
        assertThat(entry.transferAccountId).isEqualTo(2)
    }

    @Test
    fun `a payment above the outstanding balance is rejected`() = runTest {
        val error = runCatching {
            repository().addDebtPayment(
                debtAccountId = 2,
                fromAccountId = 1,
                principalCents = 60_000,
                interestCents = 0,
                description = "Too much",
            )
        }.exceptionOrNull()

        assertThat(error).isInstanceOf(IllegalArgumentException::class.java)
        assertThat(error).hasMessageThat().contains("outstanding")
        coVerify(exactly = 0) { dao.insertLightingEntry(any()) }
    }

    @Test
    fun `an existing loan enters as an opening balance without a second entry`() = runTest {
        repository().addDebt(
            name = "Car loan",
            type = AccountType.LOAN,
            amountCents = 500_000,
            originalPrincipalCents = 600_000,
            annualRateBps = 1_200,
            interestMethod = InterestMethod.REDUCING,
            termMonths = 36,
            firstDueAt = LocalDate.of(2026, 12, 15)
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli(),
            counterparty = "Bank",
            origin = DebtOrigin.EXISTING,
            fundingAccountId = null,
            categoryId = null,
        )

        coVerify {
            dao.upsertAccount(
                match {
                    it.type == "loan" &&
                        it.openingBalanceCents == -500_000L
                },
            )
        }
        coVerify { dao.upsertDebtTerms(any()) }
        assertThat(inserted).isEmpty()
    }

    @Test
    fun `a BNPL purchase creates the debt and one expense`() = runTest {
        repository().addDebt(
            name = "Phone",
            type = AccountType.BNPL,
            amountCents = 30_000,
            originalPrincipalCents = 30_000,
            annualRateBps = 0,
            interestMethod = InterestMethod.ZERO,
            termMonths = 6,
            firstDueAt = LocalDate.of(2026, 12, 15)
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli(),
            counterparty = "Shop",
            origin = DebtOrigin.PURCHASE,
            fundingAccountId = null,
            categoryId = null,
        )

        val entry = inserted.single()
        assertThat(entry.type).isEqualTo("expense")
        assertThat(entry.accountId).isEqualTo(99)
        assertThat(entry.amountCents).isEqualTo(30_000)
    }

    @Test
    fun `borrowing deposits cash without changing net at creation`() = runTest {
        repository().addDebt(
            name = "Salary loan",
            type = AccountType.LOAN,
            amountCents = 20_000,
            originalPrincipalCents = 20_000,
            annualRateBps = 1_200,
            interestMethod = InterestMethod.ADD_ON,
            termMonths = 12,
            firstDueAt = LocalDate.of(2026, 12, 15)
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli(),
            counterparty = "Lender",
            origin = DebtOrigin.BORROWED,
            fundingAccountId = 1,
            categoryId = null,
        )

        val entry = inserted.single()
        assertThat(entry.type).isEqualTo("transfer")
        assertThat(entry.accountId).isEqualTo(99)
        assertThat(entry.transferAccountId).isEqualTo(1)
        assertThat(entry.amountCents).isEqualTo(20_000)
    }

    @Test
    fun `lending money moves cash into a receivable`() = runTest {
        repository().addDebt(
            name = "Loan to Ana",
            type = AccountType.RECEIVABLE,
            amountCents = 5_000,
            originalPrincipalCents = 5_000,
            annualRateBps = 0,
            interestMethod = InterestMethod.ZERO,
            termMonths = 0,
            firstDueAt = 0,
            counterparty = "Ana",
            origin = DebtOrigin.LENT,
            fundingAccountId = 1,
            categoryId = null,
        )

        val entry = inserted.single()
        assertThat(entry.type).isEqualTo("transfer")
        assertThat(entry.accountId).isEqualTo(1)
        assertThat(entry.transferAccountId).isEqualTo(99)
        assertThat(entry.amountCents).isEqualTo(5_000)
    }

    @Test
    fun `an in-progress installment converts an existing credit account`() = runTest {
        val firstDue = LocalDate.of(2025, 12, 14)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        repository(accounts = listOf(cash, loan, homeCredit)).addDebt(
            name = "ROG Phone",
            type = AccountType.BNPL,
            amountCents = 1_649_900,
            originalPrincipalCents = 4_242_600,
            scheduleStartBalanceCents = 4_242_600,
            downPaymentCents = 1_000_000,
            annualRateBps = 0,
            interestMethod = InterestMethod.ZERO,
            termMonths = 18,
            firstDueAt = firstDue,
            counterparty = "Home Credit",
            origin = DebtOrigin.IN_PROGRESS,
            fundingAccountId = null,
            existingAccountId = 3,
            categoryId = null,
        )

        coVerify {
            dao.upsertAccount(
                match {
                    it.id == 3L &&
                        it.name == "ROG Phone" &&
                        it.type == "bnpl" &&
                        it.openingBalanceCents == -1_649_900L &&
                        it.creditLimitCents == 0L
                },
            )
        }
        coVerify {
            dao.upsertDebtTerms(
                match {
                    it.accountId == 3L &&
                        it.scheduleStartBalanceCents == 4_242_600L &&
                        it.downPaymentCents == 1_000_000L &&
                        it.termMonths == 18
                },
            )
        }
        assertThat(inserted).isEmpty()
    }
}
