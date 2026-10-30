package com.psyche.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.psyche.app.data.TransactionType
import com.psyche.app.ui.theme.LocalPsychePalette
import com.psyche.app.ui.theme.PassType
import com.psyche.app.util.ActivityRow
import com.psyche.app.util.Formats
import com.psyche.app.util.Money
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@Composable
fun LiveActivitySection(
    rows: List<ActivityRow>,
    today: LocalDate,
    onRowClick: (ActivityRow) -> Unit,
    onLogExpense: () -> Unit,
    modifier: Modifier = Modifier,
    limit: Int = 8,
    showDayInMeta: Boolean = false,
) {
    val palette = LocalPsychePalette.current
    val updated = rows.maxOfOrNull { it.entry.occurredAt }
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "LIVE ACTIVITY",
                style = PassType.sectionTitle.copy(fontSize = 18.sp, letterSpacing = 2.2.sp),
                color = palette.onGround,
                modifier = Modifier.weight(1f),
            )
            MonoText(
                text = updated?.let { "UPDATED ${Formats.time(it)}" } ?: "NO ENTRIES YET",
                color = palette.faint,
                style = PassType.monoMicro.copy(letterSpacing = 0.8.sp),
            )
        }
        Spacer(Modifier.height(7.dp))
        BoardColumnHeader()
        Hairline(color = palette.boardLine)
        if (rows.isEmpty()) {
            Spacer(Modifier.height(10.dp))
            EmptyPanel(
                title = "No activity yet",
                body = "Log the first expense. The pass reprints, and the new row stays lit until you notice it.",
                actionLabel = "Log expense",
                onAction = onLogExpense,
            )
        } else {
            rows.take(limit).forEach { row ->
                ActivityBoardRow(
                    row = row,
                    today = today,
                    onClick = { onRowClick(row) },
                    showDayInMeta = showDayInMeta,
                )
            }
            if (rows.size > limit) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MonoText(
                        text = "+${rows.size - limit} more this month",
                        color = palette.faint,
                        style = PassType.monoMicro,
                    )
                    Spacer(Modifier.weight(1f))
                    MonoText(
                        text = "OPEN ACTIVITY",
                        color = palette.muted,
                        style = PassType.monoMicro.copy(letterSpacing = 0.9.sp),
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        MonoText(
            text = "A LIT ROW CLEARS WHEN OPENED OR WHEN THE NEXT CAPTURE LANDS.",
            color = palette.faint,
            style = PassType.monoMicro.copy(fontSize = 7.sp, letterSpacing = 0.6.sp),
        )
    }
}

@Composable
fun BoardColumnHeader(
    modifier: Modifier = Modifier,
    showDate: Boolean = false,
) {
    val palette = LocalPsychePalette.current
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val compact = maxWidth < 315.dp
        val timeWidth = when {
            showDate -> 78.dp
            compact -> 38.dp
            else -> 44.dp
        }
        val amountWidth = if (compact) 64.dp else 70.dp
        val leftWidth = if (compact) 50.dp else 56.dp
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, top = 7.dp, end = 8.dp, bottom = 7.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            BoardHeaderCell(
                text = if (showDate) "DATE" else "TIME",
                modifier = Modifier.width(timeWidth),
            )
            BoardHeaderCell(text = "DESCRIPTION", modifier = Modifier.weight(1f))
            BoardHeaderCell(
                text = "AMOUNT",
                modifier = Modifier.width(amountWidth),
                alignEnd = true,
            )
            BoardHeaderCell(
                text = "LEFT",
                modifier = Modifier.width(leftWidth),
                alignEnd = true,
            )
        }
    }
}

@Composable
private fun BoardHeaderCell(
    text: String,
    modifier: Modifier = Modifier,
    alignEnd: Boolean = false,
) {
    val palette = LocalPsychePalette.current
    Text(
        text = text,
        style = PassType.monoMicro.copy(fontSize = 7.sp, letterSpacing = 1.1.sp),
        color = palette.faint,
        modifier = modifier,
        maxLines = 1,
        textAlign = if (alignEnd) androidx.compose.ui.text.style.TextAlign.End else null,
    )
}

@Composable
fun ActivityBoardRow(
    row: ActivityRow,
    today: LocalDate,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showDate: Boolean = false,
    showDayInMeta: Boolean = false,
) {
    val palette = LocalPsychePalette.current
    val lit = row.entry.needsAttention
    val background by animateColorAsState(
        targetValue = if (lit) palette.alert else Color.Transparent,
        animationSpec = tween(durationMillis = 260),
        label = "rowLight",
    )
    val foreground = if (lit) palette.onAlert else palette.onGround
    val secondary = if (lit) palette.onAlert.copy(alpha = 0.68f) else palette.muted
    val amountColor = when {
        lit -> palette.onAlert
        row.entry.type == TransactionType.TRANSFER -> palette.muted
        else -> palette.onGround
    }
    val left = row.leftAfterCents
    val leftColor = when {
        lit -> palette.onAlert
        left != null && left < 0 -> palette.danger
        else -> palette.muted
    }
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val compact = maxWidth < 315.dp
        val timeWidth = when {
            showDate -> 78.dp
            compact -> 38.dp
            else -> 44.dp
        }
        val amountWidth = if (compact) 64.dp else 70.dp
        val leftWidth = if (compact) 50.dp else 56.dp
        val entryDate = Instant.ofEpochMilli(row.entry.occurredAt)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
        Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(background)
                .clickable(role = Role.Button, onClick = onClick)
                .heightIn(min = 52.dp)
                .padding(start = 4.dp, top = 7.dp, end = 8.dp, bottom = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (showDate) {
                Column(modifier = Modifier.width(timeWidth)) {
                    Text(
                        text = Formats.dayLabel(row.entry.occurredAt, today),
                        style = PassType.monoDataSmall.copy(fontSize = 8.sp),
                        color = foreground,
                        maxLines = 1,
                    )
                    Text(
                        text = Formats.time(row.entry.occurredAt),
                        style = PassType.monoMicro.copy(fontSize = 7.sp),
                        color = secondary,
                        maxLines = 1,
                    )
                }
            } else {
                Column(modifier = Modifier.width(timeWidth)) {
                    Text(
                        text = Formats.clock(row.entry.occurredAt),
                        style = PassType.monoDataSmall.copy(fontSize = 9.sp),
                        color = foreground,
                        maxLines = 1,
                    )
                    Text(
                        text = Formats.timePeriod(row.entry.occurredAt),
                        style = PassType.monoMicro.copy(fontSize = 7.sp),
                        color = secondary,
                        maxLines = 1,
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = row.displayDescription,
                        style = androidx.compose.material3.MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                        ),
                        color = foreground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (lit) {
                        Spacer(Modifier.width(6.dp))
                        NowChip()
                    }
                }
                Spacer(Modifier.height(2.dp))
                val meta = buildString {
                    if (showDayInMeta && entryDate != today) {
                        append(Formats.dayLabel(row.entry.occurredAt, today))
                        append(" \u00B7 ")
                    }
                    append(row.metaLine)
                }
                Text(
                    text = meta,
                    style = PassType.monoMicro.copy(fontSize = 7.sp, letterSpacing = 0.4.sp),
                    color = secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(4.dp))
            Text(
                text = Money.formatTable(row.entry.amountCents, row.marker),
                style = PassType.monoData.copy(
                    fontSize = 10.sp,
                    fontWeight = if (lit) FontWeight.Bold else FontWeight.Medium,
                ),
                color = amountColor,
                modifier = Modifier.width(amountWidth),
                maxLines = 1,
                textAlign = androidx.compose.ui.text.style.TextAlign.End,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = left?.let { Money.formatPeso(it, showSymbol = false) } ?: "\u2014",
                style = PassType.monoDataSmall.copy(fontSize = 9.sp),
                color = leftColor,
                modifier = Modifier.width(leftWidth),
                maxLines = 1,
                textAlign = androidx.compose.ui.text.style.TextAlign.End,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Hairline(color = if (lit) palette.onAlert.copy(alpha = 0.16f) else palette.boardLine)
        }
    }
}

@Composable
fun NowChip() {
    val palette = LocalPsychePalette.current
    Surface(
        shape = RoundedCornerShape(2.dp),
        color = palette.onAlert,
        contentColor = palette.alert,
    ) {
        Text(
            text = "NOW",
            style = PassType.monoMicro.copy(
                fontSize = 7.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
            ),
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
        )
    }
}

@Composable
fun ActivityDayHeader(
    date: LocalDate,
    today: LocalDate,
    daySpentCents: Long,
    modifier: Modifier = Modifier,
) {
    val palette = LocalPsychePalette.current
    val label = when (date) {
        today -> "TODAY"
        today.minusDays(1) -> "YESTERDAY"
        else -> date.format(java.time.format.DateTimeFormatter.ofPattern("EEE, d MMM")).uppercase()
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(palette.ground)
            .padding(top = 16.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = PassType.monoMicro.copy(
                fontSize = 8.sp,
                letterSpacing = 1.2.sp,
                fontWeight = FontWeight.Medium,
            ),
            color = palette.muted,
            modifier = Modifier.weight(1f),
        )
        MonoText(
            text = "SPENT ${Money.formatPeso(daySpentCents, showSymbol = true)}",
            color = palette.faint,
            style = PassType.monoMicro.copy(fontSize = 7.sp),
        )
    }
    Hairline(color = palette.boardLine)
}

fun List<ActivityRow>.groupByLocalDate(): List<Pair<LocalDate, List<ActivityRow>>> {
    val zone = ZoneId.systemDefault()
    return groupBy { row ->
        Instant.ofEpochMilli(row.entry.occurredAt).atZone(zone).toLocalDate()
    }.toList().sortedByDescending { it.first }
}
