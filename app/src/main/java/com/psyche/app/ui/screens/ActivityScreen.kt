package com.psyche.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.psyche.app.data.TransactionType
import com.psyche.app.ui.FinanceUiState
import com.psyche.app.ui.components.ActivityBoardRow
import com.psyche.app.ui.components.ActivityDayHeader
import com.psyche.app.ui.components.AppTopBar
import com.psyche.app.ui.components.BoardColumnHeader
import com.psyche.app.ui.components.ChoiceChip
import com.psyche.app.ui.components.EmptyPanel
import com.psyche.app.ui.components.Hairline
import com.psyche.app.ui.components.MonoText
import com.psyche.app.ui.components.MonthSwitcher
import com.psyche.app.ui.components.ScreenTitle
import com.psyche.app.ui.components.StatCell
import com.psyche.app.ui.components.groupByLocalDate
import com.psyche.app.ui.theme.LocalPsychePalette
import com.psyche.app.ui.theme.PassType
import com.psyche.app.util.ActivityRow
import com.psyche.app.util.BudgetStatus
import com.psyche.app.util.Formats
import com.psyche.app.util.Money

private enum class ActivityFilter(val label: String) {
    ALL("All"),
    EXPENSE("Expenses"),
    INCOME("Income"),
    TRANSFER("Transfers"),
}

@Composable
fun ActivityScreen(
    state: FinanceUiState,
    onOpenEntry: (ActivityRow) -> Unit,
    onSettings: () -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onResetMonth: () -> Unit,
    onCapture: () -> Unit,
) {
    val palette = LocalPsychePalette.current
    val view = state.activity
    var filterName by rememberSaveable { mutableStateOf(ActivityFilter.ALL.name) }
    val filter = ActivityFilter.valueOf(filterName)
    val filtered = view?.rows.orEmpty().filter { row ->
        when (filter) {
            ActivityFilter.ALL -> true
            ActivityFilter.EXPENSE -> row.entry.type == TransactionType.EXPENSE
            ActivityFilter.INCOME -> row.entry.type == TransactionType.INCOME
            ActivityFilter.TRANSFER -> row.entry.type == TransactionType.TRANSFER
        }
    }
    val subtitle = view?.let {
        "${Formats.monthShort(it.month)} \u00B7 ${it.summary.statusLabel}"
    } ?: "ACTIVITY"

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
                ScreenTitle(
                    eyebrow = "The record",
                    title = "ACTIVITY",
                    trailing = {
                        view?.let {
                            Text(
                                text = Formats.entryCount(it.summary.entryCount),
                                style = PassType.monoMicro.copy(letterSpacing = 0.9.sp),
                                color = palette.faint,
                            )
                        }
                    },
                )
                Spacer(Modifier.height(12.dp))
                MonthSwitcher(
                    month = view?.month ?: state.activityMonth,
                    isCurrent = view?.summary?.isCurrentMonth == true,
                    onPrevious = onPreviousMonth,
                    onNext = onNextMonth,
                    onReset = onResetMonth,
                )
                Spacer(Modifier.height(14.dp))
            }
            item {
                if (view != null) {
                    ActivitySummary(view)
                }
                Spacer(Modifier.height(14.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    ActivityFilter.entries.forEach { option ->
                        ChoiceChip(
                            label = option.label,
                            selected = option == filter,
                            onClick = { filterName = option.name },
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                BoardColumnHeader()
                Hairline(color = palette.boardLine)
            }
            if (filtered.isEmpty()) {
                item {
                    Spacer(Modifier.height(18.dp))
                    EmptyPanel(
                        title = when {
                            view?.rows.isNullOrEmpty() -> "This month is quiet"
                            filter != ActivityFilter.ALL -> "Nothing matches this filter"
                            else -> "No activity"
                        },
                        body = when {
                            view == null || view.rows.isEmpty() ->
                                "Log an expense or income and it joins the board here, with the running budget after each row."
                            filter != ActivityFilter.ALL ->
                                "Switch back to All to see the rest of the month."
                            else -> "No entries in this month."
                        },
                        actionLabel = if (filter != ActivityFilter.ALL) {
                            "Show all"
                        } else {
                            "Log expense"
                        },
                        onAction = {
                            if (filter != ActivityFilter.ALL) {
                                filterName = ActivityFilter.ALL.name
                            } else {
                                onCapture()
                            }
                        },
                    )
                }
            } else {
                filtered.groupByLocalDate().forEach { (date, rows) ->
                    item(key = "day-$date") {
                        ActivityDayHeader(
                            date = date,
                            today = state.today,
                            daySpentCents = rows
                                .filter { it.entry.type == TransactionType.EXPENSE }
                                .sumOf { it.entry.amountCents },
                        )
                    }
                    items(rows.size, key = { index -> "entry-${rows[index].entry.id}" }) { index ->
                        val row = rows[index]
                        ActivityBoardRow(
                            row = row,
                            today = state.today,
                            onClick = { onOpenEntry(row) },
                        )
                    }
                }
            }
            item {
                Spacer(Modifier.height(28.dp))
                MonoText(
                    text = "RUNNING LEFT IS DERIVED FROM THE SAME LEDGER AS THE PASS. TRANSFERS AND GOAL CONTRIBUTIONS DO NOT COUNT AS EXPENSES.",
                    color = palette.faint,
                    style = PassType.monoMicro.copy(fontSize = 7.sp, letterSpacing = 0.5.sp),
                    maxLines = 3,
                )
            }
        }
    }
}

@Composable
private fun ActivitySummary(view: com.psyche.app.ui.MonthView) {
    val palette = LocalPsychePalette.current
    val summary = view.summary
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = palette.boardRow,
        border = BorderStroke(1.dp, palette.boardLine),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                StatCell(
                    label = "SPENT",
                    value = Money.formatPeso(summary.spentCents),
                    modifier = Modifier.weight(1f),
                )
                Divider()
                StatCell(
                    label = "INCOME",
                    value = Money.formatPeso(summary.incomeCents),
                    modifier = Modifier.weight(1f),
                )
                Divider()
                StatCell(
                    label = if (summary.status == BudgetStatus.OVER) "OVER BY" else "LEFT",
                    value = when {
                        summary.status == BudgetStatus.OVER -> Money.formatPeso(summary.overByCents)
                        summary.remainingCents != null -> Money.formatPeso(summary.remainingCents)
                        else -> "\u2014"
                    },
                    valueColor = when {
                        summary.status == BudgetStatus.OVER -> palette.danger
                        else -> palette.onGround
                    },
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(10.dp))
            Hairline(color = palette.boardLine)
            Spacer(Modifier.height(10.dp))
            Row {
                MonoText(
                    text = if (summary.capCents != null) {
                        "CAP ${Money.formatPeso(summary.capCents)}"
                    } else {
                        "NO CAP SET"
                    },
                    color = palette.faint,
                    style = PassType.monoMicro.copy(fontSize = 7.sp),
                )
                Spacer(Modifier.weight(1f))
                MonoText(
                    text = if (summary.updatedAt != null) {
                        "LAST ROW ${Formats.time(summary.updatedAt)}"
                    } else {
                        "NO ROWS"
                    },
                    color = palette.faint,
                    style = PassType.monoMicro.copy(fontSize = 7.sp),
                )
            }
        }
    }
}

@Composable
private fun Divider() {
    val palette = LocalPsychePalette.current
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .height(30.dp)
            .width(1.dp)
            .background(palette.boardLine),
    )
}
