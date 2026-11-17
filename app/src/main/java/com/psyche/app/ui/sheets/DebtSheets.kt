package com.psyche.app.ui.sheets

import android.app.DatePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.psyche.app.data.Account
import com.psyche.app.data.AccountType
import com.psyche.app.data.Category
import com.psyche.app.data.DebtOrigin
import com.psyche.app.data.DebtTerms
import com.psyche.app.data.InterestMethod
import com.psyche.app.ui.DebtDraft
import com.psyche.app.ui.FinanceViewModel
import com.psyche.app.ui.components.ChoiceChip
import com.psyche.app.ui.components.Hairline
import com.psyche.app.ui.components.MonoText
import com.psyche.app.ui.components.MoneyField
import com.psyche.app.ui.components.PrimaryAction
import com.psyche.app.ui.components.ProgressTrack
import com.psyche.app.ui.components.SecondaryAction
import com.psyche.app.ui.components.SheetHeader
import com.psyche.app.ui.components.StatCell
import com.psyche.app.ui.components.Stamp
import com.psyche.app.ui.theme.LocalPsychePalette
import com.psyche.app.ui.theme.PassType
import com.psyche.app.util.AccountBalance
import com.psyche.app.util.DebtMath
import com.psyche.app.util.DebtSummary
import com.psyche.app.util.Formats
import com.psyche.app.util.InstallmentStatus
import com.psyche.app.util.Money
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

@Composable
fun DebtEditorSheet(
    viewModel: FinanceViewModel,
    account: Account?,
    summary: DebtSummary?,
    liquidAccounts: List<Account>,
    existingAccounts: List<Account> = emptyList(),
    balances: List<AccountBalance> = emptyList(),
    categories: List<Category>,
    defaultReceivable: Boolean = false,
    onDismiss: () -> Unit,
) {
    val palette = LocalPsychePalette.current
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val editing = account != null
    val existingTerms = summary?.terms
    val directionReceivable = account?.type?.isReceivable ?: defaultReceivable

    var receivable by rememberSaveable { mutableStateOf(directionReceivable) }
    val initialType = account?.type
        ?: if (defaultReceivable) AccountType.RECEIVABLE else AccountType.LOAN
    var typeName by rememberSaveable {
        mutableStateOf(initialType.name)
    }
    var name by rememberSaveable { mutableStateOf(account?.name ?: "") }
    var counterparty by rememberSaveable {
        mutableStateOf(existingTerms?.counterparty ?: "")
    }
    var amount by rememberSaveable {
        mutableStateOf(
            when {
                account == null -> ""
                summary != null && summary.balanceCents > 0 -> Money.inputValue(summary.balanceCents)
                else -> ""
            },
        )
    }
    var originalPrincipal by rememberSaveable {
        mutableStateOf(
            existingTerms?.originalPrincipalCents
                ?.takeIf { it > 0 }
                ?.let { Money.inputValue(it) }
                ?: "",
        )
    }
    var rate by rememberSaveable {
        mutableStateOf(
            existingTerms?.annualRateBps
                ?.takeIf { it > 0 }
                ?.let { rateInput(it) }
                ?: "",
        )
    }
    var methodName by rememberSaveable {
        mutableStateOf(
            (existingTerms?.interestMethod
                ?: if (initialType == AccountType.BNPL) InterestMethod.ZERO else InterestMethod.REDUCING).name,
        )
    }
    var term by rememberSaveable {
        mutableStateOf(
            when {
                summary != null && summary.remainingCount > 0 -> summary.remainingCount.toString()
                existingTerms?.termMonths?.let { it > 0 } == true -> existingTerms.termMonths.toString()
                else -> ""
            },
        )
    }
    var firstDueAt by rememberSaveable {
        mutableStateOf(
            if (editing) {
                existingTerms?.firstDueAt?.takeIf { it > 0 } ?: nextMonthMillis()
            } else {
                nextMonthMillis()
            },
        )
    }
    var monthly by rememberSaveable { mutableStateOf("") }
    var paidInstallments by rememberSaveable { mutableStateOf("") }
    var downPayment by rememberSaveable { mutableStateOf("") }
    var existingAccountId by rememberSaveable { mutableStateOf<Long?>(null) }
    var originName by rememberSaveable {
        mutableStateOf(
            when {
                editing -> DebtOrigin.EXISTING.name
                directionReceivable -> DebtOrigin.LENT.name
                else -> DebtOrigin.EXISTING.name
            },
        )
    }
    var fundingId by rememberSaveable { mutableStateOf(liquidAccounts.firstOrNull()?.id ?: -1L) }
    var categoryId by rememberSaveable { mutableStateOf(0L) }
    var note by rememberSaveable { mutableStateOf("") }
    var scheduleTouched by rememberSaveable { mutableStateOf(false) }
    var firstDueTouched by rememberSaveable { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }

    val type = AccountType.valueOf(typeName)
    val origin = DebtOrigin.valueOf(originName)
    val method = InterestMethod.valueOf(methodName)
    val amountCents = Money.parseDebt(amount)
    val originalCents = when {
        originalPrincipal.isNotBlank() -> parseOptionalAmount(originalPrincipal)
        editing -> existingTerms?.originalPrincipalCents?.takeIf { it > 0 }
        else -> amountCents
    }
    val rateBps = parseRate(rate)
    val termMonths = term.trim().toIntOrNull()
    val needsSchedule = type.hasSchedule
    val needsTerms = needsSchedule || type.isReceivable
    val settled = editing && (summary?.balanceCents ?: 0L) == 0L
    val scheduleRequired = needsSchedule && !settled
    val funding = liquidAccounts.firstOrNull { it.id == fundingId }
    val category = categories.firstOrNull { it.id == categoryId }
    val inProgress = origin == DebtOrigin.IN_PROGRESS
    val monthlyCents = Money.parse(monthly)
    val paidCount = paidInstallments.trim().toIntOrNull()
    val downCents = parseOptionalAmount(downPayment)
    val existingOutstanding = balances
        .firstOrNull { it.account.id == existingAccountId }
        ?.outstandingCents
    val financed = if (
        inProgress &&
        monthlyCents != null &&
        termMonths != null &&
        termMonths > 0 &&
        rateBps != null
    ) {
        DebtMath.principalFromPayment(
            monthlyPaymentCents = monthlyCents,
            termMonths = termMonths,
            annualRateBps = if (method == InterestMethod.ZERO) 0 else rateBps,
            interestMethod = method,
        )
    } else {
        null
    }
    val previewTerms = if (financed != null && termMonths != null && firstDueAt > 0) {
        DebtTerms(
            accountId = -1,
            originalPrincipalCents = financed,
            annualRateBps = if (method == InterestMethod.ZERO) 0 else rateBps ?: 0,
            interestMethod = method,
            termMonths = termMonths,
            firstDueAt = firstDueAt,
            paymentsPerYear = 12,
            scheduleStartBalanceCents = financed,
            scheduleStartAt = todayMillis(),
            downPaymentCents = downCents ?: 0,
        )
    } else {
        null
    }
    val remaining = if (previewTerms != null && paidCount != null) {
        DebtMath.balanceAfter(previewTerms, paidCount)
    } else {
        null
    }
    val nextDuePreview = if (paidCount != null && firstDueAt > 0) {
        Instant.ofEpochMilli(firstDueAt)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .plusMonths(paidCount.toLong())
    } else {
        null
    }
    val purchaseTotal = (financed ?: 0L) + (downCents ?: 0L)

    val projection = if (
        !inProgress &&
        needsSchedule &&
        amountCents != null &&
        termMonths != null &&
        termMonths > 0 &&
        rateBps != null
    ) {
        projectionFor(
            type = type,
            amountCents = amountCents,
            originalCents = originalCents ?: amountCents,
            rateBps = rateBps,
            method = method,
            termMonths = termMonths,
            firstDueAt = firstDueAt,
        )
    } else {
        null
    }

    val valid = if (inProgress) {
        name.isNotBlank() &&
            needsSchedule &&
            monthlyCents != null &&
            termMonths != null &&
            termMonths in 1..600 &&
            paidCount != null &&
            paidCount in 0 until termMonths &&
            downCents != null &&
            firstDueAt > 0 &&
            rateBps != null &&
            financed != null &&
            financed > 0 &&
            remaining != null &&
            remaining > 0 &&
            (
                existingAccountId == null ||
                    (
                        existingOutstanding != null &&
                            existingOutstanding > 0 &&
                            existingOutstanding == remaining
                        )
                )
    } else {
        name.isNotBlank() &&
            (editing || amountCents != null) &&
            rateBps != null &&
            (!scheduleRequired || (termMonths != null && termMonths in 1..600 && firstDueAt > 0)) &&
            (origin != DebtOrigin.BORROWED || funding != null) &&
            (origin != DebtOrigin.LENT || funding != null) &&
            (origin != DebtOrigin.PURCHASE || type == AccountType.BNPL)
    }

    SheetFrame(onDismiss = onDismiss) {
        SheetHeader(
            eyebrow = if (editing) "Debt terms" else "New obligation",
            title = when {
                editing -> "EDIT DEBT"
                receivable -> "ADD RECEIVABLE"
                origin == DebtOrigin.IN_PROGRESS -> "ADD PLAN"
                else -> "ADD DEBT"
            },
            onClose = onDismiss,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = when {
                editing ->
                    "Changing the schedule projects forward from today's balance. Past payments stay in the record."

                origin == DebtOrigin.IN_PROGRESS ->
                    "Enter the plan you are already paying. The app computes the financed amount and remaining balance. No past payment is posted again."

                else ->
                    "Only principal counts in net. Interest is projected from the terms and recorded when you pay."
            },
            style = MaterialTheme.typography.bodySmall,
            color = palette.muted,
        )

        if (!editing) {
            Spacer(Modifier.height(14.dp))
            MonoText(
                text = "DIRECTION",
                color = palette.faint,
                style = PassType.monoLabel.copy(letterSpacing = 1.1.sp),
            )
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ChoiceChip(
                    label = "I owe",
                    selected = !receivable,
                    onClick = {
                        receivable = false
                        if (type.isReceivable) typeName = AccountType.LOAN.name
                        if (origin == DebtOrigin.LENT) originName = DebtOrigin.BORROWED.name
                        error = null
                    },
                )
                ChoiceChip(
                    label = "Owed to me",
                    selected = receivable,
                    onClick = {
                        receivable = true
                        typeName = AccountType.RECEIVABLE.name
                        originName = DebtOrigin.LENT.name
                        error = null
                    },
                )
            }
            Spacer(Modifier.height(12.dp))
            MonoText(
                text = "KIND",
                color = palette.faint,
                style = PassType.monoLabel.copy(letterSpacing = 1.1.sp),
            )
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                (if (receivable) {
                    listOf(AccountType.RECEIVABLE)
                } else {
                    listOf(AccountType.LOAN, AccountType.BNPL, AccountType.INFORMAL_DEBT)
                }).forEach { option ->
                    ChoiceChip(
                        label = option.label,
                        selected = type == option,
                        onClick = {
                            typeName = option.name
                            if (option == AccountType.BNPL && origin == DebtOrigin.BORROWED) {
                                originName = DebtOrigin.EXISTING.name
                            }
                            if (option != AccountType.BNPL && origin == DebtOrigin.PURCHASE) {
                                originName = DebtOrigin.EXISTING.name
                            }
                            if (option == AccountType.INFORMAL_DEBT &&
                                (origin == DebtOrigin.IN_PROGRESS || origin == DebtOrigin.PURCHASE)
                            ) {
                                originName = DebtOrigin.EXISTING.name
                            }
                            if (!scheduleTouched && rate.isBlank()) {
                                methodName = if (option == AccountType.BNPL) {
                                    InterestMethod.ZERO.name
                                } else {
                                    InterestMethod.REDUCING.name
                                }
                            }
                            error = null
                        },
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            MonoText(
                text = when (type) {
                    AccountType.BNPL ->
                        "INSTALLMENT COVERS STORE FINANCING, BNPL, AND PHONE OR APPLIANCE PLANS."

                    AccountType.LOAN -> "A BANK, SALARY, CAR, OR OTHER LOAN WITH A SCHEDULE."
                    AccountType.INFORMAL_DEBT -> "UTANG WITH A BALANCE AND OPTIONALLY A LENDER."
                    AccountType.RECEIVABLE -> "MONEY YOU LENT THAT SHOULD COME BACK."
                    else -> ""
                },
                color = palette.faint,
                style = PassType.monoMicro.copy(fontSize = 7.sp),
                maxLines = 2,
            )
        }

        Spacer(Modifier.height(14.dp))
        OutlinedTextField(
            value = name,
            onValueChange = { name = it.take(40) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Name", style = MaterialTheme.typography.bodySmall) },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium,
            colors = debtFieldColors(),
        )
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = counterparty,
            onValueChange = { counterparty = it.take(40) },
            modifier = Modifier.fillMaxWidth(),
            label = {
                Text(
                    if (receivable) "Borrower (optional)" else "Lender (optional)",
                    style = MaterialTheme.typography.bodySmall,
                )
            },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium,
            colors = debtFieldColors(),
        )

        if (!editing) {
            Spacer(Modifier.height(14.dp))
            MonoText(
                text = "STARTING POINT",
                color = palette.faint,
                style = PassType.monoLabel.copy(letterSpacing = 1.1.sp),
            )
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                val origins = when {
                    receivable -> listOf(DebtOrigin.EXISTING, DebtOrigin.LENT)
                    type == AccountType.BNPL ->
                        listOf(DebtOrigin.IN_PROGRESS, DebtOrigin.PURCHASE, DebtOrigin.EXISTING)

                    type == AccountType.LOAN ->
                        listOf(DebtOrigin.IN_PROGRESS, DebtOrigin.EXISTING, DebtOrigin.BORROWED)

                    else -> listOf(DebtOrigin.EXISTING, DebtOrigin.BORROWED)
                }
                origins.forEach { option ->
                    ChoiceChip(
                        label = option.label,
                        selected = origin == option,
                        onClick = {
                            originName = option.name
                            if (option != DebtOrigin.IN_PROGRESS) {
                                existingAccountId = null
                            }
                            error = null
                        },
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            MonoText(
                text = when (origin) {
                    DebtOrigin.IN_PROGRESS ->
                        "ENTER THE PLAN YOU ARE ALREADY PAYING. THE APP COMPUTES THE FINANCED AMOUNT AND REMAINING BALANCE."

                    DebtOrigin.EXISTING ->
                        "ENTER THE PRINCIPAL STILL OWED TODAY. NO PURCHASE OR PAST PAYMENT IS ADDED."

                    DebtOrigin.PURCHASE ->
                        "RECORD A NEW PURCHASE AND ITS INSTALLMENT IN ONE STEP."

                    DebtOrigin.BORROWED ->
                        "THE LOAN PROCEEDS LAND IN A CASH OR BANK ACCOUNT."

                    DebtOrigin.LENT ->
                        "THE CASH LEAVES A CASH OR BANK ACCOUNT AND BECOMES MONEY OWED TO YOU."
                },
                color = palette.faint,
                style = PassType.monoMicro.copy(fontSize = 7.sp),
                maxLines = 3,
            )
        }

        Spacer(Modifier.height(12.dp))
        if (inProgress && !editing) {
            MoneyField(
                label = "Monthly payment",
                value = monthly,
                onValueChange = {
                    monthly = it
                    error = null
                },
                supporting = "The amount you actually pay each month.",
                error = if (monthly.isNotBlank() && monthlyCents == null) {
                    "Use a valid amount."
                } else {
                    null
                },
            )
            Spacer(Modifier.height(10.dp))
            NumberField(
                label = "Payments already made",
                value = paidInstallments,
                onValueChange = {
                    paidInstallments = it.filter(Char::isDigit).take(3)
                    error = null
                },
                modifier = Modifier.fillMaxWidth(),
                supporting = "How many monthly installments you have already paid. A count, not a peso amount.",
                error = when {
                    paidInstallments.isBlank() -> null
                    paidCount == null -> "Enter a number."
                    termMonths != null && paidCount !in 0 until termMonths ->
                        "Must be 0 to ${termMonths - 1}."

                    else -> null
                },
            )
            Spacer(Modifier.height(10.dp))
            MoneyField(
                label = "Downpayment (optional)",
                value = downPayment,
                onValueChange = {
                    downPayment = it
                    error = null
                },
                supporting = "Paid at purchase. It is not part of the financed balance.",
                error = if (downPayment.isNotBlank() && downCents == null) {
                    "Use a valid amount."
                } else {
                    null
                },
            )
            if (existingAccounts.isNotEmpty()) {
                Spacer(Modifier.height(14.dp))
                MonoText(
                    text = "ALREADY TRACKED AS",
                    color = palette.faint,
                    style = PassType.monoLabel.copy(letterSpacing = 1.1.sp),
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    ChoiceChip(
                        label = "New account",
                        selected = existingAccountId == null,
                        onClick = {
                            existingAccountId = null
                            error = null
                        },
                    )
                    existingAccounts.forEach { option ->
                        ChoiceChip(
                            label = option.name,
                            selected = existingAccountId == option.id,
                            onClick = {
                                existingAccountId = option.id
                                if (name.isBlank()) name = option.name
                                error = null
                            },
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                MonoText(
                    text = if (existingAccountId == null) {
                        "A NEW DEBT ACCOUNT WILL BE CREATED. IF THIS PLAN IS ALREADY IN ACCOUNTS, SELECT IT TO AVOID DOUBLE COUNTING."
                    } else {
                        "THE SELECTED ACCOUNT IS CONVERTED AND ITS CURRENT BALANCE IS USED. NO NEW DEBT IS COUNTED."
                    },
                    color = palette.faint,
                    style = PassType.monoMicro.copy(fontSize = 7.sp),
                    maxLines = 2,
                )
                if (existingAccountId != null && existingOutstanding != null && remaining != null &&
                    existingOutstanding != remaining
                ) {
                    Spacer(Modifier.height(4.dp))
                    MonoText(
                        text = "ACCOUNT BALANCE ${Money.formatPeso(existingOutstanding)} \u00B7 " +
                            "PLAN REMAINING ${Money.formatPeso(remaining)}",
                        color = palette.danger,
                        style = PassType.monoMicro.copy(fontSize = 7.sp),
                        maxLines = 2,
                    )
                }
            }
        } else {
            MoneyField(
                label = when {
                    editing -> "Current balance"
                    origin == DebtOrigin.EXISTING && receivable -> "Amount owed to you"
                    origin == DebtOrigin.EXISTING -> "Current balance"
                    origin == DebtOrigin.PURCHASE -> "Purchase amount"
                    else -> "Principal"
                },
                value = amount,
                onValueChange = {
                    amount = it
                    if (editing) error = null
                },
                supporting = when {
                    editing -> "Record payments instead of editing this balance."
                    origin == DebtOrigin.EXISTING ->
                        "The principal still owed today, not a payoff quote that includes future interest."

                    origin == DebtOrigin.PURCHASE ->
                        "The purchase is recorded as an expense from this installment account."

                    origin == DebtOrigin.BORROWED ->
                        "The cash lands in the account you choose below."

                    else -> "The cash leaves the account you choose below."
                },
                enabled = !editing,
                error = if (amount.isNotBlank() && amountCents == null) "Use a valid amount." else null,
            )
        }

        if (!editing && (origin == DebtOrigin.BORROWED || origin == DebtOrigin.LENT)) {
            Spacer(Modifier.height(10.dp))
            MonoText(
                text = if (origin == DebtOrigin.BORROWED) "DEPOSIT INTO" else "PAID FROM",
                color = palette.faint,
                style = PassType.monoLabel.copy(letterSpacing = 1.1.sp),
            )
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                liquidAccounts.forEach { option ->
                    ChoiceChip(
                        label = option.name,
                        selected = option.id == fundingId,
                        onClick = {
                            fundingId = option.id
                            error = null
                        },
                    )
                }
            }
            if (liquidAccounts.isEmpty()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Add a cash or bank account first.",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.danger,
                )
            }
        }

        if (!editing && origin == DebtOrigin.PURCHASE) {
            Spacer(Modifier.height(10.dp))
            MonoText(
                text = "CATEGORY",
                color = palette.faint,
                style = PassType.monoLabel.copy(letterSpacing = 1.1.sp),
            )
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                ChoiceChip(
                    label = "Uncategorized",
                    selected = categoryId == 0L,
                    onClick = { categoryId = 0L },
                )
                categories.forEach { option ->
                    ChoiceChip(
                        label = option.name,
                        selected = option.id == categoryId,
                        onClick = { categoryId = option.id },
                    )
                }
            }
        }

        if (needsSchedule || (editing && existingTerms?.termMonths?.let { it > 0 } == true)) {
            Spacer(Modifier.height(20.dp))
            Hairline(color = palette.boardLine)
            Spacer(Modifier.height(14.dp))
            MonoText(
                text = "SCHEDULE",
                color = palette.faint,
                style = PassType.monoLabel.copy(letterSpacing = 1.2.sp),
            )
            Spacer(Modifier.height(8.dp))
            if (!inProgress) {
                MoneyField(
                    label = "Original principal (optional)",
                    value = originalPrincipal,
                    onValueChange = { originalPrincipal = it },
                    supporting = "Used for payoff progress. Leave blank to use the starting amount.",
                    error = if (originalPrincipal.isNotBlank() && originalCents == null) {
                        "Use a valid amount."
                    } else {
                        null
                    },
                )
                Spacer(Modifier.height(10.dp))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField(
                    label = "Annual rate %",
                    value = rate,
                    onValueChange = {
                        rate = it
                        scheduleTouched = true
                        error = null
                    },
                    modifier = Modifier.weight(1f),
                    supporting = "Leave blank for 0%.",
                    decimal = true,
                    error = if (rate.isNotBlank() && rateBps == null) "Invalid" else null,
                )
                NumberField(
                    label = "Term (months)",
                    value = term,
                    onValueChange = {
                        term = it.filter(Char::isDigit).take(3)
                        scheduleTouched = true
                        error = null
                    },
                    modifier = Modifier.weight(1f),
                    supporting = "Total monthly installments.",
                    error = if (term.isNotBlank() && (termMonths == null || termMonths !in 1..600)) {
                        "1–600"
                    } else {
                        null
                    },
                )
            }
            Spacer(Modifier.height(10.dp))
            MonoText(
                text = "RATE IS ANNUAL. 1.5% PER MONTH = 18% PER YEAR.",
                color = palette.faint,
                style = PassType.monoMicro.copy(fontSize = 7.sp),
            )
            Spacer(Modifier.height(10.dp))
            MonoText(
                text = "INTEREST METHOD",
                color = palette.faint,
                style = PassType.monoLabel.copy(letterSpacing = 1.1.sp),
            )
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                InterestMethod.entries.forEach { option ->
                    ChoiceChip(
                        label = option.label,
                        selected = method == option,
                        onClick = {
                            methodName = option.name
                            scheduleTouched = true
                            if (option == InterestMethod.ZERO) rate = ""
                            error = null
                        },
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            SecondaryAction(
                text = if (inProgress) {
                    "First installment \u00B7 ${Formats.fullDate(firstDueAt)}"
                } else {
                    "First due \u00B7 ${Formats.fullDate(firstDueAt)}"
                },
                onClick = {
                    val zone = ZoneId.systemDefault()
                    val current = Instant.ofEpochMilli(firstDueAt).atZone(zone)
                    val picker = DatePickerDialog(
                        context,
                        { _, year, month, day ->
                            firstDueAt = LocalDate.of(year, month + 1, day)
                                .atStartOfDay(zone)
                                .toInstant()
                                .toEpochMilli()
                            firstDueTouched = true
                        },
                        current.year,
                        current.monthValue - 1,
                        current.dayOfMonth,
                    )
                    picker.show()
                },
                modifier = Modifier.fillMaxWidth(),
            )
            if (editing && firstDueTouched) {
                Spacer(Modifier.height(4.dp))
                MonoText(
                    text = "ONLY THE DATES MOVE. THE PAID INSTALLMENTS STAY PAID.",
                    color = palette.faint,
                    style = PassType.monoMicro.copy(fontSize = 7.sp),
                )
            }
            if (inProgress) {
                Spacer(Modifier.height(4.dp))
                MonoText(
                    text = "SET THE DATE OF PAYMENT 1, EVEN IF IT IS IN THE PAST.",
                    color = palette.faint,
                    style = PassType.monoMicro.copy(fontSize = 7.sp),
                )
            }
            if (inProgress && financed != null && remaining != null) {
                Spacer(Modifier.height(10.dp))
                InstallmentPlanCard(
                    financedCents = financed,
                    paidCents = (financed - remaining).coerceAtLeast(0),
                    remainingCents = remaining,
                    nextDue = nextDuePreview,
                    downPaymentCents = downCents ?: 0,
                    purchaseTotalCents = purchaseTotal,
                    paidCount = paidCount ?: 0,
                    termMonths = termMonths ?: 0,
                )
            }
            if (projection != null && (!editing || scheduleTouched || firstDueTouched)) {
                Spacer(Modifier.height(10.dp))
                ProjectionCard(
                    installmentCents = projection.installmentCents,
                    interestCents = projection.interestToComeCents,
                    payoffAt = projection.projectedPayoffAt,
                    method = method,
                )
            }
        }

        if (editing) {
            Spacer(Modifier.height(12.dp))
            MonoText(
                text = "SAVED PAYMENTS STAY. THE NEW TERMS PROJECT FROM TODAY'S BALANCE.",
                color = palette.faint,
                style = PassType.monoMicro.copy(fontSize = 7.sp),
                maxLines = 2,
            )
        }

        Spacer(Modifier.height(12.dp))
        if (!editing && !inProgress) {
            DebtNoteField(
                value = note,
                onValueChange = { note = it.take(80) },
                label = "Note for the record (optional)",
            )
        }
        if (error != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = error ?: "",
                style = PassType.monoDataSmall,
                color = palette.danger,
            )
        }
        Spacer(Modifier.height(18.dp))
        PrimaryAction(
            text = when {
                saving -> "Saving\u2026"
                editing && receivable -> "Save receivable"
                editing -> "Save debt"
                receivable -> "Add receivable"
                origin == DebtOrigin.IN_PROGRESS -> "Add plan"
                else -> "Add debt"
            },
            onClick = {
                val cents = amountCents
                if (!editing && !inProgress && cents == null) {
                    error = "Enter an amount."
                    return@PrimaryAction
                }
                if (name.isBlank()) {
                    error = "Name the debt."
                    return@PrimaryAction
                }
                if (scheduleRequired && (termMonths == null || termMonths !in 1..600)) {
                    error = "Enter a term between 1 and 600 months."
                    return@PrimaryAction
                }
                if (scheduleRequired && rateBps == null) {
                    error = "Enter a valid annual rate."
                    return@PrimaryAction
                }
                if (inProgress) {
                    if (monthlyCents == null) {
                        error = "Enter the monthly payment."
                        return@PrimaryAction
                    }
                    if (paidCount == null || termMonths == null || paidCount !in 0 until termMonths) {
                        error = "Enter how many payments you have made."
                        return@PrimaryAction
                    }
                    if (downCents == null) {
                        error = "Use a valid downpayment, or leave it blank."
                        return@PrimaryAction
                    }
                    if (financed == null || remaining == null || remaining <= 0L) {
                        error = "Check the monthly payment and term."
                        return@PrimaryAction
                    }
                }
                if (origin == DebtOrigin.BORROWED || origin == DebtOrigin.LENT) {
                    if (funding == null) {
                        error = "Choose an account for the money."
                        return@PrimaryAction
                    }
                }
                saving = true
                error = null
                scope.launch {
                    val result = if (editing) {
                        val current = account
                        val terms = when {
                            existingTerms != null && !scheduleTouched -> existingTerms.copy(
                                originalPrincipalCents = originalCents
                                    ?: existingTerms.originalPrincipalCents,
                                firstDueAt = if (firstDueTouched) firstDueAt else existingTerms.firstDueAt,
                                counterparty = counterparty.trim().ifBlank { null },
                            )

                            needsTerms -> DebtTerms(
                                accountId = current.id,
                                originalPrincipalCents = originalCents
                                    ?: existingTerms?.originalPrincipalCents
                                    ?: summary?.balanceCents
                                    ?: 0L,
                                annualRateBps = if (method == InterestMethod.ZERO) 0 else rateBps ?: 0,
                                interestMethod = method,
                                termMonths = if (type.hasSchedule) termMonths ?: 0 else 0,
                                firstDueAt = firstDueAt,
                                paymentsPerYear = 12,
                                scheduleStartBalanceCents = summary?.balanceCents ?: 0L,
                                scheduleStartAt = System.currentTimeMillis(),
                                counterparty = counterparty.trim().ifBlank { null },
                                downPaymentCents = existingTerms?.downPaymentCents ?: 0,
                            )

                            else -> null
                        }
                        viewModel.updateDebt(
                            current.copy(name = name.trim()),
                            terms,
                        )
                    } else {
                        val createCents = if (inProgress) requireNotNull(remaining) else requireNotNull(cents)
                        viewModel.createDebt(
                            DebtDraft(
                                name = name.trim(),
                                type = type,
                                amountCents = createCents,
                                originalPrincipalCents = if (inProgress) {
                                    requireNotNull(financed)
                                } else {
                                    originalCents ?: createCents
                                },
                                scheduleStartBalanceCents = if (inProgress) {
                                    requireNotNull(financed)
                                } else {
                                    0L
                                },
                                downPaymentCents = if (inProgress) downCents ?: 0L else 0L,
                                annualRateBps = if (method == InterestMethod.ZERO) 0 else rateBps ?: 0,
                                interestMethod = method,
                                termMonths = if (type.hasSchedule) termMonths ?: 0 else 0,
                                firstDueAt = firstDueAt,
                                counterparty = counterparty.trim().ifBlank { null },
                                origin = origin,
                                fundingAccountId = funding?.id,
                                existingAccountId = if (inProgress) existingAccountId else null,
                                categoryId = categoryId.takeIf { it > 0 },
                                note = note.trim(),
                                occurredAt = todayMillis(),
                            ),
                        )
                    }
                    result.onSuccess { onDismiss() }
                        .onFailure { error = it.message ?: "Couldn't save the debt." }
                    saving = false
                }
            },
            enabled = !saving && valid,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(6.dp))
        MonoText(
            text = "INTEREST IS NEVER PRE-FILLED INTO OWED. ONLY PRINCIPAL MOVES THE PASS.",
            color = palette.faint,
            style = PassType.monoMicro.copy(fontSize = 7.sp),
            maxLines = 2,
        )
    }
}

@Composable
fun DebtPaymentSheet(
    viewModel: FinanceViewModel,
    summary: DebtSummary,
    liquidAccounts: List<Account>,
    balances: List<AccountBalance>,
    lastAccountId: Long?,
    collect: Boolean = false,
    onDismiss: () -> Unit,
) {
    val palette = LocalPsychePalette.current
    val scope = rememberCoroutineScope()
    val next = summary.installments.firstOrNull {
        it.status == InstallmentStatus.DUE ||
            it.status == InstallmentStatus.OVERDUE ||
            it.status == InstallmentStatus.UPCOMING
    }
    var total by rememberSaveable {
        mutableStateOf(
            when {
                collect -> summary.balanceCents.takeIf { it > 0 }?.let { Money.inputValue(it) } ?: ""
                next != null -> Money.inputValue(next.paymentCents)
                else -> ""
            },
        )
    }
    var interest by rememberSaveable {
        mutableStateOf(next?.interestCents?.takeIf { it > 0 }?.let { Money.inputValue(it) } ?: "")
    }
    var accountId by rememberSaveable {
        mutableStateOf(
            lastAccountId?.takeIf { id -> liquidAccounts.any { it.id == id } }
                ?: liquidAccounts.firstOrNull()?.id
                ?: -1L,
        )
    }
    var note by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }

    val totalCents = Money.parse(total)
    val interestCents = parseOptionalAmount(interest)
    val principalCents = totalCents?.let { (it - (interestCents ?: 0L)).coerceAtLeast(0L) }
    val account = liquidAccounts.firstOrNull { it.id == accountId }
    val available = balances.firstOrNull { it.account.id == accountId }?.availableToMoveCents ?: 0L
    val amountError = when {
        error != null -> error
        total.isNotBlank() && totalCents == null -> "Use a valid amount."
        interest.isNotBlank() && interestCents == null -> "Use a valid interest amount."
        totalCents != null && interestCents != null && interestCents > totalCents ->
            "Interest cannot exceed the total paid."

        !collect && principalCents != null && principalCents > summary.balanceCents ->
            "Only ${Money.formatPeso(summary.balanceCents)} is outstanding."

        totalCents != null && account != null && totalCents > available ->
            "Only ${Money.formatPeso(available)} is available in ${account.name}."

        else -> null
    }
    val canSave = account != null &&
        totalCents != null &&
        totalCents <= available &&
        interestCents != null &&
        interestCents <= totalCents &&
        (collect || principalCents == null || principalCents <= summary.balanceCents)

    SheetFrame(onDismiss = onDismiss) {
        SheetHeader(
            eyebrow = if (collect) "Money owed to you" else "Debt payment",
            title = if (collect) "COLLECT" else "PAY DEBT",
            onClose = onDismiss,
        )
        Spacer(Modifier.height(10.dp))
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = palette.boardRow,
            border = BorderStroke(1.dp, palette.boardLine),
        ) {
            Column(Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = summary.account.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.onGround,
                        modifier = Modifier.weight(1f),
                    )
                    Stamp(
                        text = summary.account.type.shortLabel,
                        color = palette.faint,
                        borderColor = palette.boardLine,
                    )
                }
                Spacer(Modifier.height(4.dp))
                MonoText(
                    text = "${Money.formatPeso(summary.balanceCents)} " +
                        if (collect) "TO COLLECT" else "OUTSTANDING",
                    color = if (collect) palette.accentText else palette.danger,
                    style = PassType.monoDataSmall,
                )
                if (!collect && next != null) {
                    Spacer(Modifier.height(8.dp))
                    ProgressTrack(
                        fraction = if (summary.installments.isEmpty()) 0f else {
                            summary.paidCount.toFloat() / summary.installments.size.toFloat()
                        },
                        color = palette.alert,
                        track = palette.panelLine,
                        height = 4.dp,
                    )
                    Spacer(Modifier.height(4.dp))
                    MonoText(
                        text = "NEXT ${Formats.shortDate(next.dueAt)} \u00B7 " +
                            "PRINCIPAL ${Money.formatPeso(next.principalCents)} \u00B7 " +
                            "INTEREST ${Money.formatPeso(next.interestCents)}",
                        color = palette.faint,
                        style = PassType.monoMicro.copy(fontSize = 7.sp),
                    )
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        MoneyField(
            label = if (collect) "Amount collected" else "Total paid",
            value = total,
            onValueChange = {
                total = it
                error = null
            },
            error = amountError,
            supporting = if (!collect) {
                "Cash out includes principal plus interest."
            } else {
                "This lands in the account below and leaves the receivable."
            },
        )
        if (!collect) {
            Spacer(Modifier.height(10.dp))
            MoneyField(
                label = "Interest portion (optional)",
                value = interest,
                onValueChange = {
                    interest = it
                    error = null
                },
                supporting = if (principalCents != null) {
                    "Principal ${Money.formatPeso(principalCents)} \u00B7 Interest ${Money.formatPeso(interestCents ?: 0L)}"
                } else {
                    "Leave blank for a principal-only payment."
                },
                error = if (interest.isNotBlank() && interestCents == null) "Use a valid amount." else null,
            )
            if (principalCents != null && interestCents != null && interestCents > 0) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = if (principalCents == 0L) {
                        "Interest-only: this lowers net but does not advance the schedule."
                    } else {
                        "Only the interest lowers net. The principal is a transfer."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.muted,
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        MonoText(
            text = if (collect) "DEPOSIT INTO" else "PAY FROM",
            color = palette.faint,
            style = PassType.monoLabel.copy(letterSpacing = 1.1.sp),
        )
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            liquidAccounts.forEach { option ->
                ChoiceChip(
                    label = option.name,
                    selected = option.id == accountId,
                    onClick = {
                        accountId = option.id
                        error = null
                    },
                )
            }
        }
        if (liquidAccounts.isEmpty()) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Add a cash or bank account first.",
                style = MaterialTheme.typography.bodySmall,
                color = palette.danger,
            )
        }
        Spacer(Modifier.height(12.dp))
        DebtNoteField(
            value = note,
            onValueChange = { note = it.take(80) },
            label = "Note (optional)",
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = if (collect) {
                "Collecting moves money from the receivable into the account. It raises liquid money and is excluded from owed."
            } else {
                "Principal reduces the debt; interest is an expense. The pass moves only by the interest."
            },
            style = MaterialTheme.typography.bodySmall,
            color = palette.muted,
        )
        if (error != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = error ?: "",
                style = PassType.monoDataSmall,
                color = palette.danger,
            )
        }
        Spacer(Modifier.height(18.dp))
        PrimaryAction(
            text = when {
                saving -> "Saving\u2026"
                collect -> "Collect"
                else -> "Record payment"
            },
            onClick = {
                val cents = totalCents
                if (cents == null) {
                    error = "Enter an amount."
                    return@PrimaryAction
                }
                if (account == null) {
                    error = "Choose an account."
                    return@PrimaryAction
                }
                if (cents > available) {
                    error = "Only ${Money.formatPeso(available)} is available in ${account.name}."
                    return@PrimaryAction
                }
                saving = true
                error = null
                scope.launch {
                    val result = if (collect) {
                        viewModel.recordTransfer(
                            amountCents = cents,
                            fromAccountId = summary.account.id,
                            toAccountId = account.id,
                            goalId = null,
                            note = note.trim().ifBlank { "Collected ${summary.account.name}" },
                        )
                    } else {
                        val interestValue = interestCents ?: 0L
                        val principal = (cents - interestValue).coerceAtLeast(0L)
                        viewModel.recordDebtPayment(
                            debtAccountId = summary.account.id,
                            fromAccountId = account.id,
                            principalCents = principal,
                            interestCents = interestValue,
                            note = note.trim().ifBlank { "Payment to ${summary.account.name}" },
                        )
                    }
                    result.onSuccess { onDismiss() }
                        .onFailure { error = it.message ?: "Couldn't record the payment." }
                    saving = false
                }
            },
            enabled = !saving && canSave,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun InstallmentPlanCard(
    financedCents: Long,
    paidCents: Long,
    remainingCents: Long,
    nextDue: LocalDate?,
    downPaymentCents: Long,
    purchaseTotalCents: Long,
    paidCount: Int,
    termMonths: Int,
) {
    val palette = LocalPsychePalette.current
    val fraction = if (financedCents > 0) {
        (paidCents.toFloat() / financedCents.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = palette.boardRow,
        border = BorderStroke(1.dp, palette.boardLine),
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCell(
                    label = "FINANCED",
                    value = Money.formatPeso(financedCents),
                    valueStyle = PassType.monoData.copy(fontSize = 13.sp, fontWeight = FontWeight.Medium),
                    modifier = Modifier.weight(1f),
                )
                StatCell(
                    label = "REMAINING",
                    value = Money.formatPeso(remainingCents),
                    valueColor = palette.danger,
                    valueStyle = PassType.monoData.copy(fontSize = 13.sp, fontWeight = FontWeight.Medium),
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(10.dp))
            ProgressTrack(
                fraction = fraction,
                color = palette.alert,
                track = palette.panelLine,
                height = 4.dp,
            )
            Spacer(Modifier.height(4.dp))
            Row {
                MonoText(
                    text = "$paidCount OF $termMonths PAYMENTS MADE",
                    color = palette.faint,
                    style = PassType.monoMicro.copy(fontSize = 7.sp),
                )
                Spacer(Modifier.weight(1f))
                MonoText(
                    text = "${(fraction * 100).toInt()}% PAID",
                    color = palette.accentText,
                    style = PassType.monoMicro.copy(fontSize = 7.sp),
                )
            }
            Spacer(Modifier.height(8.dp))
            Row {
                Column(Modifier.weight(1f)) {
                    MonoText(
                        text = "PAID TO DATE",
                        color = palette.faint,
                        style = PassType.monoMicro.copy(fontSize = 7.sp),
                    )
                    Text(
                        text = Money.formatPeso(paidCents),
                        style = PassType.monoData.copy(fontSize = 13.sp),
                        color = palette.onGround,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    MonoText(
                        text = "NEXT DUE",
                        color = palette.faint,
                        style = PassType.monoMicro.copy(fontSize = 7.sp),
                    )
                    Text(
                        text = nextDue?.let { Formats.fullDate(it) } ?: "\u2014",
                        style = PassType.monoData.copy(fontSize = 13.sp),
                        color = palette.onGround,
                    )
                }
            }
            if (downPaymentCents > 0) {
                Spacer(Modifier.height(8.dp))
                Row {
                    Column(Modifier.weight(1f)) {
                        MonoText(
                            text = "DOWNPAYMENT",
                            color = palette.faint,
                            style = PassType.monoMicro.copy(fontSize = 7.sp),
                        )
                        Text(
                            text = Money.formatPeso(downPaymentCents),
                            style = PassType.monoData.copy(fontSize = 13.sp),
                            color = palette.onGround,
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        MonoText(
                            text = "PURCHASE TOTAL",
                            color = palette.faint,
                            style = PassType.monoMicro.copy(fontSize = 7.sp),
                        )
                        Text(
                            text = Money.formatPeso(purchaseTotalCents),
                            style = PassType.monoData.copy(fontSize = 13.sp),
                            color = palette.onGround,
                        )
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            MonoText(
                text = "THE APP COMPUTES THE BALANCE FROM THE PLAN. NO PAST PAYMENTS ARE POSTED AGAIN.",
                color = palette.faint,
                style = PassType.monoMicro.copy(fontSize = 7.sp),
                maxLines = 2,
            )
        }
    }
}

@Composable
private fun ProjectionCard(
    installmentCents: Long,
    interestCents: Long,
    payoffAt: Long?,
    method: InterestMethod,
) {
    val palette = LocalPsychePalette.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = palette.boardRow,
        border = BorderStroke(1.dp, palette.boardLine),
    ) {
        Column(Modifier.padding(12.dp)) {
            Row {
                Column(Modifier.weight(1f)) {
                    MonoText(
                        text = "PROJECTED INSTALLMENT",
                        color = palette.faint,
                        style = PassType.monoMicro.copy(fontSize = 7.sp),
                    )
                    Text(
                        text = Money.formatPeso(installmentCents),
                        style = PassType.monoData.copy(fontSize = 16.sp),
                        color = palette.onGround,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    MonoText(
                        text = "INTEREST TO COME",
                        color = palette.faint,
                        style = PassType.monoMicro.copy(fontSize = 7.sp),
                    )
                    Text(
                        text = Money.formatPeso(interestCents),
                        style = PassType.monoData.copy(fontSize = 16.sp),
                        color = palette.muted,
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            MonoText(
                text = buildString {
                    append(method.label.uppercase())
                    payoffAt?.let {
                        append(" \u00B7 PAYOFF ")
                        append(Formats.fullDate(it))
                    }
                },
                color = palette.faint,
                style = PassType.monoMicro.copy(fontSize = 7.sp),
            )
        }
    }
}

@Composable
private fun NumberField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    error: String? = null,
    supporting: String? = null,
    decimal: Boolean = false,
) {
    val palette = LocalPsychePalette.current
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = { Text(label, style = MaterialTheme.typography.bodySmall) },
        singleLine = true,
        isError = error != null,
        supportingText = when {
            error != null -> {
                { Text(error, style = PassType.monoDataSmall, color = palette.danger) }
            }

            supporting != null -> {
                { Text(supporting, style = PassType.monoDataSmall, color = palette.muted) }
            }

            else -> null
        },
        textStyle = PassType.monoData.copy(fontSize = 15.sp),
        keyboardOptions = KeyboardOptions(
            keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number,
        ),
        colors = debtFieldColors(),
    )
}

@Composable
private fun DebtNoteField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
) {
    val palette = LocalPsychePalette.current
    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MonoText(
                text = label.uppercase(),
                color = palette.faint,
                style = PassType.monoLabel.copy(letterSpacing = 1.1.sp),
            )
            Spacer(Modifier.weight(1f))
            MonoText(
                text = "${value.length}/80",
                color = palette.faint,
                style = PassType.monoMicro.copy(fontSize = 7.sp),
            )
        }
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text("Optional", style = MaterialTheme.typography.bodySmall)
            },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium,
            colors = debtFieldColors(),
        )
    }
}

@Composable
private fun debtFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = LocalPsychePalette.current.onGround,
    unfocusedTextColor = LocalPsychePalette.current.onGround,
    cursorColor = LocalPsychePalette.current.onGround,
    focusedBorderColor = LocalPsychePalette.current.onGround,
    unfocusedBorderColor = LocalPsychePalette.current.panelLine,
    errorBorderColor = LocalPsychePalette.current.danger,
    focusedLabelColor = LocalPsychePalette.current.muted,
    unfocusedLabelColor = LocalPsychePalette.current.muted,
    errorLabelColor = LocalPsychePalette.current.danger,
    focusedContainerColor = Color.Transparent,
    unfocusedContainerColor = Color.Transparent,
    errorContainerColor = Color.Transparent,
)

private fun projectionFor(
    type: AccountType,
    amountCents: Long,
    originalCents: Long,
    rateBps: Int,
    method: InterestMethod,
    termMonths: Int,
    firstDueAt: Long,
): DebtSummary {
    val account = Account(
        id = -1,
        name = "Preview",
        type = type,
        openingBalanceCents = -amountCents,
        creditLimitCents = 0,
        lastFour = null,
        sortOrder = 0,
        archived = false,
    )
    return DebtMath.summarize(
        account = account,
        terms = DebtTerms(
            accountId = -1,
            originalPrincipalCents = originalCents,
            annualRateBps = if (method == InterestMethod.ZERO) 0 else rateBps,
            interestMethod = method,
            termMonths = termMonths,
            firstDueAt = firstDueAt,
            paymentsPerYear = 12,
            scheduleStartBalanceCents = amountCents,
            scheduleStartAt = todayMillis(),
        ),
        entries = emptyList(),
    )
}

private fun parseRate(input: String): Int? {
    val cleaned = input.trim().removeSuffix("%").replace(",", "").replace(" ", "")
    if (cleaned.isEmpty()) return 0
    val value = cleaned.toDoubleOrNull() ?: return null
    if (value < 0.0 || value > 1000.0) return null
    return (value * 100.0).roundToInt()
}

/** Treats a blank or explicit zero as 0; otherwise requires a positive debt amount. */
private fun parseOptionalAmount(input: String): Long? {
    val cleaned = input.trim()
        .replace("\u20B1", "")
        .replace(",", "")
        .replace(" ", "")
    if (cleaned.isEmpty() || cleaned == "0" || cleaned == "0.00") return 0L
    return Money.parseDebt(cleaned)
}

private fun rateInput(bps: Int): String {
    val value = bps / 100.0
    return if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()
}

private fun todayMillis(): Long =
    LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

private fun nextMonthMillis(): Long =
    LocalDate.now().plusMonths(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
