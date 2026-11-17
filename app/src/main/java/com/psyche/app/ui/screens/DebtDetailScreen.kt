package com.psyche.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.psyche.app.R
import com.psyche.app.ui.components.AutoSizeText
import com.psyche.app.ui.components.DangerAction
import com.psyche.app.ui.components.Hairline
import com.psyche.app.ui.components.KeyValueRow
import com.psyche.app.ui.components.MonoText
import com.psyche.app.ui.components.PrimaryAction
import com.psyche.app.ui.components.ProgressTrack
import com.psyche.app.ui.components.SecondaryAction
import com.psyche.app.ui.components.SectionHeading
import com.psyche.app.ui.components.Stamp
import com.psyche.app.ui.theme.LocalPsychePalette
import com.psyche.app.ui.theme.PassType
import com.psyche.app.util.DebtSummary
import com.psyche.app.util.Formats
import com.psyche.app.util.Installment
import com.psyche.app.util.InstallmentStatus
import com.psyche.app.util.Money

@Composable
fun DebtDetailScreen(
    summary: DebtSummary,
    onBack: () -> Unit,
    onPay: () -> Unit,
    onCollect: () -> Unit,
    onEdit: () -> Unit,
    onArchive: () -> Unit,
    onRestore: () -> Unit,
) {
    val palette = LocalPsychePalette.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.ground),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(start = 8.dp, end = 12.dp, top = 10.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(44.dp)) {
                Icon(
                    painter = painterResource(R.drawable.ic_chevron_left),
                    contentDescription = "Back to debts",
                    tint = palette.onGround,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                MonoText(
                    text = "THE OBLIGATIONS",
                    color = palette.faint,
                    style = PassType.monoLabel.copy(letterSpacing = 1.2.sp),
                )
                Text(
                    text = summary.account.name,
                    style = PassType.sectionTitle.copy(fontSize = 22.sp, letterSpacing = 1.2.sp),
                    color = palette.onGround,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Stamp(
                text = summary.account.type.label,
                color = palette.faint,
                borderColor = palette.boardLine,
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.navigationBars),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
        ) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = palette.pass,
                    contentColor = palette.passInk,
                    border = if (palette.isDark) null else BorderStroke(1.dp, palette.panelLine),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            MonoText(
                                text = if (summary.isReceivable) "TO COLLECT" else "BALANCE OWED",
                                color = palette.passMuted,
                                style = PassType.monoLabel.copy(letterSpacing = 1.2.sp),
                            )
                            Spacer(Modifier.weight(1f))
                            if (summary.hasSchedule) {
                                Stamp(
                                    text = summary.scheduleLabel,
                                    color = if (summary.overdue) palette.dangerOnPass else palette.passInk,
                                    borderColor = palette.passLine,
                                )
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        AutoSizeText(
                            text = Money.formatPeso(summary.balanceCents),
                            style = PassType.passAmount,
                            minFontSize = 26.sp,
                            maxFontSize = 48.sp,
                            color = if (summary.overdue) palette.dangerOnPass else palette.passInk,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = buildString {
                                append(summary.account.type.label)
                                summary.terms?.counterparty?.takeIf { it.isNotBlank() }?.let {
                                    append(" \u00B7 ")
                                    append(it)
                                }
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.passMuted,
                        )
                        if (summary.hasSchedule && summary.payoffProgress != null) {
                            Spacer(Modifier.height(12.dp))
                            ProgressTrack(
                                fraction = summary.payoffProgress,
                                color = if (summary.overdue) palette.dangerOnPass else palette.alert,
                                track = palette.passLine,
                                height = 5.dp,
                            )
                            Spacer(Modifier.height(4.dp))
                            Row {
                                MonoText(
                                    text = "${(summary.payoffProgress * 100).toInt()}% PAID",
                                    color = palette.passMuted,
                                    style = PassType.monoMicro.copy(fontSize = 7.sp),
                                )
                                Spacer(Modifier.weight(1f))
                                MonoText(
                                    text = "${summary.paidCount} OF ${summary.installments.size} INSTALLMENTS",
                                    color = palette.passMuted,
                                    style = PassType.monoMicro.copy(fontSize = 7.sp),
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (summary.balanceCents > 0) {
                        PrimaryAction(
                            text = if (summary.isReceivable) "Collect" else "Pay",
                            onClick = if (summary.isReceivable) onCollect else onPay,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    SecondaryAction(
                        text = "Edit",
                        onClick = onEdit,
                        modifier = Modifier.weight(1f),
                    )
                }
                if (summary.account.archived) {
                    Spacer(Modifier.height(8.dp))
                    SecondaryAction(
                        text = "Restore debt",
                        onClick = onRestore,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Spacer(Modifier.height(22.dp))
                SectionHeading(title = "Terms")
                Spacer(Modifier.height(4.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = palette.boardRow,
                    border = BorderStroke(1.dp, palette.boardLine),
                ) {
                    Column(Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
                        KeyValueRow(
                            label = "KIND",
                            value = summary.account.type.label,
                            valueColor = palette.onGround,
                        )
                        summary.terms?.let { terms ->
                            if (terms.annualRateBps > 0) {
                                KeyValueRow(
                                    label = "RATE",
                                    value = "${formatRate(terms.annualRateBps)} / YEAR",
                                    valueColor = palette.onGround,
                                )
                            }
                            if (summary.account.type.hasSchedule) {
                                KeyValueRow(
                                    label = "METHOD",
                                    value = terms.interestMethod.label,
                                    valueColor = palette.onGround,
                                )
                                if (terms.termMonths > 0) {
                                    KeyValueRow(
                                        label = "TERM",
                                        value = "${terms.termMonths} months",
                                        valueColor = palette.onGround,
                                    )
                                }
                            }
                            if (terms.originalPrincipalCents > 0) {
                                KeyValueRow(
                                    label = if (summary.account.type.hasSchedule) {
                                        "FINANCED"
                                    } else {
                                        "ORIGINAL"
                                    },
                                    value = Money.formatPeso(terms.originalPrincipalCents),
                                    valueColor = palette.onGround,
                                )
                            }
                            if (terms.downPaymentCents > 0) {
                                KeyValueRow(
                                    label = "DOWNPAYMENT",
                                    value = Money.formatPeso(terms.downPaymentCents),
                                    valueColor = palette.onGround,
                                )
                                KeyValueRow(
                                    label = "PURCHASE TOTAL",
                                    value = Money.formatPeso(
                                        terms.originalPrincipalCents + terms.downPaymentCents,
                                    ),
                                    valueColor = palette.onGround,
                                )
                            }
                            terms.counterparty?.takeIf { it.isNotBlank() }?.let {
                                KeyValueRow(
                                    label = if (summary.isReceivable) "FROM" else "LENDER",
                                    value = it,
                                    valueColor = palette.onGround,
                                )
                            }
                        }
                        if (summary.hasSchedule) {
                            KeyValueRow(
                                label = "INSTALLMENT",
                                value = Money.formatPeso(summary.installmentCents),
                                valueColor = palette.onGround,
                            )
                            val original = summary.terms?.originalPrincipalCents
                                ?.takeIf { it > 0 }
                                ?: summary.terms?.scheduleStartBalanceCents
                                ?: 0L
                            val paid = (original - summary.balanceCents).coerceAtLeast(0L)
                            KeyValueRow(
                                label = "PAID TO DATE",
                                value = Money.formatPeso(paid),
                                valueColor = palette.onGround,
                            )
                            KeyValueRow(
                                label = "PAYMENTS",
                                value = "${summary.paidCount} of ${summary.installments.size}",
                                valueColor = palette.onGround,
                            )
                            KeyValueRow(
                                label = "INTEREST TO COME",
                                value = Money.formatPeso(summary.interestToComeCents),
                                valueColor = palette.muted,
                            )
                            summary.nextDueAt?.let { due ->
                                KeyValueRow(
                                    label = "NEXT DUE",
                                    value = Formats.fullDate(due),
                                    valueColor = if (summary.overdue) palette.danger else palette.onGround,
                                )
                            }
                            summary.projectedPayoffAt?.let { payoff ->
                                KeyValueRow(
                                    label = "PROJECTED PAYOFF",
                                    value = Formats.fullDate(payoff),
                                    valueColor = palette.onGround,
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(22.dp))
            }

            if (summary.hasSchedule) {
                item {
                    SectionHeading(
                        title = "Schedule",
                        trailing = {
                            MonoText(
                                text = "${summary.remainingCount} LEFT",
                                color = palette.faint,
                                style = PassType.monoMicro.copy(fontSize = 7.sp),
                            )
                        },
                    )
                    Spacer(Modifier.height(4.dp))
                    MonoText(
                        text = "PAID ROWS REFLECT ACTUAL PRINCIPAL PAYMENTS. INTEREST IS CHARGED WHEN PAID.",
                        color = palette.faint,
                        style = PassType.monoMicro.copy(fontSize = 7.sp),
                        maxLines = 2,
                    )
                    Spacer(Modifier.height(6.dp))
                    ScheduleHeader()
                    Hairline(color = palette.boardLine)
                }
                items(summary.installments.size, key = { index -> "installment-${summary.installments[index].index}" }) { index ->
                    ScheduleRow(installment = summary.installments[index])
                }
            } else {
                item {
                    EmptyPanelLike(
                        title = when {
                            summary.isReceivable -> "Collection only"
                            summary.balanceCents == 0L -> "Settled"
                            else -> "No schedule"
                        },
                        body = when {
                            summary.isReceivable ->
                                "This is money owed to you. It stays out of the net position until it lands in an account."

                            summary.balanceCents == 0L ->
                                "This debt is paid. The terms and payments stay in the record."

                            summary.account.type.hasSchedule ->
                                "This debt has a balance but no amortization terms. Add a term and first due date from Edit to project the payoff."

                            else ->
                                "This balance is tracked without a schedule. Use Pay to reduce it, or Edit to update the lender."
                        },
                    )
                }
            }

            item {
                Spacer(Modifier.height(24.dp))
                if (!summary.account.archived) {
                    DangerAction(
                        text = "Archive debt",
                        onClick = onArchive,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    MonoText(
                        text = "ARCHIVING REMOVES IT FROM THE PASS AND SCHEDULE. THE RECORD STAYS.",
                        color = palette.faint,
                        style = PassType.monoMicro.copy(fontSize = 7.sp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ScheduleHeader() {
    val palette = LocalPsychePalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        MonoText(
            text = "DUE",
            color = palette.faint,
            style = PassType.monoMicro.copy(fontSize = 7.sp, letterSpacing = 1.sp),
            modifier = Modifier.width(58.dp),
        )
        MonoText(
            text = "PAYMENT",
            color = palette.faint,
            style = PassType.monoMicro.copy(fontSize = 7.sp, letterSpacing = 1.sp),
            modifier = Modifier.weight(1f),
        )
        MonoText(
            text = "PRINCIPAL",
            color = palette.faint,
            style = PassType.monoMicro.copy(fontSize = 7.sp, letterSpacing = 1.sp),
            modifier = Modifier.width(74.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
        )
        MonoText(
            text = "BALANCE",
            color = palette.faint,
            style = PassType.monoMicro.copy(fontSize = 7.sp, letterSpacing = 1.sp),
            modifier = Modifier.width(74.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
        )
    }
}

@Composable
private fun ScheduleRow(
    installment: Installment,
) {
    val palette = LocalPsychePalette.current
    val statusColor = when (installment.status) {
        InstallmentStatus.PAID -> palette.accentText
        InstallmentStatus.DUE -> palette.alert
        InstallmentStatus.OVERDUE -> palette.danger
        InstallmentStatus.UPCOMING -> palette.faint
    }
    val rowColor = when (installment.status) {
        InstallmentStatus.PAID -> palette.faint
        InstallmentStatus.OVERDUE -> palette.danger
        else -> palette.onGround
    }
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.width(58.dp)) {
                Text(
                    text = Formats.shortDateWithYear(installment.dueAt),
                    style = PassType.monoDataSmall.copy(fontSize = 8.sp),
                    color = rowColor,
                    maxLines = 1,
                )
                Text(
                    text = statusLabel(installment.status),
                    style = PassType.monoMicro.copy(fontSize = 6.sp),
                    color = statusColor,
                    maxLines = 1,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = Money.formatPeso(installment.paymentCents),
                    style = PassType.monoData.copy(fontSize = 11.sp),
                    color = rowColor,
                    maxLines = 1,
                )
                if (installment.interestCents > 0) {
                    Text(
                        text = "INT ${Money.formatPeso(installment.interestCents)}",
                        style = PassType.monoMicro.copy(fontSize = 6.sp),
                        color = palette.faint,
                        maxLines = 1,
                    )
                }
            }
            Text(
                text = Money.formatPeso(installment.principalCents, showSymbol = false),
                style = PassType.monoDataSmall.copy(fontSize = 8.sp),
                color = rowColor,
                modifier = Modifier.width(74.dp),
                maxLines = 1,
                textAlign = androidx.compose.ui.text.style.TextAlign.End,
            )
            Text(
                text = Money.formatPeso(installment.balanceCents, showSymbol = false),
                style = PassType.monoDataSmall.copy(fontSize = 8.sp),
                color = palette.muted,
                modifier = Modifier.width(74.dp),
                maxLines = 1,
                textAlign = androidx.compose.ui.text.style.TextAlign.End,
            )
        }
        Hairline(color = palette.boardLine)
    }
}

@Composable
private fun EmptyPanelLike(title: String, body: String) {
    val palette = LocalPsychePalette.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = palette.boardRow,
        border = BorderStroke(1.dp, palette.boardLine),
    ) {
        Column(Modifier.padding(16.dp)) {
            MonoText(text = "PLAN", color = palette.faint, style = PassType.monoMicro)
            Spacer(Modifier.height(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = palette.onGround,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = body,
                style = MaterialTheme.typography.bodySmall,
                color = palette.muted,
            )
        }
    }
}

private fun statusLabel(status: InstallmentStatus): String = when (status) {
    InstallmentStatus.PAID -> "PAID"
    InstallmentStatus.DUE -> "DUE"
    InstallmentStatus.OVERDUE -> "LATE"
    InstallmentStatus.UPCOMING -> "PLANNED"
}

private fun formatRate(bps: Int): String {
    val whole = bps / 100
    val fraction = bps % 100
    return if (fraction == 0) "$whole%" else "$whole.${fraction.toString().padStart(2, '0')}%"
}
