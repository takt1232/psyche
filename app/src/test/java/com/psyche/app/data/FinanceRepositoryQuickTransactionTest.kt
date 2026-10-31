package com.psyche.app.data

import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

class FinanceRepositoryQuickTransactionTest {

    private val dao = mockk<FinanceDao>(relaxed = true)

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

    private fun repository(
        accounts: List<AccountEntity> = listOf(cash),
        quick: List<QuickTransactionEntity> = emptyList(),
        categories: List<CategoryEntity> = emptyList(),
    ): FinanceRepository {
        every { dao.observeAccounts() } returns flowOf(accounts)
        every { dao.observeCategories() } returns flowOf(categories)
        every { dao.observeEntries() } returns flowOf(emptyList())
        every { dao.observeSettings() } returns flowOf(emptyList())
        every { dao.observeBudgets() } returns flowOf(emptyList())
        every { dao.observeAllocations() } returns flowOf(emptyList())
        every { dao.observeGoals() } returns flowOf(emptyList())
        every { dao.observeQuickTransactions() } returns flowOf(quick)
        coEvery { dao.allAccounts() } returns accounts
        coEvery { dao.allCategories() } returns categories
        coEvery { dao.allQuickTransactions() } returns quick
        coEvery { dao.upsertSetting(any()) } returns Unit
        coEvery { dao.insertLightingEntry(any()) } returns 99L
        coEvery { dao.upsertQuickTransaction(any()) } returns 7L
        return FinanceRepository(dao, now = { 1_000L })
    }

    @Test
    fun `run quick transaction logs the stored expense`() = runTest {
        val quick = QuickTransactionEntity(
            id = 7,
            label = "Coffee",
            type = "expense",
            amountCents = 15_000,
            accountId = 1,
            categoryId = null,
            note = "",
            sortOrder = 0,
            archived = false,
        )
        val repository = repository(quick = listOf(quick))

        val result = repository.runQuickTransaction(7)

        assertThat(result.label).isEqualTo("Coffee")
        assertThat(result.amountCents).isEqualTo(15_000)
        assertThat(result.entryId).isEqualTo(99L)
        coVerify {
            dao.insertLightingEntry(
                match {
                    it.type == "expense" &&
                        it.amountCents == 15_000L &&
                        it.description == "Coffee" &&
                        it.accountId == 1L
                },
            )
        }
    }

    @Test
    fun `a fifth active action is rejected`() = runTest {
        val active = (1..4).map { index ->
            QuickTransactionEntity(
                id = index.toLong(),
                label = "Action $index",
                type = "expense",
                amountCents = 100,
                accountId = 1,
                categoryId = null,
                note = "",
                sortOrder = index,
                archived = false,
            )
        }
        val repository = repository(quick = active)

        val error = runCatching {
            repository.addQuickTransaction(
                label = "Fifth",
                type = TransactionType.EXPENSE,
                amountCents = 100,
                accountId = 1,
                categoryId = null,
                note = "",
            )
        }.exceptionOrNull()

        assertThat(error).isInstanceOf(IllegalArgumentException::class.java)
        assertThat(error).hasMessageThat().contains("four slots")
    }

    @Test
    fun `an account used by an active action cannot be archived`() = runTest {
        val quick = QuickTransactionEntity(
            id = 7,
            label = "Coffee",
            type = "expense",
            amountCents = 15_000,
            accountId = 1,
            categoryId = null,
            note = "",
            sortOrder = 0,
            archived = false,
        )
        val repository = repository(
            accounts = listOf(cash.copy(isDefault = false)),
            quick = listOf(quick),
        )
        coEvery { dao.activeQuickTransactionsForAccount(1) } returns listOf(quick)

        val error = runCatching {
            repository.setAccountArchived(1, archived = true)
        }.exceptionOrNull()

        assertThat(error).isInstanceOf(IllegalArgumentException::class.java)
        assertThat(error).hasMessageThat().contains("one-tap")
    }
}
