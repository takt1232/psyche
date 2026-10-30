package com.psyche.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.psyche.app.R
import com.psyche.app.ui.MainTab
import com.psyche.app.ui.theme.LocalPsychePalette
import com.psyche.app.ui.theme.PassType
import com.psyche.app.util.Formats
import com.psyche.app.util.Money
import java.time.YearMonth
import java.util.Locale

@Composable
fun AppTopBar(
    subtitle: String,
    onSettings: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val palette = LocalPsychePalette.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(start = 20.dp, end = 12.dp, top = 10.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "PSYCHE",
            style = PassType.brand,
            color = palette.onGround,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = subtitle,
            style = PassType.monoDataSmall.copy(letterSpacing = 0.7.sp),
            color = palette.muted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (onSettings != null) {
            Spacer(Modifier.size(4.dp))
            IconButton(
                onClick = onSettings,
                modifier = Modifier.size(44.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_tune),
                    contentDescription = "Settings",
                    tint = palette.onGround,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
fun ScreenTitle(
    eyebrow: String,
    title: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    val palette = LocalPsychePalette.current
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Bottom,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = eyebrow.uppercase(Locale.US),
                style = PassType.monoLabel.copy(fontSize = 9.sp, letterSpacing = 1.4.sp),
                color = palette.muted,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = title,
                style = PassType.sectionTitle.copy(fontSize = 24.sp, letterSpacing = 1.4.sp),
                color = palette.onGround,
            )
        }
        trailing?.invoke()
    }
}

@Composable
fun Hairline(
    color: Color = LocalPsychePalette.current.boardLine,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(color),
    )
}

@Composable
fun DashedLine(
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp),
    ) {
        drawLine(
            color = color,
            start = Offset(0f, size.height / 2f),
            end = Offset(size.width, size.height / 2f),
            strokeWidth = 1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(
                floatArrayOf(3.dp.toPx(), 3.dp.toPx()),
            ),
        )
    }
}

@Composable
fun SectionHeading(
    title: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    val palette = LocalPsychePalette.current
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title.uppercase(Locale.US),
            style = PassType.sectionTitle,
            color = palette.onGround,
            modifier = Modifier.weight(1f),
        )
        trailing?.invoke()
    }
}

@Composable
fun MonoText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = LocalPsychePalette.current.muted,
    style: TextStyle = PassType.monoDataSmall,
    maxLines: Int = 1,
    textAlign: TextAlign? = null,
) {
    Text(
        text = text,
        style = style,
        color = color,
        modifier = modifier,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        textAlign = textAlign,
    )
}

@Composable
fun Stamp(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = LocalPsychePalette.current.muted,
    borderColor: Color = LocalPsychePalette.current.panelLine,
    filled: Color? = null,
) {
    val palette = LocalPsychePalette.current
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(2.dp),
        color = filled ?: Color.Transparent,
        border = if (filled == null) BorderStroke(1.dp, borderColor) else null,
    ) {
        Text(
            text = text.uppercase(Locale.US),
            style = PassType.monoMicro.copy(fontSize = 8.sp, letterSpacing = 1.1.sp),
            color = if (filled != null) palette.onAlert else color,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
            maxLines = 1,
        )
    }
}

@Composable
fun AutoSizeText(
    text: String,
    style: TextStyle,
    minFontSize: TextUnit,
    maxFontSize: TextUnit,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    maxLines: Int = 1,
    textAlign: TextAlign? = null,
) {
    val measurer = rememberTextMeasurer()
    val fontScale = androidx.compose.ui.platform.LocalDensity.current.fontScale
    BoxWithConstraints(modifier = modifier) {
        val widthPx = constraints.maxWidth
        val fontSize = remember(text, widthPx, style, minFontSize, maxFontSize, fontScale) {
            var candidate = maxFontSize.value
            var chosen = minFontSize.value
            while (candidate >= minFontSize.value) {
                val measured = measurer.measure(
                    text = AnnotatedString(text),
                    style = style.copy(
                        fontSize = candidate.sp,
                        lineHeight = (candidate * 1.02f).sp,
                    ),
                    maxLines = maxLines,
                    softWrap = false,
                )
                if (measured.size.width <= widthPx) {
                    chosen = candidate
                    break
                }
                candidate -= 1f
            }
            chosen.sp
        }
        Text(
            text = text,
            style = style.copy(fontSize = fontSize, lineHeight = (fontSize.value * 1.02f).sp),
            color = color,
            maxLines = maxLines,
            softWrap = false,
            overflow = TextOverflow.Clip,
            textAlign = textAlign,
        )
    }
}

@Composable
fun MoneyField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    error: String? = null,
    enabled: Boolean = true,
    supporting: String? = null,
) {
    val palette = LocalPsychePalette.current
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(label, style = MaterialThemeLabel) },
        prefix = {
            Text(
                text = "\u20B1",
                style = PassType.monoData.copy(fontSize = 15.sp),
                color = palette.muted,
            )
        },
        singleLine = true,
        enabled = enabled,
        isError = error != null,
        supportingText = when {
            error != null -> {
                {
                    Text(
                        text = error,
                        style = PassType.monoDataSmall,
                        color = palette.danger,
                    )
                }
            }

            supporting != null -> {
                {
                    Text(
                        text = supporting,
                        style = PassType.monoDataSmall,
                        color = palette.muted,
                    )
                }
            }

            else -> null
        },
        textStyle = PassType.monoData.copy(fontSize = 17.sp),
        keyboardOptions = KeyboardOptions(
            keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal,
        ),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = palette.onGround,
            unfocusedTextColor = palette.onGround,
            disabledTextColor = palette.faint,
            cursorColor = palette.onGround,
            focusedBorderColor = palette.onGround,
            unfocusedBorderColor = palette.panelLine,
            disabledBorderColor = palette.panelLine,
            errorBorderColor = palette.danger,
            focusedLabelColor = palette.muted,
            unfocusedLabelColor = palette.muted,
            disabledLabelColor = palette.faint,
            errorLabelColor = palette.danger,
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
            errorContainerColor = Color.Transparent,
        ),
    )
}

private val MaterialThemeLabel = TextStyle(
    fontSize = 12.sp,
    fontWeight = FontWeight.Medium,
    letterSpacing = 0.3.sp,
)

@Composable
fun ChoiceChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val palette = LocalPsychePalette.current
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = 38.dp),
        shape = RoundedCornerShape(50),
        color = if (selected) palette.alert else Color.Transparent,
        contentColor = if (selected) palette.onAlert else if (enabled) palette.onGround else palette.faint,
        border = BorderStroke(
            1.dp,
            if (selected) palette.alert else palette.panelLine,
        ),
    ) {
        Text(
            text = label.uppercase(Locale.US),
            style = PassType.monoLabel.copy(fontSize = 9.sp),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            maxLines = 1,
        )
    }
}

@Composable
fun TopFeedbackBanner(
    visible: Boolean,
    message: String,
    actionLabel: String?,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalPsychePalette.current
    AnimatedVisibility(
        visible = visible,
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(start = 16.dp, end = 16.dp, top = 8.dp),
        enter = slideInVertically(animationSpec = tween(220)) { -it } + fadeIn(tween(180)),
        exit = slideOutVertically(animationSpec = tween(180)) { -it } + fadeOut(tween(140)),
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = palette.panel,
            contentColor = palette.onPanel,
            border = BorderStroke(1.dp, palette.accentText.copy(alpha = 0.45f)),
            shadowElevation = 12.dp,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(palette.accentText, RoundedCornerShape(7.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_check),
                        contentDescription = null,
                        tint = if (palette.isDark) palette.onAlert else palette.pass,
                        modifier = Modifier.size(17.dp),
                    )
                }
                Spacer(Modifier.size(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    MonoText(
                        text = "NOTICE",
                        color = palette.accentText,
                        style = PassType.monoMicro.copy(fontSize = 7.sp, letterSpacing = 1.1.sp),
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = message,
                        style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                        color = palette.onPanel,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (actionLabel != null) {
                    Spacer(Modifier.size(6.dp))
                    TextButton(onClick = onAction) {
                        Text(
                            text = actionLabel.uppercase(Locale.US),
                            style = PassType.monoLabel,
                            color = palette.accentText,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PrimaryAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentColor: Color = LocalPsychePalette.current.onAlert,
) {
    val palette = LocalPsychePalette.current
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = 52.dp),
        enabled = enabled,
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = palette.alert,
            contentColor = contentColor,
            disabledContainerColor = palette.panelLine,
            disabledContentColor = palette.faint,
        ),
    ) {
        Text(
            text = text.uppercase(Locale.US),
            style = PassType.monoData.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp),
        )
    }
}

@Composable
fun SecondaryAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val palette = LocalPsychePalette.current
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = 48.dp),
        enabled = enabled,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, palette.panelLine),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = palette.onGround,
            disabledContentColor = palette.faint,
        ),
    ) {
        Text(
            text = text.uppercase(Locale.US),
            style = PassType.monoLabel.copy(fontSize = 9.sp),
            modifier = Modifier.padding(horizontal = 4.dp),
        )
    }
}

@Composable
fun QuietAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = LocalPsychePalette.current.onGround,
) {
    TextButton(onClick = onClick, modifier = modifier) {
        Text(
            text = text.uppercase(Locale.US),
            style = PassType.monoLabel.copy(fontSize = 9.sp),
            color = color,
        )
    }
}

@Composable
fun DangerAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalPsychePalette.current
    TextButton(onClick = onClick, modifier = modifier) {
        Text(
            text = text.uppercase(Locale.US),
            style = PassType.monoLabel.copy(fontSize = 9.sp),
            color = palette.danger,
        )
    }
}

@Composable
fun ProgressTrack(
    fraction: Float,
    modifier: Modifier = Modifier,
    color: Color = LocalPsychePalette.current.alert,
    track: Color = LocalPsychePalette.current.panelLine,
    height: Dp = 5.dp,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(50))
            .background(track),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .height(height)
                .clip(RoundedCornerShape(50))
                .background(color),
        )
    }
}

@Composable
fun EmptyPanel(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    eyebrow: String = "NO ENTRIES",
) {
    val palette = LocalPsychePalette.current
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = palette.boardRow,
        border = BorderStroke(1.dp, palette.boardLine),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            MonoText(eyebrow, color = palette.faint)
            Spacer(Modifier.height(8.dp))
            Text(
                text = title,
                style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                color = palette.onGround,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = body,
                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                color = palette.muted,
            )
            if (actionLabel != null && onAction != null) {
                Spacer(Modifier.height(14.dp))
                SecondaryAction(text = actionLabel, onClick = onAction)
            }
        }
    }
}

@Composable
fun MonthSwitcher(
    month: YearMonth,
    isCurrent: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalPsychePalette.current
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onPrevious, modifier = Modifier.size(44.dp)) {
            Icon(
                painter = painterResource(R.drawable.ic_chevron_left),
                contentDescription = "Previous month",
                tint = palette.onGround,
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .clickable(role = Role.Button, onClick = onReset)
                .padding(vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = Formats.monthTitle(month),
                style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
                color = palette.onGround,
                maxLines = 1,
            )
            MonoText(
                text = if (isCurrent) "THIS MONTH" else "TAP TO RETURN",
                color = if (isCurrent) palette.accentText else palette.faint,
                style = PassType.monoMicro.copy(letterSpacing = 1.2.sp),
            )
        }
        IconButton(onClick = onNext, modifier = Modifier.size(44.dp)) {
            Icon(
                painter = painterResource(R.drawable.ic_chevron_right),
                contentDescription = "Next month",
                tint = palette.onGround,
            )
        }
    }
    Hairline(color = palette.boardLine)
}

@Composable
fun StatCell(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = LocalPsychePalette.current.onGround,
    valueStyle: TextStyle = PassType.monoData.copy(fontSize = 15.sp, fontWeight = FontWeight.Medium),
) {
    val palette = LocalPsychePalette.current
    Column(modifier = modifier) {
        MonoText(label, color = palette.faint, style = PassType.monoMicro.copy(letterSpacing = 1.1.sp))
        Spacer(Modifier.height(3.dp))
        Text(
            text = value,
            style = valueStyle,
            color = valueColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun PsycheBottomBar(
    selected: MainTab,
    onSelect: (MainTab) -> Unit,
) {
    val palette = LocalPsychePalette.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(palette.navContainer)
            .windowInsetsPadding(WindowInsets.navigationBars),
    ) {
        Hairline(color = palette.boardLine)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(66.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            MainTab.entries.forEach { tab ->
                val selectedTab = tab == selected
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .selectableTab(
                            selected = selectedTab,
                            onClick = { onSelect(tab) },
                        )
                        .padding(top = 9.dp, bottom = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        painter = painterResource(tab.icon),
                        contentDescription = null,
                        tint = if (selectedTab) palette.accentText else palette.navInactive,
                        modifier = Modifier.size(21.dp),
                    )
                    Spacer(Modifier.height(5.dp))
                    Text(
                        text = tab.label.uppercase(Locale.US),
                        style = PassType.monoMicro.copy(
                            fontSize = 8.sp,
                            letterSpacing = 1.1.sp,
                            fontWeight = if (selectedTab) FontWeight.Bold else FontWeight.Medium,
                        ),
                        color = if (selectedTab) palette.accentText else palette.navInactive,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

private fun Modifier.selectableTab(
    selected: Boolean,
    onClick: () -> Unit,
): Modifier = this.then(
    Modifier.clickable(
        role = Role.Tab,
        onClick = onClick,
    ),
)

@Composable
fun SheetHeader(
    eyebrow: String,
    title: String,
    onClose: () -> Unit,
) {
    val palette = LocalPsychePalette.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            MonoText(eyebrow, color = palette.faint, style = PassType.monoLabel)
            Spacer(Modifier.height(3.dp))
            Text(
                text = title,
                style = PassType.sectionTitle.copy(fontSize = 21.sp, letterSpacing = 1.2.sp),
                color = palette.onGround,
            )
        }
        IconButton(onClick = onClose, modifier = Modifier.size(44.dp)) {
            Icon(
                painter = painterResource(R.drawable.ic_close),
                contentDescription = "Close",
                tint = palette.muted,
            )
        }
    }
}

@Composable
fun KeyValueRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = LocalPsychePalette.current.onGround,
) {
    val palette = LocalPsychePalette.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MonoText(
            text = label,
            color = palette.faint,
            style = PassType.monoLabel.copy(fontSize = 8.sp),
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = PassType.monoData,
            color = valueColor,
            textAlign = TextAlign.End,
        )
    }
}

@Composable
fun IconAction(
    painter: Painter,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = LocalPsychePalette.current.onGround,
) {
    IconButton(onClick = onClick, modifier = modifier.size(44.dp)) {
        Icon(
            painter = painter,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
fun PassNotch(color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(18.dp)
            .clip(CircleShape)
            .background(color),
    )
}
