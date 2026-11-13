package com.psyche.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.psyche.app.data.MAX_QUICK_ACTIONS
import com.psyche.app.ui.theme.LocalPsychePalette
import com.psyche.app.ui.theme.PassType
import com.psyche.app.util.Formats
import com.psyche.app.util.Money
import com.psyche.app.util.PositionStatus
import com.psyche.app.util.PositionSummary
import java.time.LocalDate
import java.util.Locale

@Composable
fun BalancePassCard(
    position: PositionSummary,
    today: LocalDate,
    quickActionCount: Int,
    onCopySummary: () -> Unit,
    onAddAccount: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalPsychePalette.current
    val negative = position.netCents < 0
    val statusColor = when (position.status) {
        PositionStatus.SHORT -> palette.dangerOnPass
        PositionStatus.NO_ACCOUNTS -> palette.passMuted
        else -> palette.passInk
    }
    val passShape = RoundedCornerShape(16.dp)

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = passShape,
        color = palette.pass,
        contentColor = palette.passInk,
        border = if (palette.isDark) null else BorderStroke(1.dp, palette.panelLine),
        shadowElevation = if (palette.isDark) 18.dp else 2.dp,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            PassTopStrip(
                asOf = Formats.shortDate(today),
            )
            Hairline(color = palette.passLine)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "NET POSITION",
                        style = PassType.monoLabel.copy(letterSpacing = 1.2.sp),
                        color = palette.passInk,
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = "${position.accountCount} ACTIVE ${if (position.accountCount == 1) "ACCOUNT" else "ACCOUNTS"}",
                        style = PassType.monoMicro.copy(letterSpacing = 1.sp),
                        color = palette.passMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    AutoSizeText(
                        text = Money.formatPeso(position.netCents),
                        style = PassType.passAmount,
                        minFontSize = 25.sp,
                        maxFontSize = 58.sp,
                        color = if (negative) palette.dangerOnPass else palette.passInk,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "MONEY",
                            style = PassType.monoMicro.copy(letterSpacing = 1.2.sp),
                            color = palette.passMuted,
                        )
                        Text(
                            text = Money.formatPeso(position.moneyCents),
                            style = PassType.monoData,
                            color = palette.passInk,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "OWED",
                            style = PassType.monoMicro.copy(letterSpacing = 1.2.sp),
                            color = palette.passMuted,
                        )
                        Text(
                            text = Money.formatPeso(position.owedCents),
                            style = PassType.monoData.copy(fontWeight = FontWeight.Medium),
                            color = if (position.owedCents > 0) palette.dangerOnPass else palette.passInk,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                if (position.owedCents > 0) {
                    Spacer(Modifier.height(9.dp))
                    val coverage = ((position.moneyCents.toDouble() / position.owedCents.toDouble()) * 100.0)
                    val fraction = (coverage / 100.0).toFloat().coerceIn(0f, 1f)
                    ProgressTrack(
                        fraction = fraction,
                        color = if (negative) palette.dangerOnPass else palette.alert,
                        track = palette.passLine,
                        height = 5.dp,
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "DEBT COVERAGE",
                            style = PassType.monoMicro.copy(letterSpacing = 1.sp),
                            color = palette.passMuted,
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            text = String.format(Locale.US, "%.0f%%", coverage),
                            style = PassType.monoDataSmall.copy(fontWeight = FontWeight.Medium),
                            color = if (negative) palette.dangerOnPass else palette.passInk,
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                PassSegments(
                    cash = Money.formatPeso(position.cashCents),
                    saved = Money.formatPeso(position.savingsCents),
                    status = position.status.label,
                    statusColor = statusColor,
                )
                if (position.accountCount == 0) {
                    Spacer(Modifier.height(12.dp))
                    PrimaryAction(
                        text = "Add account",
                        onClick = onAddAccount,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            SnapshotFooter(
                reference = position.snapshotRef,
                quickActionCount = quickActionCount,
                onCopy = onCopySummary,
            )
        }
    }
}

@Composable
private fun PassTopStrip(asOf: String) {
    val palette = LocalPsychePalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "BALANCE PASS",
            style = PassType.monoLabel.copy(letterSpacing = 1.6.sp),
            color = palette.passInk,
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = "AS OF $asOf",
            style = PassType.monoDataSmall.copy(letterSpacing = 0.8.sp),
            color = palette.passMuted,
        )
    }
}

@Composable
private fun PassSegments(
    cash: String,
    saved: String,
    status: String,
    statusColor: Color,
) {
    val palette = LocalPsychePalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PassSegment(
            label = "CASH",
            value = cash,
            modifier = Modifier.weight(1f),
            valueColor = palette.passInk,
        )
        PassDivider()
        PassSegment(
            label = "SAVED",
            value = saved,
            modifier = Modifier.weight(1.15f),
            valueColor = palette.passInk,
        )
        PassDivider()
        PassSegment(
            label = "STATUS",
            value = status,
            modifier = Modifier.weight(1.35f),
            valueColor = statusColor,
        )
    }
}

@Composable
private fun PassSegment(
    label: String,
    value: String,
    modifier: Modifier,
    valueColor: Color,
) {
    val palette = LocalPsychePalette.current
    Column(
        modifier = modifier.padding(horizontal = 2.dp),
    ) {
        Text(
            text = label,
            style = PassType.monoMicro.copy(letterSpacing = 1.1.sp),
            color = palette.passMuted,
        )
        Spacer(Modifier.height(2.dp))
        AutoSizeText(
            text = value,
            style = PassType.monoData.copy(fontSize = 15.sp, fontWeight = FontWeight.Medium),
            minFontSize = 10.sp,
            maxFontSize = 15.sp,
            color = valueColor,
        )
    }
}

@Composable
private fun PassDivider() {
    val palette = LocalPsychePalette.current
    Box(
        modifier = Modifier
            .height(30.dp)
            .width(1.dp)
            .background(palette.passLine),
    )
}

@Composable
private fun SnapshotFooter(
    reference: String,
    quickActionCount: Int,
    onCopy: () -> Unit,
) {
    val palette = LocalPsychePalette.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onCopy)
            .semantics {
                contentDescription =
                    "Snapshot reference $reference. Tap to copy the balance summary."
            },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
        ) {
            DashedLine(color = palette.passLine)
            Spacer(Modifier.height(9.dp))
            ReferenceBarcode(
                reference = reference,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp),
            )
            Spacer(Modifier.height(7.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "SNAPSHOT REF",
                        style = PassType.monoMicro.copy(letterSpacing = 1.1.sp),
                        color = palette.passMuted,
                    )
                    Text(
                        text = reference,
                        style = PassType.monoData.copy(fontSize = 10.sp),
                        color = palette.passInk,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .background(
                                    if (quickActionCount > 0) palette.alert else palette.passLine,
                                    shape = RoundedCornerShape(50),
                                ),
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = if (quickActionCount > 0) {
                                "ONE-TAP $quickActionCount/$MAX_QUICK_ACTIONS"
                            } else {
                                "NO ONE-TAP YET"
                            },
                            style = PassType.monoMicro.copy(letterSpacing = 0.7.sp),
                            color = palette.passMuted,
                        )
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "TAP REF TO COPY",
                        style = PassType.monoMicro.copy(fontSize = 7.sp),
                        color = palette.passMuted,
                    )
                }
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = (-9).dp, y = (-9).dp)
                .size(18.dp)
                .background(palette.ground, RoundedCornerShape(50)),
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 9.dp, y = (-9).dp)
                .size(18.dp)
                .background(palette.ground, RoundedCornerShape(50)),
        )
    }
}

@Composable
fun ReferenceBarcode(
    reference: String,
    modifier: Modifier = Modifier,
) {
    val palette = LocalPsychePalette.current
    Canvas(modifier = modifier) {
        val modules = buildList {
            reference.forEach { character ->
                val code = character.code
                for (bit in 6 downTo 0) {
                    add((code shr bit) and 1)
                }
                add(1)
            }
        }
        val quiet = size.width * 0.01f
        val unit = (size.width - quiet * 2f) / modules.size.toFloat()
        var x = quiet
        modules.forEachIndexed { index, bit ->
            if (bit == 1) {
                val wide = index % 5 == 0
                val barWidth = if (wide) unit * 1.7f else unit
                drawRect(
                    color = palette.passInk,
                    topLeft = Offset(x, 0f),
                    size = Size(barWidth.coerceAtLeast(1f), size.height),
                )
            }
            x += unit
        }
    }
}
