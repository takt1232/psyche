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
import com.psyche.app.data.AccountType
import com.psyche.app.data.SavingsGoal
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
import com.psyche.app.util.AccountBalance
import com.psyche.app.util.GoalProgress
import com.psyche.app.util.Money

@Composable
fun AccountsScreen(
    state: FinanceUiState,
    onSettings: () -> Unit,
    onAddAccount: () -> Unit,
    onTransfer: () -> Unit,
    onEditAccount: (Account) -> Unit,
    onRestoreAccount: (Account) -> Unit,
    onAddGoal: () -> Unit,
    onEditGoal: (SavingsGoal) -> Unit,
    onContribute: (GoalProgress) -> Unit,
    onRestoreGoal: (SavingsGoal) -> Unit,
) {
    val palette = LocalPsychePalette.current
    val archivedAccounts = state.snapshot.accounts.filter { it.archived }
    val archivedGoals = state.snapshot.goals.filter { it.archived }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.ground),
    ) {
        AppTopBar(
            subtitle = "CASH ${Money.formatPeso(state.cashTotalCents)}",
            onSettings = onSettings,
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 108.dp),
        ) {
            item {
                ScreenTitle(eyebrow = "The instruments", title = "ACCOUNTS")
                Spacer(Modifier.height(16.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = palette.boardRow,
                    border = BorderStroke(1.dp, palette.boardLine),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        StatCell(
                            label = "CASH + DEBIT",
                            value = Money.formatPeso(state.cashTotalCents),
                            valueStyle = PassType.monoData.copy(fontSize = 16.sp, fontWeight = FontWeight.Medium),
                            modifier = Modifier.weight(1f),
                        )
                        StatCell(
                            label = "SAVED IN GOALS",
                            value = Money.formatPeso(state.savingsTotalCents),
                            valueStyle = PassType.monoData.copy(fontSize = 16.sp, fontWeight = FontWeight.Medium),
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                if (state.snapshot.activeAccounts.size >= 2) {
                    SecondaryAction(
                        text = "Move money",
                        onClick = onTransfer,
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    MonoText(
                        text = "ADD A SECOND ACCOUNT TO MOVE MONEY BETWEEN THEM.",
                        color = palette.faint,
                        style = PassType.monoMicro.copy(fontSize = 7.sp),
                    )
                }
                Spacer(Modifier.height(26.dp))
                SectionHeading(
                    title = "Accounts",
                    trailing = {
                        AddAction(label = "Add account", onClick = onAddAccount)
                    },
                )
                Spacer(Modifier.height(8.dp))
            }
            if (state.accountBalances.isEmpty()) {
                item {
                    EmptyPanel(
                        title = "No accounts yet",
                        body = "Add the cash, debit, e-wallet, or credit account you actually spend from. Credit spending still counts against the cap.",
                        actionLabel = "Add account",
                        onAction = onAddAccount,
                        eyebrow = "NO ACCOUNTS",
                    )
                }
            } else {
                items(state.accountBalances.size, key = { index -> "acct-${state.accountBalances[index].account.id}" }) { index ->
                    AccountRow(
                        balance = state.accountBalances[index],
                        onClick = { onEditAccount(state.accountBalances[index].account) },
                    )
                }
            }
            item {
                Spacer(Modifier.height(30.dp))
                SectionHeading(
                    title = "Savings goals",
                    trailing = {
                        AddAction(label = "Add goal", onClick = onAddGoal)
                    },
                )
                Spacer(Modifier.height(4.dp))
                MonoText(
                    text = "GOAL CONTRIBUTIONS ARE TRANSFERS, NOT EXPENSES.",
                    color = palette.faint,
                    style = PassType.monoMicro.copy(fontSize = 7.sp),
                )
                Spacer(Modifier.height(8.dp))
            }
            if (state.goals.isEmpty()) {
                item {
                    EmptyPanel(
                        title = "No savings goal yet",
                        body = "Name a target and move money toward it from a cash or bank account. The cap is untouched.",
                        actionLabel = "Add goal",
                        onAction = onAddGoal,
                        eyebrow = "NO GOALS",
                    )
                }
            } else {
                items(state.goals.size, key = { index -> "goal-${state.goals[index].goal.id}" }) { index ->
                    val goal = state.goals[index]
                    GoalRow(
                        goal = goal,
                        onContribute = { onContribute(goal) },
                        onEdit = { onEditGoal(goal.goal) },
                    )
                }
            }
            if (archivedAccounts.isNotEmpty() || archivedGoals.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(28.dp))
                    SectionHeading(title = "Archived")
                    Spacer(Modifier.height(6.dp))
                    MonoText(
                        text = "HIDDEN FROM CAPTURE, STILL PART OF THE RECORD.",
                        color = palette.faint,
                        style = PassType.monoMicro.copy(fontSize = 7.sp),
                    )
                }
                items(archivedAccounts.size, key = { index -> "arch-${archivedAccounts[index].id}" }) { index ->
                    val account = archivedAccounts[index]
                    ArchivedRow(
                        name = "${account.name} \u00B7 ${account.type.label}",
                        detail = Money.formatPeso(account.openingBalanceCents),
                        onRestore = { onRestoreAccount(account) },
                    )
                }
                items(archivedGoals.size, key = { index -> "arch-goal-${archivedGoals[index].id}" }) { index ->
                    val goal = archivedGoals[index]
                    ArchivedRow(
                        name = goal.name,
                        detail = Money.formatPeso(goal.targetCents),
                        onRestore = { onRestoreGoal(goal) },
                    )
                }
            }
            item {
                Spacer(Modifier.height(24.dp))
                MonoText(
                    text = "OPENING BALANCES ARE YOURS, NOT DERIVED. EVERY LATER BALANCE IS CALCULATED FROM THE LEDGER.",
                    color = palette.faint,
                    style = PassType.monoMicro.copy(fontSize = 7.sp, letterSpacing = 0.5.sp),
                    maxLines = 3,
                )
            }
        }
    }
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

@Composable
private fun AccountRow(
    balance: AccountBalance,
    onClick: () -> Unit,
) {
    val palette = LocalPsychePalette.current
    val account = balance.account
    val credit = account.type == AccountType.CREDIT
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
                        text = account.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.onGround,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.width(6.dp))
                    Stamp(
                        text = account.type.label,
                        color = palette.faint,
                        borderColor = palette.boardLine,
                    )
                    if (account.isDefault) {
                        Spacer(Modifier.width(4.dp))
                        Stamp(
                            text = "Default",
                            color = palette.accentText,
                            borderColor = palette.panelLine,
                        )
                    }
                }
                Spacer(Modifier.height(3.dp))
                MonoText(
                    text = buildString {
                        append(account.type.shortLabel)
                        account.lastFour?.let { append(" \u00B7 \u2022\u2022$it") }
                        if (credit) append(" \u00B7 LIMIT ${Money.formatPeso(account.creditLimitCents)}")
                    },
                    color = palette.faint,
                    style = PassType.monoMicro.copy(fontSize = 7.sp),
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = Money.formatPeso(balance.balanceCents),
                    style = PassType.monoData.copy(fontSize = 15.sp),
                    color = if (balance.balanceCents < 0) palette.danger else palette.onGround,
                    maxLines = 1,
                )
                Text(
                    text = if (credit) {
                        "${Money.formatPeso(balance.availableCreditCents)} AVAILABLE"
                    } else {
                        "OPENING ${Money.formatPeso(account.openingBalanceCents)}"
                    },
                    style = PassType.monoMicro.copy(fontSize = 7.sp),
                    color = palette.faint,
                )
            }
        }
        if (credit && account.creditLimitCents > 0) {
            Spacer(Modifier.height(7.dp))
            ProgressTrack(
                fraction = balance.outstandingCents.toFloat() / account.creditLimitCents.toFloat(),
                color = if (balance.availableCreditCents <= 0) palette.danger else palette.alert,
                track = palette.panelLine,
                height = 4.dp,
            )
            Spacer(Modifier.height(4.dp))
            Row {
                MonoText(
                    text = "OUTSTANDING ${Money.formatPeso(balance.outstandingCents)}",
                    color = palette.faint,
                    style = PassType.monoMicro.copy(fontSize = 7.sp),
                )
                Spacer(Modifier.weight(1f))
                MonoText(
                    text = "TAP TO EDIT",
                    color = palette.faint,
                    style = PassType.monoMicro.copy(fontSize = 7.sp),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Hairline(color = palette.boardLine)
    }
}

@Composable
private fun GoalRow(
    goal: GoalProgress,
    onContribute: () -> Unit,
    onEdit: () -> Unit,
) {
    val palette = LocalPsychePalette.current
    Column(modifier = Modifier.padding(vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(role = Role.Button, onClick = onEdit),
            ) {
                Text(
                    text = goal.goal.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.onGround,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(3.dp))
                MonoText(
                    text = "${Money.formatPeso(goal.savedCents)} SAVED \u00B7 ${Money.formatPeso(goal.remainingCents)} TO GO",
                    color = palette.faint,
                    style = PassType.monoMicro.copy(fontSize = 7.sp),
                )
            }
            Text(
                text = "${(goal.fraction * 100).toInt()}%",
                style = PassType.monoData.copy(fontSize = 14.sp),
                color = palette.accentText,
            )
        }
        Spacer(Modifier.height(7.dp))
        ProgressTrack(
            fraction = goal.fraction,
            color = palette.alert,
            track = palette.panelLine,
            height = 5.dp,
        )
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            MonoText(
                text = "TARGET ${Money.formatPeso(goal.goal.targetCents)}",
                color = palette.faint,
                style = PassType.monoMicro.copy(fontSize = 7.sp),
            )
            Spacer(Modifier.weight(1f))
            if (goal.remainingCents > 0) {
                SecondaryAction(text = "Contribute", onClick = onContribute)
            } else {
                Stamp(text = "Funded", color = palette.accentText, borderColor = palette.panelLine)
            }
        }
        Spacer(Modifier.height(8.dp))
        Hairline(color = palette.boardLine)
    }
}

@Composable
private fun ArchivedRow(
    name: String,
    detail: String,
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
                text = name,
                style = MaterialTheme.typography.bodyMedium,
                color = palette.muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            MonoText(
                text = detail,
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
