package com.psyche.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import com.psyche.app.ui.FinanceUiState
import com.psyche.app.ui.MonthView
import com.psyche.app.ui.components.AppTopBar
import com.psyche.app.ui.components.EmptyPanel
import com.psyche.app.ui.components.Hairline
import com.psyche.app.ui.components.MonoText
import com.psyche.app.ui.components.MonthSwitcher
import com.psyche.app.ui.components.PrimaryAction
import com.psyche.app.ui.components.ProgressTrack
import com.psyche.app.ui.components.ScreenTitle
import com.psyche.app.ui.components.SecondaryAction
import com.psyche.app.ui.components.Stamp
import com.psyche.app.ui.theme.LocalPsychePalette
import com.psyche.app.ui.theme.PassType
import com.psyche.app.util.BudgetStatus
import com.psyche.app.util.CategoryLine
import com.psyche.app.util.Formats
import com.psyche.app.util.Money

@Composable
fun BudgetsScreen(
    state: FinanceUiState,
    onSettings: () -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onResetMonth: () -> Unit,
    onEditCap: () -> Unit,
    onEditAllocations: () -> Unit,
    onManageCategories: () -> Unit,
    onCapture: () -> Unit,
) {
    val palette = LocalPsychePalette.current
    val view = state.budget
    val subtitle = view?.let {
        "${Formats.monthShort(it.month)} \u00B7 ${it.summary.statusLabel}"
    } ?: "BUDGETS"
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.ground),
    ) {
        AppTopBar(subtitle = subtitle, onSettings = onSettings)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 108.dp),
        ) {
            item {
                ScreenTitle(eyebrow = "The cap", title = "BUDGETS")
                Spacer(Modifier.height(12.dp))
                MonthSwitcher(
                    month = view?.month ?: state.budgetMonth,
                    isCurrent = view?.summary?.isCurrentMonth == true,
                    onPrevious = onPreviousMonth,
                    onNext = onNextMonth,
                    onReset = onResetMonth,
                )
                Spacer(Modifier.height(16.dp))
            }
            item {
                if (view == null) {
                    MonoText("READING THE LEDGER", color = palette.muted)
                } else {
                    CapPanel(
                        view = view,
                        onEditCap = onEditCap,
                        onEditAllocations = onEditAllocations,
                    )
                }
            }
            if (view != null) {
                item {
                    Spacer(Modifier.height(26.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "CATEGORY LINES",
                            style = PassType.sectionTitle.copy(fontSize = 17.sp, letterSpacing = 1.8.sp),
                            color = palette.onGround,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = "MANAGE",
                            style = PassType.monoMicro.copy(letterSpacing = 1.1.sp),
                            color = palette.muted,
                            modifier = Modifier
                                .clickable(role = Role.Button, onClick = onManageCategories)
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                }
                if (view.hasCategories || view.uncategorizedSpentCents > 0) {
                    items(view.categories.size, key = { index -> "cat-${view.categories[index].category.id}" }) { index ->
                        CategoryBudgetRow(view.categories[index])
                    }
                    if (view.uncategorizedSpentCents > 0) {
                        item {
                            UncategorizedRow(view.uncategorizedSpentCents)
                        }
                    }
                    item {
                        AllocationFooter(view)
                    }
                } else {
                    item {
                        Spacer(Modifier.height(8.dp))
                        EmptyPanel(
                            title = "No category lines",
                            body = "Categories are optional. The cap still works on its own; add lines only if you want to split the month.",
                            actionLabel = "Add categories",
                            onAction = onManageCategories,
                        )
                    }
                }
                item {
                    Spacer(Modifier.height(18.dp))
                    MonoText(
                        text = "ALLOCATIONS CANNOT EXCEED THE CAP. SPENDING COUNTS AGAINST THE CAP EVEN WHEN A CATEGORY HAS NO LINE.",
                        color = palette.faint,
                        style = PassType.monoMicro.copy(fontSize = 7.sp, letterSpacing = 0.5.sp),
                        maxLines = 3,
                    )
                }
            }
        }
    }
}

@Composable
private fun CapPanel(
    view: MonthView,
    onEditCap: () -> Unit,
    onEditAllocations: () -> Unit,
) {
    val palette = LocalPsychePalette.current
    val summary = view.summary
    val hasCap = summary.capCents != null
    val spentFraction = if (hasCap && summary.capCents > 0) {
        summary.spentCents.toFloat() / summary.capCents.toFloat()
    } else {
        0f
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = palette.boardRow,
        border = BorderStroke(1.dp, palette.boardLine),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MonoText(
                    text = "MONTHLY CAP",
                    color = palette.faint,
                    style = PassType.monoLabel.copy(letterSpacing = 1.4.sp),
                )
                Spacer(Modifier.weight(1f))
                Stamp(
                    text = summary.statusLabel,
                    color = when (summary.status) {
                        BudgetStatus.OVER -> palette.danger
                        BudgetStatus.TIGHT -> if (palette.isDark) palette.alert else palette.cautionOnPass
                        else -> palette.muted
                    },
                    borderColor = when (summary.status) {
                        BudgetStatus.OVER -> palette.danger
                        else -> palette.boardLine
                    },
                )
            }
            Spacer(Modifier.height(8.dp))
            if (hasCap) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = Money.formatPeso(summary.capCents),
                        style = PassType.passAmount.copy(fontSize = 42.sp, lineHeight = 42.sp),
                        color = palette.onGround,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Column(horizontalAlignment = Alignment.End) {
                        MonoText(
                            text = if (summary.remainingCents != null && summary.remainingCents < 0) {
                                "OVER BY"
                            } else {
                                "LEFT"
                            },
                            color = palette.faint,
                            style = PassType.monoMicro.copy(letterSpacing = 1.1.sp),
                        )
                        Text(
                            text = if (summary.status == BudgetStatus.OVER) {
                                Money.formatPeso(summary.overByCents)
                            } else {
                                summary.remainingCents?.let { Money.formatPeso(it) } ?: "\u2014"
                            },
                            style = PassType.monoData.copy(fontSize = 17.sp),
                            color = if (summary.status == BudgetStatus.OVER) palette.danger else palette.onGround,
                            maxLines = 1,
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                ProgressTrack(
                    fraction = spentFraction,
                    color = if (summary.status == BudgetStatus.OVER) palette.danger else palette.alert,
                    track = palette.panelLine,
                    height = 6.dp,
                )
                Spacer(Modifier.height(5.dp))
                Row {
                    MonoText(
                        text = "SPENT ${Money.formatPeso(summary.spentCents)}",
                        color = palette.muted,
                        style = PassType.monoMicro.copy(fontSize = 7.sp),
                    )
                    Spacer(Modifier.weight(1f))
                    MonoText(
                        text = "${(spentFraction * 100f).coerceAtLeast(0f).toInt()}% USED",
                        color = palette.faint,
                        style = PassType.monoMicro.copy(fontSize = 7.sp),
                    )
                }
                Spacer(Modifier.height(12.dp))
                Hairline(color = palette.boardLine)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Stat(
                        label = "DAILY",
                        value = Money.formatPeso(summary.dailyAllowanceCents ?: 0),
                        modifier = Modifier.weight(1f),
                    )
                    Stat(
                        label = "DAYS LEFT",
                        value = summary.daysLeft.toString(),
                        modifier = Modifier.weight(0.7f),
                    )
                    Stat(
                        label = "WEEK",
                        value = "${summary.weekIndex}/${summary.weekCount}",
                        modifier = Modifier.weight(0.7f),
                    )
                }
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PrimaryAction(
                        text = "Edit cap",
                        onClick = onEditCap,
                        modifier = Modifier.weight(1f),
                    )
                    SecondaryAction(
                        text = "Allocations",
                        onClick = onEditAllocations,
                        modifier = Modifier.weight(1f),
                    )
                }
            } else {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "NO CAP SET",
                    style = PassType.sectionTitle.copy(fontSize = 28.sp, letterSpacing = 1.2.sp),
                    color = palette.onGround,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "A monthly cap gives the pass its number. Nothing is prefilled.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.muted,
                )
                if (view.uncategorizedSpentCents > 0) {
                    Spacer(Modifier.height(8.dp))
                    MonoText(
                        text = "THIS MONTH ALREADY HAS ${Money.formatPeso(summary.spentCents)} OF SPENDING.",
                        color = palette.muted,
                        style = PassType.monoMicro,
                    )
                }
                Spacer(Modifier.height(14.dp))
                PrimaryAction(
                    text = "Set monthly cap",
                    onClick = onEditCap,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun Stat(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    val palette = LocalPsychePalette.current
    Column(modifier = modifier) {
        MonoText(
            text = label,
            color = palette.faint,
            style = PassType.monoMicro.copy(fontSize = 7.sp, letterSpacing = 1.sp),
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = value,
            style = PassType.monoData.copy(fontSize = 13.sp),
            color = palette.onGround,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun CategoryBudgetRow(line: CategoryLine) {
    val palette = LocalPsychePalette.current
    val remaining = line.remainingCents
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = line.category.name,
                style = MaterialTheme.typography.titleSmall,
                color = palette.onGround,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (line.category.archived) {
                Stamp(text = "Archived", color = palette.faint, borderColor = palette.boardLine)
                Spacer(Modifier.width(6.dp))
            }
            Text(
                text = when {
                    line.allocatedCents == null -> "NO LINE"
                    remaining != null && remaining < 0 -> "${Money.formatPeso(-remaining)} OVER"
                    else -> "${Money.formatPeso(remaining ?: 0)} LEFT"
                },
                style = PassType.monoDataSmall.copy(fontSize = 9.sp),
                color = when {
                    line.allocatedCents == null -> palette.faint
                    line.isOver -> palette.danger
                    else -> palette.muted
                },
                maxLines = 1,
            )
        }
        Spacer(Modifier.height(4.dp))
        Row {
            MonoText(
                text = if (line.allocatedCents != null) {
                    "SPENT ${Money.formatPeso(line.spentCents)} OF ${Money.formatPeso(line.allocatedCents)}"
                } else {
                    "SPENT ${Money.formatPeso(line.spentCents)} \u00B7 NOT ALLOCATED"
                },
                color = palette.faint,
                style = PassType.monoMicro.copy(fontSize = 7.sp),
            )
        }
        if (line.allocatedCents != null) {
            Spacer(Modifier.height(6.dp))
            ProgressTrack(
                fraction = line.fraction,
                color = if (line.isOver) palette.danger else palette.alert,
                track = palette.panelLine,
                height = 4.dp,
            )
        }
        Spacer(Modifier.height(2.dp))
        Hairline(color = palette.boardLine)
    }
}

@Composable
private fun UncategorizedRow(spentCents: Long) {
    val palette = LocalPsychePalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Uncategorized",
                style = MaterialTheme.typography.titleSmall,
                color = palette.onGround,
            )
            MonoText(
                text = "SPENT WITHOUT A CATEGORY LINE",
                color = palette.faint,
                style = PassType.monoMicro.copy(fontSize = 7.sp),
            )
        }
        Text(
            text = Money.formatPeso(spentCents),
            style = PassType.monoData.copy(fontSize = 11.sp),
            color = palette.muted,
        )
    }
    Hairline(color = palette.boardLine)
}

@Composable
private fun AllocationFooter(view: MonthView) {
    val palette = LocalPsychePalette.current
    val cap = view.capCents
    val unallocated = view.unallocatedCents
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp),
        color = palette.ground,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            MonoText(
                text = if (cap != null) {
                    "ALLOCATED ${Money.formatPeso(view.allocatedCents)} OF ${Money.formatPeso(cap)}"
                } else {
                    "ALLOCATED ${Money.formatPeso(view.allocatedCents)}"
                },
                color = palette.faint,
                style = PassType.monoMicro.copy(fontSize = 7.sp),
            )
            Spacer(Modifier.weight(1f))
            if (unallocated != null) {
                Text(
                    text = "${Money.formatPeso(unallocated)} UNALLOCATED",
                    style = PassType.monoMicro.copy(fontSize = 7.sp),
                    color = if (unallocated < 0) palette.danger else palette.muted,
                )
            }
        }
    }
}
