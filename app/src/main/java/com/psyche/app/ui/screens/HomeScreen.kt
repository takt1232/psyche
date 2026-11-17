package com.psyche.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.psyche.app.data.Account
import com.psyche.app.data.QuickTransaction
import com.psyche.app.data.TransactionType
import com.psyche.app.ui.FinanceUiState
import com.psyche.app.ui.components.AppTopBar
import com.psyche.app.ui.components.BalancePassCard
import com.psyche.app.ui.components.EmptyPanel
import com.psyche.app.ui.components.Hairline
import com.psyche.app.ui.components.LiveActivitySection
import com.psyche.app.ui.components.MonoText
import com.psyche.app.ui.components.PrimaryAction
import com.psyche.app.ui.components.ProgressTrack
import com.psyche.app.ui.components.SecondaryAction
import com.psyche.app.ui.components.SectionHeading
import com.psyche.app.ui.components.Stamp
import com.psyche.app.ui.theme.LocalPsychePalette
import com.psyche.app.ui.theme.PassType
import com.psyche.app.util.AccountBalance
import com.psyche.app.util.ActivityRow
import com.psyche.app.util.Formats
import com.psyche.app.util.GoalProgress
import com.psyche.app.util.Money

@Composable
fun HomeScreen(
    state: FinanceUiState,
    onRunQuick: (QuickTransaction) -> Unit,
    onManageQuick: () -> Unit,
    onCapture: () -> Unit,
    onSettings: () -> Unit,
    onOpenEntry: (ActivityRow) -> Unit,
    onAddAccount: () -> Unit,
    onAddCard: () -> Unit,
    onEditAccount: (Account) -> Unit,
    onAccounts: () -> Unit,
    onActivity: () -> Unit,
    onCopySummary: () -> Unit,
) {
    val palette = LocalPsychePalette.current
    val home = state.homeActivity
    val subtitle = "${Formats.shortDate(state.today)} \u00B7 ${state.position.status.label}"
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.ground),
    ) {
        AppTopBar(subtitle = subtitle, onSettings = onSettings)
        if (state.isLoading || home == null) {
            LoadingBalance()
            return@Column
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 108.dp),
        ) {
            item {
                BalancePassCard(
                    position = state.position,
                    today = state.today,
                    quickActionCount = state.quickTransactions.size,
                    onCopySummary = onCopySummary,
                    onAddAccount = onAddAccount,
                )
            }
            if (state.isFirstRun) {
                item {
                    Spacer(Modifier.height(12.dp))
                    FirstRunPanel(
                        state = state,
                        onEditAccount = onEditAccount,
                        onAddAccount = onAddAccount,
                        onAddCard = onAddCard,
                    )
                }
            }
            if (
                state.accountBalances.any { it.account.type.isLiquid || it.account.isCredit } ||
                state.goals.isNotEmpty()
            ) {
                item {
                    Spacer(Modifier.height(24.dp))
                    AccountsSection(
                        balances = state.accountBalances,
                        goals = state.goals,
                        onAccounts = onAccounts,
                        onEditAccount = onEditAccount,
                    )
                }
            }
            item {
                Spacer(Modifier.height(24.dp))
                OneTapSection(
                    quickTransactions = state.quickTransactions,
                    balances = state.accountBalances,
                    onRun = onRunQuick,
                    onManage = onManageQuick,
                )
            }
            item {
                Spacer(Modifier.height(24.dp))
                LiveActivitySection(
                    rows = home.rows,
                    today = state.today,
                    onRowClick = onOpenEntry,
                    onLogExpense = onCapture,
                    showDayInMeta = true,
                )
            }
            if (home.rows.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        MonoText(
                            text = "${Formats.entryCount(home.summary.entryCount)} THIS MONTH",
                            color = palette.faint,
                            style = PassType.monoMicro,
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            text = "FULL ACTIVITY",
                            style = PassType.monoMicro.copy(letterSpacing = 1.1.sp),
                            color = palette.muted,
                            modifier = Modifier
                                .clickable(role = Role.Button, onClick = onActivity)
                                .padding(vertical = 10.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingBalance() {
    val palette = LocalPsychePalette.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(80.dp))
        CircularProgressIndicator(
            color = palette.accentText,
            trackColor = palette.boardLine,
            strokeWidth = 2.dp,
            modifier = Modifier.width(28.dp),
        )
        Spacer(Modifier.height(16.dp))
        MonoText(
            text = "READING THE LOCAL LEDGER",
            color = palette.muted,
            style = PassType.monoLabel.copy(letterSpacing = 1.5.sp),
        )
    }
}

@Composable
private fun FirstRunPanel(
    state: FinanceUiState,
    onEditAccount: (Account) -> Unit,
    onAddAccount: () -> Unit,
    onAddCard: () -> Unit,
) {
    val palette = LocalPsychePalette.current
    val startingAccount = state.snapshot.activeLiquidAccounts
        .firstOrNull { it.isDefault }
        ?: state.snapshot.activeLiquidAccounts.firstOrNull()
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = palette.boardRow,
        border = BorderStroke(1.dp, palette.alert.copy(alpha = 0.55f)),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Stamp(
                    text = "FIRST RUN",
                    color = palette.accentText,
                    borderColor = palette.alert.copy(alpha = 0.5f),
                )
                Spacer(Modifier.weight(1f))
                MonoText(
                    text = "NOTHING PREFILLED",
                    color = palette.faint,
                    style = PassType.monoMicro.copy(fontSize = 7.sp),
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = "Set your real starting point",
                style = MaterialTheme.typography.titleLarge,
                color = palette.onGround,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Enter what Cash holds and add any card outstanding. The pass will tell you whether the money covers every card.",
                style = MaterialTheme.typography.bodyMedium,
                color = palette.muted,
            )
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PrimaryAction(
                    text = if (startingAccount != null) "Set cash" else "Add account",
                    onClick = {
                        if (startingAccount != null) {
                            onEditAccount(startingAccount)
                        } else {
                            onAddAccount()
                        }
                    },
                    modifier = Modifier.weight(1f),
                )
                SecondaryAction(
                    text = "Add card",
                    onClick = onAddCard,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun AccountsSection(
    balances: List<AccountBalance>,
    goals: List<GoalProgress>,
    onAccounts: () -> Unit,
    onEditAccount: (Account) -> Unit,
) {
    val palette = LocalPsychePalette.current
    val visible = balances.filter { it.account.type.isLiquid || it.account.isCredit }
    Column(Modifier.fillMaxWidth()) {
        SectionHeading(
            title = "Accounts",
            trailing = {
                Text(
                    text = "OPEN ACCOUNTS",
                    style = PassType.monoMicro.copy(letterSpacing = 0.9.sp),
                    color = palette.muted,
                    modifier = Modifier
                        .clickable(role = Role.Button, onClick = onAccounts)
                        .padding(vertical = 8.dp),
                )
            },
        )
        Spacer(Modifier.height(2.dp))
        if (visible.isEmpty()) {
            EmptyPanel(
                title = "No accounts yet",
                body = "Add the cash, debit, e-wallet, or credit card you actually use.",
                actionLabel = "Add account",
                onAction = onAccounts,
                eyebrow = "NO ACCOUNTS",
            )
        } else {
            visible.forEach { balance ->
                HomeAccountRow(
                    balance = balance,
                    onClick = { onEditAccount(balance.account) },
                )
            }
        }
        if (goals.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            SectionHeading(
                title = "Savings",
                trailing = {
                    Text(
                        text = "MANAGE",
                        style = PassType.monoMicro.copy(letterSpacing = 0.9.sp),
                        color = palette.muted,
                        modifier = Modifier
                            .clickable(role = Role.Button, onClick = onAccounts)
                            .padding(vertical = 8.dp),
                    )
                },
            )
            Spacer(Modifier.height(2.dp))
            goals.forEach { goal ->
                HomeGoalRow(goal = goal, onClick = onAccounts)
            }
        }
    }
}

@Composable
private fun HomeAccountRow(
    balance: AccountBalance,
    onClick: () -> Unit,
) {
    val palette = LocalPsychePalette.current
    val account = balance.account
    val credit = account.isCredit
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 11.dp),
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
                    if (account.isDefault) {
                        Spacer(Modifier.width(6.dp))
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
                        "OWED ${Money.formatPeso(balance.outstandingCents)}"
                    } else {
                        "CALCULATED BALANCE"
                    },
                    style = PassType.monoMicro.copy(fontSize = 7.sp),
                    color = if (credit && balance.outstandingCents > 0) palette.danger else palette.faint,
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
                    text = "LIMIT ${Money.formatPeso(account.creditLimitCents)}",
                    color = palette.faint,
                    style = PassType.monoMicro.copy(fontSize = 7.sp),
                )
                Spacer(Modifier.weight(1f))
                MonoText(
                    text = "${Money.formatPeso(balance.availableCreditCents)} AVAILABLE",
                    color = if (balance.availableCreditCents <= 0) palette.danger else palette.faint,
                    style = PassType.monoMicro.copy(fontSize = 7.sp),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Hairline(color = palette.boardLine)
    }
}

@Composable
private fun HomeGoalRow(
    goal: GoalProgress,
    onClick: () -> Unit,
) {
    val palette = LocalPsychePalette.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = goal.goal.name,
                style = MaterialTheme.typography.titleSmall,
                color = palette.onGround,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "${Money.formatPeso(goal.savedCents)} / ${Money.formatPeso(goal.goal.targetCents)}",
                style = PassType.monoDataSmall.copy(fontSize = 9.sp),
                color = palette.muted,
                maxLines = 1,
            )
        }
        Spacer(Modifier.height(7.dp))
        ProgressTrack(
            fraction = goal.fraction,
            color = palette.alert,
            track = palette.panelLine,
            height = 4.dp,
        )
        Spacer(Modifier.height(2.dp))
        Hairline(color = palette.boardLine)
    }
}

@Composable
private fun OneTapSection(
    quickTransactions: List<QuickTransaction>,
    balances: List<AccountBalance>,
    onRun: (QuickTransaction) -> Unit,
    onManage: () -> Unit,
) {
    val palette = LocalPsychePalette.current
    val balancesById = balances.associateBy { it.account.id }
    Column(Modifier.fillMaxWidth()) {
        SectionHeading(
            title = "One-tap",
            trailing = {
                Text(
                    text = if (quickTransactions.isEmpty()) "SET UP" else "EDIT",
                    style = PassType.monoMicro.copy(letterSpacing = 0.9.sp),
                    color = palette.muted,
                    modifier = Modifier
                        .clickable(role = Role.Button, onClick = onManage)
                        .padding(vertical = 8.dp),
                )
            },
        )
        Spacer(Modifier.height(8.dp))
        if (quickTransactions.isEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = palette.boardRow,
                border = BorderStroke(1.dp, palette.alert.copy(alpha = 0.45f)),
            ) {
                Column(Modifier.padding(14.dp)) {
                    Text(
                        text = "Log a preset in one tap",
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.onGround,
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = "Custom buttons for the home-screen widget. Each tap logs the amount immediately.",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.muted,
                    )
                    Spacer(Modifier.height(10.dp))
                    SecondaryAction(
                        text = "Make one-tap actions",
                        onClick = onManage,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                quickTransactions.take(4).forEach { quick ->
                    val available = balancesById[quick.accountId]?.availableToSpendCents ?: 0L
                    val insufficient = quick.type == TransactionType.EXPENSE &&
                        quick.amountCents > available
                    QuickActionCard(
                        quick = quick,
                        insufficient = insufficient,
                        onRun = { onRun(quick) },
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickActionCard(
    quick: QuickTransaction,
    insufficient: Boolean,
    onRun: () -> Unit,
) {
    val palette = LocalPsychePalette.current
    Surface(
        onClick = onRun,
        modifier = Modifier.width(152.dp),
        shape = RoundedCornerShape(10.dp),
        color = palette.boardRow,
        border = BorderStroke(
            1.dp,
            if (insufficient) palette.danger.copy(alpha = 0.65f) else palette.boardLine,
        ),
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MonoText(
                    text = if (quick.type == TransactionType.EXPENSE) "EXPENSE" else "INCOME",
                    color = palette.accentText,
                    style = PassType.monoMicro.copy(fontSize = 7.sp, letterSpacing = 0.9.sp),
                )
                Spacer(Modifier.weight(1f))
                MonoText(
                    text = if (insufficient) "LOW" else "TAP",
                    color = if (insufficient) palette.danger else palette.faint,
                    style = PassType.monoMicro.copy(fontSize = 7.sp),
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = quick.label,
                style = MaterialTheme.typography.titleSmall,
                color = if (insufficient) palette.danger else palette.onGround,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = Money.formatPeso(quick.amountCents),
                style = PassType.monoData.copy(fontSize = 15.sp),
                color = if (insufficient) palette.danger else palette.onGround,
                maxLines = 1,
            )
        }
    }
}
