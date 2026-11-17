package com.psyche.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.psyche.app.data.Account
import com.psyche.app.ui.FinanceUiState
import com.psyche.app.ui.components.AppTopBar
import com.psyche.app.ui.components.EmptyPanel
import com.psyche.app.ui.components.Hairline
import com.psyche.app.ui.components.MonoText
import com.psyche.app.ui.components.ProgressTrack
import com.psyche.app.ui.components.ScreenTitle
import com.psyche.app.ui.components.SecondaryAction
import com.psyche.app.ui.components.SectionHeading
import com.psyche.app.ui.components.StatCell
import com.psyche.app.ui.components.Stamp
import com.psyche.app.ui.theme.LocalPsychePalette
import com.psyche.app.ui.theme.PassType
import com.psyche.app.util.DebtSummary
import com.psyche.app.util.Formats
import com.psyche.app.util.Installment
import com.psyche.app.util.InstallmentStatus
import com.psyche.app.util.Money
import java.time.LocalDate
import java.time.ZoneId

@Composable
fun DebtsScreen(
    state: FinanceUiState,
    onSettings: () -> Unit,
    onAddDebt: () -> Unit,
    onAddReceivable: () -> Unit,
    onOpenDebt: (Account) -> Unit,
    onPay: (DebtSummary) -> Unit,
    onCollect: (DebtSummary) -> Unit,
    onAccounts: () -> Unit,
    onRestoreAccount: (Account) -> Unit,
) {
    val palette = LocalPsychePalette.current
    val archived = state.snapshot.accounts.filter {
        it.archived && ((it.type.isLiability && !it.isCredit) || it.type.isReceivable)
    }
    val dueSoon = state.debts
        .flatMap { debt ->
            debt.installments
                .filter { it.status != InstallmentStatus.PAID && isDueWithin(it, state.today, 30) }
                .map { debt to it }
        }
        .sortedBy { it.second.dueAt }
        .take(6)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.ground),
    ) {
        AppTopBar(
            subtitle = "NET ${Money.formatPeso(state.position.netCents)}",
            onSettings = onSettings,
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 108.dp),
        ) {
            item {
                ScreenTitle(eyebrow = "The obligations", title = "DEBTS")
                Spacer(Modifier.height(14.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = palette.boardRow,
                    border = BorderStroke(1.dp, palette.boardLine),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        StatCell(
                            label = "YOU OWE",
                            value = Money.formatPeso(state.position.owedCents),
                            valueColor = if (state.position.owedCents > 0) palette.danger else palette.onGround,
                            valueStyle = PassType.monoData.copy(fontSize = 14.sp, fontWeight = FontWeight.Medium),
                            modifier = Modifier.weight(1f),
                        )
                        StatCell(
                            label = "OWED TO YOU",
                            value = Money.formatPeso(state.position.receivableCents),
                            valueColor = if (state.position.receivableCents > 0) palette.accentText else palette.onGround,
                            valueStyle = PassType.monoData.copy(fontSize = 14.sp, fontWeight = FontWeight.Medium),
                            modifier = Modifier.weight(1f),
                        )
                        StatCell(
                            label = "DUE 7D",
                            value = Money.formatPeso(state.position.due7Cents),
                            valueColor = if (state.position.due7Cents > 0) palette.alert else palette.onGround,
                            valueStyle = PassType.monoData.copy(fontSize = 14.sp, fontWeight = FontWeight.Medium),
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                MonoText(
                    text = "ONLY PRINCIPAL COUNTS IN NET. FUTURE INTEREST IS A PROJECTION, NOT DEBT.",
                    color = palette.faint,
                    style = PassType.monoMicro.copy(fontSize = 7.sp),
                    maxLines = 2,
                )
                Spacer(Modifier.height(22.dp))
            }

            if (dueSoon.isNotEmpty()) {
                item {
                    SectionHeading(title = "Due soon")
                    Spacer(Modifier.height(4.dp))
                    MonoText(
                        text = "NEXT 30 DAYS. TAP A ROW TO OPEN THE SCHEDULE.",
                        color = palette.faint,
                        style = PassType.monoMicro.copy(fontSize = 7.sp),
                    )
                    Spacer(Modifier.height(6.dp))
                }
                items(dueSoon.size, key = { index ->
                    "due-${dueSoon[index].first.account.id}-${dueSoon[index].second.index}"
                }) { index ->
                    val (debt, installment) = dueSoon[index]
                    DueSoonRow(
                        debt = debt,
                        installment = installment,
                        today = state.today,
                        onClick = { onOpenDebt(debt.account) },
                    )
                }
                item { Spacer(Modifier.height(22.dp)) }
            }

            item {
                SectionHeading(
                    title = "You owe",
                    trailing = { AddAction(label = "Add debt", onClick = onAddDebt) },
                )
                Spacer(Modifier.height(8.dp))
            }

            if (state.position.creditAccountCount > 0) {
                item {
                    CardsSummaryRow(
                        count = state.position.creditAccountCount,
                        outstanding = state.position.cardOwedCents,
                        onAccounts = onAccounts,
                    )
                }
            }

            if (state.debts.isEmpty()) {
                item {
                    EmptyPanel(
                        title = "No loans or installments tracked",
                        body = "Add a loan, an installment, or an informal utang. Principal counts in the pass; the schedule tracks what is due next.",
                        actionLabel = "Add debt",
                        onAction = onAddDebt,
                        eyebrow = "NO DEBTS",
                    )
                }
            } else {
                items(state.debts.size, key = { index -> "debt-${state.debts[index].account.id}" }) { index ->
                    val debt = state.debts[index]
                    DebtRow(
                        debt = debt,
                        today = state.today,
                        onClick = { onOpenDebt(debt.account) },
                        onPay = { onPay(debt) },
                    )
                }
            }

            item {
                Spacer(Modifier.height(28.dp))
                SectionHeading(
                    title = "Owed to you",
                    trailing = { AddAction(label = "Add receivable", onClick = onAddReceivable) },
                )
                Spacer(Modifier.height(4.dp))
                MonoText(
                    text = "TRACKED, BUT NOT COUNTED IN NET UNTIL COLLECTED.",
                    color = palette.faint,
                    style = PassType.monoMicro.copy(fontSize = 7.sp),
                )
                Spacer(Modifier.height(8.dp))
            }
            if (state.receivables.isEmpty()) {
                item {
                    EmptyPanel(
                        title = "Nothing owed to you",
                        body = "If you lend money, record it here so the cash that left is visible and the collection has a destination.",
                        actionLabel = "Add receivable",
                        onAction = onAddReceivable,
                        eyebrow = "NO RECEIVABLES",
                    )
                }
            } else {
                items(state.receivables.size, key = { index -> "recv-${state.receivables[index].account.id}" }) { index ->
                    val receivable = state.receivables[index]
                    ReceivableRow(
                        debt = receivable,
                        onClick = { onOpenDebt(receivable.account) },
                        onCollect = { onCollect(receivable) },
                    )
                }
            }

            if (archived.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(28.dp))
                    SectionHeading(title = "Archived")
                    Spacer(Modifier.height(6.dp))
                    MonoText(
                        text = "HIDDEN FROM THE SCHEDULE AND NOT COUNTED IN TOTALS.",
                        color = palette.faint,
                        style = PassType.monoMicro.copy(fontSize = 7.sp),
                    )
                }
                items(archived.size, key = { index -> "arch-debt-${archived[index].id}" }) { index ->
                    val account = archived[index]
                    ArchivedDebtRow(
                        account = account,
                        onRestore = { onRestoreAccount(account) },
                    )
                }
            }

            item {
                Spacer(Modifier.height(24.dp))
                MonoText(
                    text = "INTEREST IS RECORDED WHEN YOU PAY. THE SCHEDULE IS A PROJECTION FROM THE TERMS, NOT A SECOND LEDGER.",
                    color = palette.faint,
                    style = PassType.monoMicro.copy(fontSize = 7.sp, letterSpacing = 0.5.sp),
                    maxLines = 3,
                )
            }
        }
    }
}

@Composable
private fun DueSoonRow(
    debt: DebtSummary,
    installment: Installment,
    today: LocalDate,
    onClick: () -> Unit,
) {
    val palette = LocalPsychePalette.current
    val overdue = installment.status == InstallmentStatus.OVERDUE
    val color = if (overdue) palette.danger else palette.alert
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.width(66.dp)) {
            Text(
                text = dueDateLabel(installment.dueAt, today),
                style = PassType.monoData.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium),
                color = if (overdue) palette.danger else palette.onGround,
            )
            Text(
                text = if (overdue) "OVERDUE" else dueLabel(installment.dueAt, today),
                style = PassType.monoMicro.copy(fontSize = 7.sp),
                color = color,
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = debt.account.name,
                style = MaterialTheme.typography.titleSmall,
                color = palette.onGround,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            MonoText(
                text = "${debt.account.type.shortLabel} \u00B7 INSTALLMENT",
                color = palette.faint,
                style = PassType.monoMicro.copy(fontSize = 7.sp),
            )
        }
        Text(
            text = Money.formatPeso(installment.paymentCents),
            style = PassType.monoData.copy(fontSize = 13.sp),
            color = if (overdue) palette.danger else palette.onGround,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
    Hairline(color = palette.boardLine)
}

@Composable
private fun CardsSummaryRow(
    count: Int,
    outstanding: Long,
    onAccounts: () -> Unit,
) {
    val palette = LocalPsychePalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onAccounts)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Credit cards",
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.onGround,
                )
                Spacer(Modifier.width(6.dp))
                Stamp(
                    text = "$count ${if (count == 1) "card" else "cards"}",
                    color = palette.faint,
                    borderColor = palette.boardLine,
                )
            }
            Spacer(Modifier.height(3.dp))
            MonoText(
                text = "REVOLVING \u00B7 MANAGED IN ACCOUNTS",
                color = palette.faint,
                style = PassType.monoMicro.copy(fontSize = 7.sp),
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = Money.formatPeso(outstanding),
                style = PassType.monoData.copy(fontSize = 15.sp),
                color = if (outstanding > 0) palette.danger else palette.onGround,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "MANAGE",
                style = PassType.monoMicro.copy(fontSize = 7.sp, letterSpacing = 0.8.sp),
                color = palette.accentText,
            )
        }
    }
    Hairline(color = palette.boardLine)
}

@Composable
private fun DebtRow(
    debt: DebtSummary,
    today: LocalDate,
    onClick: () -> Unit,
    onPay: () -> Unit,
) {
    val palette = LocalPsychePalette.current
    val overdue = debt.overdue
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = debt.account.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.onGround,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Spacer(Modifier.width(6.dp))
                    Stamp(
                        text = debt.account.type.shortLabel,
                        color = palette.faint,
                        borderColor = palette.boardLine,
                    )
                    debt.terms?.counterparty?.let { counterparty ->
                        Spacer(Modifier.width(4.dp))
                        Stamp(
                            text = counterparty,
                            color = palette.faint,
                            borderColor = palette.boardLine,
                        )
                    }
                }
                Spacer(Modifier.height(3.dp))
                MonoText(
                    text = debtMeta(debt, today),
                    color = if (overdue) palette.danger else palette.faint,
                    style = PassType.monoMicro.copy(fontSize = 7.sp),
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = Money.formatPeso(debt.balanceCents),
                    style = PassType.monoData.copy(fontSize = 15.sp),
                    color = if (debt.balanceCents > 0) palette.danger else palette.onGround,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "OWED",
                    style = PassType.monoMicro.copy(fontSize = 7.sp),
                    color = palette.faint,
                )
            }
        }
        if (debt.payoffProgress != null) {
            Spacer(Modifier.height(7.dp))
            ProgressTrack(
                fraction = debt.payoffProgress,
                color = if (overdue) palette.danger else palette.alert,
                track = palette.panelLine,
                height = 4.dp,
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                MonoText(
                    text = "${(debt.payoffProgress * 100).toInt()}% PAID",
                    color = palette.faint,
                    style = PassType.monoMicro.copy(fontSize = 7.sp),
                )
                Spacer(Modifier.weight(1f))
                MonoText(
                    text = debt.scheduleLabel,
                    color = if (overdue) palette.danger else if (debt.aheadMonths > 0) palette.accentText else palette.faint,
                    style = PassType.monoMicro.copy(fontSize = 7.sp),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            MonoText(
                text = debt.installmentCents.takeIf { it > 0 }?.let {
                    "${Money.formatPeso(it)} / MO"
                } ?: "NO SCHEDULE",
                color = palette.faint,
                style = PassType.monoMicro.copy(fontSize = 7.sp),
            )
            Spacer(Modifier.weight(1f))
            if (debt.balanceCents > 0) {
                SecondaryAction(text = "Pay", onClick = onPay)
            } else {
                Stamp(text = "Settled", color = palette.accentText, borderColor = palette.panelLine)
            }
        }
        Spacer(Modifier.height(8.dp))
        Hairline(color = palette.boardLine)
    }
}

@Composable
private fun ReceivableRow(
    debt: DebtSummary,
    onClick: () -> Unit,
    onCollect: () -> Unit,
) {
    val palette = LocalPsychePalette.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = debt.account.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.onGround,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Spacer(Modifier.width(6.dp))
                    Stamp(
                        text = "TO COLLECT",
                        color = palette.accentText,
                        borderColor = palette.panelLine,
                    )
                }
                Spacer(Modifier.height(3.dp))
                MonoText(
                    text = debt.terms?.counterparty?.takeIf { it.isNotBlank() }
                        ?.let { "FROM $it" }
                        ?: "NO COUNTERPARTY SET",
                    color = palette.faint,
                    style = PassType.monoMicro.copy(fontSize = 7.sp),
                )
            }
            Spacer(Modifier.width(10.dp))
            Text(
                text = Money.formatPeso(debt.balanceCents),
                style = PassType.monoData.copy(fontSize = 15.sp),
                color = palette.accentText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            MonoText(
                text = "EXCLUDED FROM NET",
                color = palette.faint,
                style = PassType.monoMicro.copy(fontSize = 7.sp),
            )
            Spacer(Modifier.weight(1f))
            if (debt.balanceCents > 0) {
                SecondaryAction(text = "Collect", onClick = onCollect)
            } else {
                Stamp(text = "Collected", color = palette.accentText, borderColor = palette.panelLine)
            }
        }
        Spacer(Modifier.height(8.dp))
        Hairline(color = palette.boardLine)
    }
}

@Composable
private fun ArchivedDebtRow(
    account: Account,
    onRestore: () -> Unit,
) {
    val palette = LocalPsychePalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = "${account.name} \u00B7 ${account.type.label}",
                style = MaterialTheme.typography.bodyMedium,
                color = palette.muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            MonoText(
                text = "ARCHIVED",
                color = palette.faint,
                style = PassType.monoMicro.copy(fontSize = 7.sp),
            )
        }
        Text(
            text = "RESTORE",
            style = PassType.monoMicro.copy(letterSpacing = 1.sp),
            color = palette.accentText,
            modifier = Modifier
                .clickable(role = Role.Button, onClick = onRestore)
                .padding(10.dp),
        )
    }
    Hairline(color = palette.boardLine)
}

@Composable
private fun AddAction(label: String, onClick: () -> Unit) {
    val palette = LocalPsychePalette.current
    Row(
        modifier = Modifier
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "+",
            style = PassType.monoData.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold),
            color = palette.accentText,
        )
        Spacer(Modifier.width(5.dp))
        Text(
            text = label.uppercase(),
            style = PassType.monoMicro.copy(letterSpacing = 1.sp),
            color = palette.muted,
        )
    }
}

private fun debtMeta(debt: DebtSummary, today: LocalDate): String = buildString {
    append(debt.account.type.shortLabel)
    debt.terms?.let { terms ->
        if (terms.annualRateBps > 0) {
            append(" \u00B7 ")
            append(formatRate(terms.annualRateBps))
            append(" / YR")
        }
        if (terms.termMonths > 0) {
            append(" \u00B7 ")
            append(terms.termMonths)
            append(" MO")
        }
    }
    debt.nextDueAt?.let { due ->
        append(" \u00B7 NEXT ")
        append(dueDateLabel(due, today))
        if (due < today.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()) {
            append(" \u00B7 LATE")
        }
    }
}

private fun formatRate(bps: Int): String {
    val whole = bps / 100
    val fraction = bps % 100
    return if (fraction == 0) "$whole%" else "$whole.${fraction.toString().padStart(2, '0')}%"
}

private fun dueLabel(dueAt: Long, today: LocalDate): String {
    val due = java.time.Instant.ofEpochMilli(dueAt)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
    val days = java.time.temporal.ChronoUnit.DAYS.between(today, due)
    return when {
        days <= 0 -> "DUE NOW"
        days == 1L -> "TOMORROW"
        else -> "IN $days DAYS"
    }
}

private fun isDueWithin(installment: Installment, today: LocalDate, days: Long): Boolean {
    if (installment.status == InstallmentStatus.OVERDUE) return true
    val due = java.time.Instant.ofEpochMilli(installment.dueAt)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
    return !due.isBefore(today) && !due.isAfter(today.plusDays(days))
}

private fun dueDateLabel(instant: Long, today: LocalDate): String {
    val date = java.time.Instant.ofEpochMilli(instant)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
    return if (date.year == today.year) {
        Formats.shortDate(instant)
    } else {
        Formats.shortDateWithYear(instant)
    }
}
