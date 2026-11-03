package com.psyche.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.psyche.app.data.AccountType
import com.psyche.app.data.QuickTransaction
import com.psyche.app.data.TransactionType
import com.psyche.app.ui.FinanceUiState
import com.psyche.app.ui.MonthView
import com.psyche.app.ui.components.AppTopBar
import com.psyche.app.ui.components.BudgetPassCard
import com.psyche.app.ui.components.EmptyPanel
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
    onEditCap: () -> Unit,
    onSetup: () -> Unit,
    onAddAccount: () -> Unit,
    onAccounts: () -> Unit,
    onActivity: () -> Unit,
    onCopySummary: () -> Unit,
) {
    val palette = LocalPsychePalette.current
    val home = state.home
    val subtitle = if (home != null && home.capCents != null) {
        "${Formats.monthShort(home.month)} \u00B7 WK ${home.summary.weekIndex}/${home.summary.weekCount}"
    } else {
        "${Formats.monthShort(state.homeMonth)} \u00B7 SETUP"
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.ground),
    ) {
        AppTopBar(subtitle = subtitle, onSettings = onSettings)
        if (state.isLoading || home == null) {
            LoadingPass()
            return@Column
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 108.dp),
        ) {
            item {
                BudgetPassCard(
                    view = home,
                    quickActionCount = state.quickTransactions.size,
                    onEditCap = onEditCap,
                    onCopyPeriodSummary = onCopySummary,
                )
            }
            if (state.needsSetup) {
                item {
                    Spacer(Modifier.height(12.dp))
                    SetupPanel(
                        hasCap = home.capCents != null,
                        hasAccount = state.snapshot.activeAccounts.isNotEmpty(),
                        onSetup = onSetup,
                        onAddAccount = onAddAccount,
                        onAccounts = onAccounts,
                    )
                }
            }
            if (state.quickTransactions.isNotEmpty() || !state.needsSetup) {
                item {
                    Spacer(Modifier.height(22.dp))
                    OneTapSection(
                        quickTransactions = state.quickTransactions,
                        balances = state.accountBalances,
                        onRun = onRunQuick,
                        onManage = onManageQuick,
                    )
                }
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
            if (state.accountBalances.isNotEmpty() || state.goals.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(26.dp))
                    PocketSection(
                        balances = state.accountBalances,
                        goals = state.goals,
                        cashTotalCents = state.cashTotalCents,
                        savingsTotalCents = state.savingsTotalCents,
                        onAccounts = onAccounts,
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadingPass() {
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
            text = "READING THE LOCAL PASS",
            color = palette.muted,
            style = PassType.monoLabel.copy(letterSpacing = 1.5.sp),
        )
    }
}

@Composable
private fun SetupPanel(
    hasCap: Boolean,
    hasAccount: Boolean,
    onSetup: () -> Unit,
    onAddAccount: () -> Unit,
    onAccounts: () -> Unit,
) {
    val palette = LocalPsychePalette.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = palette.boardRow,
        border = BorderStroke(1.dp, palette.alert.copy(alpha = 0.55f)),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Stamp(
                    text = if (!hasCap) "FIRST RUN" else "ONE STEP LEFT",
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
                text = if (!hasCap) "Give the pass a number" else "Add a starting account",
                style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
                color = palette.onGround,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (!hasCap && hasAccount) {
                    "Set this month's spending cap. Your default Cash account is ready at \u20B10; set its opening balance in Accounts when you want."
                } else if (!hasCap) {
                    "Set this month's spending cap and add the account the money moves through. The app starts empty on purpose."
                } else {
                    "The cap is set. Add the cash or card account you spend from before the first capture."
                },
                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                color = palette.muted,
            )
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PrimaryAction(
                    text = if (!hasCap) "Set up pass" else "Add account",
                    onClick = if (!hasCap) onSetup else onAddAccount,
                    modifier = Modifier.weight(1f),
                )
                if (!hasCap && !hasAccount) {
                    QuietSetupAction(
                        text = "I have an account",
                        onClick = onAccounts,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun QuietSetupAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.heightIn(min = 52.dp), contentAlignment = Alignment.Center) {
        Text(
            text = text.uppercase(),
            style = PassType.monoLabel.copy(fontSize = 8.sp, letterSpacing = 0.7.sp),
            color = LocalPsychePalette.current.muted,
            modifier = Modifier
                .clickable(role = Role.Button, onClick = onClick)
                .padding(12.dp),
        )
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
                        style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                        color = palette.onGround,
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = "Custom buttons for the home-screen widget. Each tap logs the amount immediately.",
                        style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
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
                    val available = balancesById[quick.accountId]?.availableToMoveCents ?: 0L
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
                style = androidx.compose.material3.MaterialTheme.typography.titleSmall,
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

@Composable
private fun PocketSection(
    balances: List<AccountBalance>,
    goals: List<GoalProgress>,
    cashTotalCents: Long,
    savingsTotalCents: Long,
    onAccounts: () -> Unit,
) {
    val palette = LocalPsychePalette.current
    Column(Modifier.fillMaxWidth()) {
        SectionHeading(
            title = "Pocket",
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
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PocketStat(
                label = "CASH",
                value = Money.formatPeso(cashTotalCents),
                onClick = onAccounts,
                modifier = Modifier.weight(1f),
            )
            PocketStat(
                label = "SAVED",
                value = Money.formatPeso(savingsTotalCents),
                onClick = onAccounts,
                modifier = Modifier.weight(1f),
            )
        }
        if (goals.isNotEmpty() || balances.size > 1) {
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                balances.forEach { balance ->
                    AccountChip(balance)
                }
                goals.forEach { goal ->
                    GoalChip(goal)
                }
            }
        }
    }
}

@Composable
private fun PocketStat(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalPsychePalette.current
    Surface(
        modifier = modifier.clickable(role = Role.Button, onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = palette.boardRow,
        border = BorderStroke(1.dp, palette.boardLine),
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            MonoText(label, color = palette.faint, style = PassType.monoMicro)
            Spacer(Modifier.height(3.dp))
            Text(
                text = value,
                style = PassType.monoData.copy(fontSize = 15.sp),
                color = palette.onGround,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun AccountChip(balance: AccountBalance) {
    val palette = LocalPsychePalette.current
    val account = balance.account
    Surface(
        modifier = Modifier.width(158.dp),
        shape = RoundedCornerShape(10.dp),
        color = palette.boardRow,
        border = BorderStroke(1.dp, palette.boardLine),
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(
                text = account.name,
                style = androidx.compose.material3.MaterialTheme.typography.titleSmall,
                color = palette.onGround,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            MonoText(
                text = account.type.shortLabel + (account.lastFour?.let { " \u00B7 \u2022\u2022$it" } ?: ""),
                color = palette.faint,
                style = PassType.monoMicro.copy(fontSize = 7.sp),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = Money.formatPeso(balance.balanceCents),
                style = PassType.monoData.copy(fontSize = 14.sp),
                color = if (balance.balanceCents < 0) palette.danger else palette.onGround,
                maxLines = 1,
            )
            if (account.type == AccountType.CREDIT) {
                Spacer(Modifier.height(2.dp))
                MonoText(
                    text = "${Money.formatPeso(balance.availableCreditCents)} AVAILABLE",
                    color = palette.faint,
                    style = PassType.monoMicro.copy(fontSize = 7.sp),
                )
            }
        }
    }
}

@Composable
private fun GoalChip(goal: GoalProgress) {
    val palette = LocalPsychePalette.current
    Surface(
        modifier = Modifier.width(174.dp),
        shape = RoundedCornerShape(10.dp),
        color = palette.boardRow,
        border = BorderStroke(1.dp, palette.boardLine),
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = goal.goal.name,
                    style = androidx.compose.material3.MaterialTheme.typography.titleSmall,
                    color = palette.onGround,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Stamp(text = "Goal", color = palette.faint, borderColor = palette.boardLine)
            }
            Spacer(Modifier.height(6.dp))
            MonoText(
                text = "${Money.formatPeso(goal.savedCents)} / ${Money.formatPeso(goal.goal.targetCents)}",
                color = palette.muted,
                style = PassType.monoMicro.copy(fontSize = 8.sp),
            )
            Spacer(Modifier.height(6.dp))
            ProgressTrack(
                fraction = goal.fraction,
                color = palette.alert,
                track = palette.panelLine,
                height = 4.dp,
            )
        }
    }
}
