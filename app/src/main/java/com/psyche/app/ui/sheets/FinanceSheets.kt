package com.psyche.app.ui.sheets

import android.app.DatePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.psyche.app.R
import com.psyche.app.data.Account
import com.psyche.app.data.AccountType
import com.psyche.app.data.Category
import com.psyche.app.data.LedgerEntry
import com.psyche.app.data.MAX_QUICK_ACTIONS
import com.psyche.app.data.QuickTransaction
import com.psyche.app.data.SavingsGoal
import com.psyche.app.data.ThemeMode
import com.psyche.app.data.TransactionType
import com.psyche.app.ui.FinanceUiState
import com.psyche.app.ui.FinanceViewModel
import com.psyche.app.ui.components.AutoSizeText
import com.psyche.app.ui.components.ChoiceChip
import com.psyche.app.ui.components.DangerAction
import com.psyche.app.ui.components.Hairline
import com.psyche.app.ui.components.KeyValueRow
import com.psyche.app.ui.components.MonoText
import com.psyche.app.ui.components.MoneyField
import com.psyche.app.ui.components.PrimaryAction
import com.psyche.app.ui.components.ProgressTrack
import com.psyche.app.ui.components.QuietAction
import com.psyche.app.ui.components.SecondaryAction
import com.psyche.app.ui.components.SheetHeader
import com.psyche.app.ui.components.Stamp
import com.psyche.app.ui.theme.LocalPsychePalette
import com.psyche.app.ui.theme.PassType
import com.psyche.app.util.AccountBalance
import com.psyche.app.util.ActivityRow
import com.psyche.app.util.BudgetMath
import com.psyche.app.util.Formats
import com.psyche.app.util.GoalProgress
import com.psyche.app.util.Money
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId
import java.util.Locale
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SheetFrame(
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val palette = LocalPsychePalette.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = palette.ground,
        contentColor = palette.onGround,
        shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 4.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .background(palette.panelLine, RoundedCornerShape(50)),
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
            content = content,
        )
    }
}

@Composable
fun BudgetEditorSheet(
    viewModel: FinanceViewModel,
    month: YearMonth,
    capCents: Long?,
    previousCapCents: Long?,
    spentCents: Long,
    onDismiss: () -> Unit,
) {
    val palette = LocalPsychePalette.current
    val scope = rememberCoroutineScope()
    var amount by rememberSaveable { mutableStateOf(capCents?.let { Money.inputValue(it) } ?: "") }
    var error by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    val parsed = Money.parse(amount)

    SheetFrame(onDismiss = onDismiss) {
        SheetHeader(
            eyebrow = Formats.monthYear(month),
            title = if (capCents == null) "SET THE CAP" else "EDIT THE CAP",
            onClose = onDismiss,
        )
        Spacer(Modifier.height(16.dp))
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = palette.boardRow,
            border = BorderStroke(1.dp, palette.boardLine),
        ) {
            Column(Modifier.padding(12.dp)) {
                MonoText(
                    text = "ALREADY SPENT THIS MONTH",
                    color = palette.faint,
                    style = PassType.monoMicro.copy(letterSpacing = 0.9.sp),
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = Money.formatPeso(spentCents),
                    style = PassType.monoData.copy(fontSize = 16.sp),
                    color = palette.onGround,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "The cap is the ceiling for expenses in this month. Income and transfers do not change it.",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.muted,
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        MoneyField(
            label = "Monthly cap",
            value = amount,
            onValueChange = {
                amount = it
                error = null
            },
            error = error,
            supporting = "PHP \u00B7 up to \u20B1999,999.99",
        )
        if (previousCapCents != null && capCents == null) {
            Spacer(Modifier.height(8.dp))
            SecondaryAction(
                text = "Use previous cap \u00B7 ${Money.formatPeso(previousCapCents)}",
                onClick = { amount = Money.inputValue(previousCapCents) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(Modifier.height(18.dp))
        PrimaryAction(
            text = if (saving) "Saving\u2026" else "Save cap",
            onClick = {
                val cents = parsed
                if (cents == null) {
                    error = "Enter an amount between \u20B10.01 and \u20B1999,999.99."
                    return@PrimaryAction
                }
                saving = true
                error = null
                scope.launch {
                    viewModel.saveBudget(month, cents)
                        .onSuccess { onDismiss() }
                        .onFailure { error = it.message ?: "Couldn't save the cap." }
                    saving = false
                }
            },
            enabled = !saving && parsed != null,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        MonoText(
            text = "CHANGES REPRINT THE PASS IMMEDIATELY.",
            color = palette.faint,
            style = PassType.monoMicro.copy(fontSize = 7.sp),
        )
    }
}

@Composable
fun AllocationsSheet(
    viewModel: FinanceViewModel,
    month: YearMonth,
    capCents: Long,
    categories: List<Category>,
    allocations: Map<Long, Long>,
    onManageCategories: () -> Unit,
    onDismiss: () -> Unit,
) {
    val palette = LocalPsychePalette.current
    val scope = rememberCoroutineScope()
    val values = remember(categories, allocations) {
        categories.associate { category ->
            category.id to (allocations[category.id]?.let { Money.inputValue(it) } ?: "")
        }.toMutableMap()
    }
    var fields by remember(categories, allocations) {
        mutableStateOf<Map<Long, String>>(values)
    }
    var error by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    val parsed = fields.mapValues { (_, value) -> if (value.isBlank()) 0L else Money.parse(value) }
    val hasInvalid = parsed.values.any { it == null }
    val total = parsed.values.filterNotNull().sum()
    val over = total > capCents

    SheetFrame(onDismiss = onDismiss) {
        SheetHeader(
            eyebrow = Formats.monthYear(month),
            title = "CATEGORY LINES",
            onClose = onDismiss,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = "Split the cap into optional category lines. Leave a line blank to keep the money unallocated.",
            style = MaterialTheme.typography.bodyMedium,
            color = palette.muted,
        )
        Spacer(Modifier.height(14.dp))
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = palette.boardRow,
            border = BorderStroke(1.dp, palette.boardLine),
        ) {
            Column(Modifier.padding(12.dp)) {
                Row {
                    MonoText(
                        text = "CAP ${Money.formatPeso(capCents)}",
                        color = palette.faint,
                        style = PassType.monoMicro,
                    )
                    Spacer(Modifier.weight(1f))
                    MonoText(
                        text = "UNALLOCATED ${Money.formatPeso((capCents - total).coerceAtLeast(0))}",
                        color = if (over) palette.danger else palette.muted,
                        style = PassType.monoMicro,
                    )
                }
                Spacer(Modifier.height(6.dp))
                ProgressTrack(
                    fraction = total.toFloat() / capCents.toFloat(),
                    color = if (over) palette.danger else palette.alert,
                    track = palette.panelLine,
                    height = 5.dp,
                )
                if (over) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Lines are ${Money.formatPeso(total - capCents)} over the cap.",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.danger,
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        if (categories.isEmpty()) {
            Text(
                text = "No categories yet. The cap works without them, or add a few lines now.",
                style = MaterialTheme.typography.bodyMedium,
                color = palette.muted,
            )
            Spacer(Modifier.height(10.dp))
            SecondaryAction(
                text = "Manage categories",
                onClick = onManageCategories,
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            categories.forEach { category ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = category.name,
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.onGround,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (category.archived) {
                            Text(
                                text = "ARCHIVED",
                                style = PassType.monoMicro.copy(fontSize = 7.sp),
                                color = palette.faint,
                            )
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    InlineMoneyField(
                        value = fields[category.id].orEmpty(),
                        onValueChange = { value ->
                            fields = fields + (category.id to value)
                            error = null
                        },
                        isError = fields[category.id]?.isNotBlank() == true &&
                            parsed[category.id] == null,
                    )
                }
            }
            if (error != null) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = error ?: "",
                    style = PassType.monoDataSmall,
                    color = palette.danger,
                )
            }
            Spacer(Modifier.height(16.dp))
            PrimaryAction(
                text = if (saving) "Saving\u2026" else "Save allocations",
                onClick = {
                    if (hasInvalid) {
                        error = "A line has an amount that isn't valid."
                        return@PrimaryAction
                    }
                    if (over) {
                        error = "Lower the lines so they fit inside the cap."
                        return@PrimaryAction
                    }
                    saving = true
                    scope.launch {
                        viewModel.saveAllocations(
                            month = month,
                            allocations = parsed.mapNotNull { (id, value) ->
                                value?.takeIf { it > 0 }?.let { id to it }
                            }.toMap(),
                        ).onSuccess { onDismiss() }
                            .onFailure { error = it.message ?: "Couldn't save allocations." }
                        saving = false
                    }
                },
                enabled = !saving && !hasInvalid && !over,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            QuietAction(
                text = "Manage categories",
                onClick = onManageCategories,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }
    }
}

@Composable
private fun InlineMoneyField(
    value: String,
    onValueChange: (String) -> Unit,
    isError: Boolean,
) {
    val palette = LocalPsychePalette.current
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.width(126.dp),
        singleLine = true,
        isError = isError,
        prefix = {
            Text(
                text = "\u20B1",
                style = PassType.monoDataSmall,
                color = palette.faint,
            )
        },
        textStyle = PassType.monoData.copy(fontSize = 12.sp),
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
            keyboardType = KeyboardType.Decimal,
        ),
        colors = fieldColors(),
    )
}

@Composable
fun CategoriesSheet(
    viewModel: FinanceViewModel,
    categories: List<Category>,
    onDismiss: () -> Unit,
) {
    val palette = LocalPsychePalette.current
    val scope = rememberCoroutineScope()
    var newName by rememberSaveable { mutableStateOf("") }
    var editingId by rememberSaveable { mutableStateOf(-1L) }
    var editingName by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }

    fun resetEdit() {
        editingId = -1L
        editingName = ""
    }

    SheetFrame(onDismiss = onDismiss) {
        SheetHeader(
            eyebrow = "The cap",
            title = "CATEGORIES",
            onClose = onDismiss,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Categories are optional labels for expenses. Archiving hides one from capture; its past entries remain.",
            style = MaterialTheme.typography.bodySmall,
            color = palette.muted,
        )
        Spacer(Modifier.height(14.dp))
        if (editingId >= 0) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = editingName,
                    onValueChange = { editingName = it.take(30) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium,
                    colors = fieldColors(),
                    placeholder = {
                        Text("Category name", style = MaterialTheme.typography.bodySmall)
                    },
                )
                Spacer(Modifier.width(8.dp))
                PrimaryAction(
                    text = if (saving) "\u2026" else "Save",
                    onClick = {
                        val category = categories.firstOrNull { it.id == editingId } ?: return@PrimaryAction
                        if (editingName.isBlank()) {
                            error = "Name the category."
                            return@PrimaryAction
                        }
                        saving = true
                        scope.launch {
                            viewModel.updateCategory(category.copy(name = editingName))
                                .onSuccess { resetEdit() }
                                .onFailure { error = it.message ?: "Couldn't rename." }
                            saving = false
                        }
                    },
                    enabled = !saving,
                )
            }
            Spacer(Modifier.height(4.dp))
            QuietAction(
                text = "Cancel",
                onClick = {
                    resetEdit()
                    error = null
                },
            )
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it.take(30) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium,
                    colors = fieldColors(),
                    placeholder = {
                        Text("New category", style = MaterialTheme.typography.bodySmall)
                    },
                )
                Spacer(Modifier.width(8.dp))
                PrimaryAction(
                    text = if (saving) "\u2026" else "Add",
                    onClick = {
                        if (newName.isBlank()) {
                            error = "Name the category."
                            return@PrimaryAction
                        }
                        saving = true
                        scope.launch {
                            viewModel.createCategory(newName)
                                .onSuccess { newName = "" }
                                .onFailure { error = it.message ?: "Couldn't add." }
                            saving = false
                        }
                    },
                    enabled = !saving,
                )
            }
        }
        if (error != null) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = error ?: "",
                style = PassType.monoDataSmall,
                color = palette.danger,
            )
        }
        Spacer(Modifier.height(18.dp))
        if (categories.isEmpty()) {
            MonoText(
                text = "NO CATEGORIES YET",
                color = palette.faint,
                style = PassType.monoLabel,
            )
        } else {
            categories.forEach { category ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = category.name,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (category.archived) palette.muted else palette.onGround,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (category.archived) {
                            Text(
                                text = "ARCHIVED",
                                style = PassType.monoMicro.copy(fontSize = 7.sp),
                                color = palette.faint,
                            )
                        }
                    }
                    if (!category.archived) {
                        Text(
                            text = "RENAME",
                            style = PassType.monoMicro.copy(fontSize = 7.sp),
                            color = palette.muted,
                            modifier = Modifier
                                .clickable(role = Role.Button) {
                                    editingId = category.id
                                    editingName = category.name
                                    error = null
                                }
                                .padding(8.dp),
                        )
                    }
                    Text(
                        text = if (category.archived) "RESTORE" else "ARCHIVE",
                        style = PassType.monoMicro.copy(fontSize = 7.sp),
                        color = if (category.archived) palette.accentText else palette.faint,
                        modifier = Modifier
                            .clickable(role = Role.Button) {
                                saving = true
                                scope.launch {
                                    val result = if (category.archived) {
                                        viewModel.restoreCategory(category)
                                    } else {
                                        viewModel.archiveCategory(category)
                                    }
                                    result.onFailure {
                                        error = it.message ?: "Couldn't update."
                                    }
                                    saving = false
                                }
                            }
                            .padding(8.dp),
                    )
                }
                Hairline(color = palette.boardLine)
            }
        }
    }
}

private fun parseAccountAmount(input: String): Long? {
    val cleaned = input.trim()
        .replace("\u20B1", "")
        .replace(",", "")
        .replace(" ", "")
    if (cleaned.isEmpty()) return 0L
    if (cleaned != "." && !cleaned.startsWith(".") && cleaned.all { it == '0' || it == '.' }) {
        return 0L
    }
    return Money.parse(cleaned)
}

@Composable
private fun NoteField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String = "Note (optional)",
    maxLength: Int = 80,
) {
    val palette = LocalPsychePalette.current
    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MonoText(
                text = label.uppercase(Locale.US),
                color = palette.faint,
                style = PassType.monoLabel.copy(letterSpacing = 1.1.sp),
            )
            Spacer(Modifier.weight(1f))
            MonoText(
                text = "${value.length}/$maxLength",
                color = if (value.length >= maxLength - 8) palette.accentText else palette.faint,
                style = PassType.monoMicro.copy(fontSize = 7.sp),
            )
        }
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(
            value = value,
            onValueChange = { onValueChange(it.take(maxLength)) },
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    text = "Optional",
                    style = MaterialTheme.typography.bodySmall,
                )
            },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium,
            colors = fieldColors(),
        )
    }
}

@Composable
private fun fieldColors(): TextFieldColors {
    val palette = LocalPsychePalette.current
    return OutlinedTextFieldDefaults.colors(
        focusedTextColor = palette.onGround,
        unfocusedTextColor = palette.onGround,
        cursorColor = palette.onGround,
        focusedBorderColor = palette.onGround,
        unfocusedBorderColor = palette.panelLine,
        errorBorderColor = palette.danger,
        focusedContainerColor = Color.Transparent,
        unfocusedContainerColor = Color.Transparent,
        errorContainerColor = Color.Transparent,
    )
}

@Composable
fun AccountEditorSheet(
    viewModel: FinanceViewModel,
    account: Account?,
    onDismiss: () -> Unit,
) {
    val palette = LocalPsychePalette.current
    val scope = rememberCoroutineScope()
    var name by rememberSaveable { mutableStateOf(account?.name ?: "") }
    var typeName by rememberSaveable { mutableStateOf((account?.type ?: AccountType.CASH).name) }
    var opening by rememberSaveable {
        val existing = account?.openingBalanceCents ?: 0L
        val initial = when {
            account?.type == AccountType.CREDIT && existing < 0 -> Money.inputValue(-existing)
            account?.type == AccountType.CREDIT -> ""
            existing > 0 -> Money.inputValue(existing)
            else -> ""
        }
        mutableStateOf(initial)
    }
    var openingTouched by rememberSaveable { mutableStateOf(false) }
    var limit by rememberSaveable {
        mutableStateOf(
            account?.creditLimitCents
                ?.takeIf { it > 0 }
                ?.let { Money.inputValue(it) }
                ?: "",
        )
    }
    var lastFour by rememberSaveable { mutableStateOf(account?.lastFour ?: "") }
    var error by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    val type = AccountType.valueOf(typeName)
    val isCredit = type == AccountType.CREDIT
    val isDefaultAccount = account?.isDefault == true
    var previousType by remember { mutableStateOf(type) }
    LaunchedEffect(type) {
        if (type != previousType) {
            opening = ""
            openingTouched = true
            previousType = type
        }
    }
    val openingParsed = parseAccountAmount(opening)
    val limitParsed = if (limit.isBlank()) 0L else Money.parse(limit)
    val lastFourValid = lastFour.isBlank() || lastFour.length == 4
    val valid = name.isNotBlank() &&
        openingParsed != null &&
        lastFourValid &&
        (!isCredit || (limitParsed != null && limitParsed > 0))

    SheetFrame(onDismiss = onDismiss) {
        SheetHeader(
            eyebrow = if (account == null) "New instrument" else "Instrument",
            title = if (account == null) "ADD ACCOUNT" else "EDIT ACCOUNT",
            onClose = onDismiss,
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = name,
            onValueChange = { name = it.take(30) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Account name", style = MaterialTheme.typography.bodySmall) },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium,
            colors = fieldColors(),
        )
        Spacer(Modifier.height(14.dp))
        MonoText(
            text = "TYPE",
            color = palette.faint,
            style = PassType.monoLabel.copy(letterSpacing = 1.1.sp),
        )
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            AccountType.entries.forEach { option ->
                ChoiceChip(
                    label = option.label,
                    selected = option == type,
                    enabled = !isDefaultAccount,
                    onClick = {
                        typeName = option.name
                        error = null
                    },
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = if (isDefaultAccount) {
                "This is the default Cash account. It stays Cash, stays on the pass, and cannot be archived; set its opening balance to the cash you actually hold."
            } else {
                when (type) {
                    AccountType.CASH -> "Physical wallet. Counts as cash on the pass."
                    AccountType.DEBIT_EWALLET -> "Bank, ATM, or e-wallet. Counts as cash."
                    AccountType.CREDIT -> "Spending counts against the cap. Paying the card is a transfer."
                }
            },
            style = MaterialTheme.typography.bodySmall,
            color = palette.muted,
        )
        Spacer(Modifier.height(14.dp))
        MoneyField(
            label = when {
                isCredit -> "Outstanding now (optional)"
                account == null -> "Opening balance (optional)"
                else -> "Opening balance"
            },
            value = opening,
            onValueChange = {
                opening = it
                openingTouched = true
                error = null
            },
            supporting = if (isCredit) {
                val prepaid = account?.openingBalanceCents?.takeIf { it > 0 && !openingTouched }
                if (prepaid != null) {
                    "This card has a ${Money.formatPeso(prepaid)} prepaid balance. Leave blank to keep it."
                } else {
                    "What you owe on the card today. Leave blank if it is paid off."
                }
            } else {
                "What the account holds now. Leave blank for \u20B10."
            },
            error = if (opening.isNotBlank() && openingParsed == null) {
                "Use a valid amount."
            } else {
                null
            },
        )
        if (isCredit) {
            Spacer(Modifier.height(10.dp))
            MoneyField(
                label = "Credit limit",
                value = limit,
                onValueChange = {
                    limit = it
                    error = null
                },
                supporting = if (limit.isBlank()) "Required for a credit account." else null,
                error = if (limit.isNotBlank() && limitParsed == null) "Use a valid limit." else null,
            )
        }
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = lastFour,
            onValueChange = { value ->
                lastFour = value.filter { it.isDigit() }.take(4)
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Card last four (optional)", style = MaterialTheme.typography.bodySmall) },
            singleLine = true,
            isError = !lastFourValid,
            supportingText = if (!lastFourValid) {
                {
                    Text(
                        text = "Enter all four digits, or leave it blank.",
                        style = PassType.monoDataSmall,
                        color = palette.danger,
                    )
                }
            } else {
                null
            },
            textStyle = PassType.monoData,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = KeyboardType.Number,
            ),
            colors = fieldColors(),
        )
        if (error != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = error ?: "",
                style = PassType.monoDataSmall,
                color = palette.danger,
            )
        }
        Spacer(Modifier.height(18.dp))
        PrimaryAction(
            text = when {
                saving -> "Saving\u2026"
                account == null -> "Add account"
                else -> "Save account"
            },
            onClick = {
                if (!valid) {
                    error = "Check the name and amounts."
                    return@PrimaryAction
                }
                val enteredOpening = openingParsed
                val keepPrepaid = isCredit &&
                    opening.isBlank() &&
                    !openingTouched &&
                    account?.type == AccountType.CREDIT &&
                    account.openingBalanceCents > 0
                val openCents = when {
                    keepPrepaid -> account.openingBalanceCents
                    isCredit -> -enteredOpening
                    else -> enteredOpening
                }
                val limitCents = if (isCredit) limitParsed ?: 0L else 0L
                saving = true
                error = null
                scope.launch {
                    val result = if (account == null) {
                        viewModel.createAccount(
                            name = name,
                            type = type,
                            openingBalanceCents = openCents,
                            creditLimitCents = limitCents,
                            lastFour = lastFour,
                        )
                    } else {
                        viewModel.updateAccount(
                            account.copy(
                                name = name,
                                type = type,
                                openingBalanceCents = openCents,
                                creditLimitCents = limitCents,
                                lastFour = lastFour.ifBlank { null },
                            ),
                        )
                    }
                    result.onSuccess { onDismiss() }
                        .onFailure { error = it.message ?: "Couldn't save the account." }
                    saving = false
                }
            },
            enabled = !saving && valid,
            modifier = Modifier.fillMaxWidth(),
        )
        if (account != null) {
            Spacer(Modifier.height(10.dp))
            Hairline(color = palette.boardLine)
            Spacer(Modifier.height(6.dp))
            if (account.isDefault) {
                MonoText(
                    text = "DEFAULT CASH ACCOUNT \u00B7 CANNOT BE ARCHIVED",
                    color = palette.faint,
                    style = PassType.monoMicro.copy(fontSize = 7.sp),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else if (account.archived) {
                SecondaryAction(
                    text = "Restore account",
                    onClick = {
                        scope.launch {
                            viewModel.restoreAccount(account).onSuccess { onDismiss() }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                DangerAction(
                    text = "Archive account",
                    onClick = {
                        scope.launch {
                            viewModel.archiveAccount(account)
                                .onSuccess { onDismiss() }
                                .onFailure {
                                    viewModel.notify(
                                        it.message ?: "Couldn't archive the account.",
                                    )
                                }
                        }
                    },
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
                MonoText(
                    text = "Archiving hides the account from capture. Its entries stay in the record.",
                    color = palette.faint,
                    style = PassType.monoMicro.copy(fontSize = 7.sp),
                )
            }
        }
    }
}

@Composable
fun GoalEditorSheet(
    viewModel: FinanceViewModel,
    goal: SavingsGoal?,
    onDismiss: () -> Unit,
) {
    val palette = LocalPsychePalette.current
    val scope = rememberCoroutineScope()
    var name by rememberSaveable { mutableStateOf(goal?.name ?: "") }
    var target by rememberSaveable {
        mutableStateOf(goal?.targetCents?.let { Money.inputValue(it) } ?: "")
    }
    var error by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    val targetParsed = Money.parse(target)

    SheetFrame(onDismiss = onDismiss) {
        SheetHeader(
            eyebrow = "Savings",
            title = if (goal == null) "ADD GOAL" else "EDIT GOAL",
            onClose = onDismiss,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Contributions come from an account and stay separate from the monthly cap. They are transfers, not expenses.",
            style = MaterialTheme.typography.bodySmall,
            color = palette.muted,
        )
        Spacer(Modifier.height(14.dp))
        OutlinedTextField(
            value = name,
            onValueChange = { name = it.take(30) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Goal name", style = MaterialTheme.typography.bodySmall) },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium,
            colors = fieldColors(),
        )
        Spacer(Modifier.height(10.dp))
        MoneyField(
            label = "Target",
            value = target,
            onValueChange = {
                target = it
                error = null
            },
            error = error,
        )
        Spacer(Modifier.height(18.dp))
        PrimaryAction(
            text = when {
                saving -> "Saving\u2026"
                goal == null -> "Add goal"
                else -> "Save goal"
            },
            onClick = {
                val cents = targetParsed
                if (cents == null || name.isBlank()) {
                    error = "Name the goal and enter a target."
                    return@PrimaryAction
                }
                saving = true
                error = null
                scope.launch {
                    val result = if (goal == null) {
                        viewModel.createGoal(name, cents)
                    } else {
                        viewModel.updateGoal(goal.copy(name = name, targetCents = cents))
                    }
                    result.onSuccess { onDismiss() }
                        .onFailure { error = it.message ?: "Couldn't save the goal." }
                    saving = false
                }
            },
            enabled = !saving && name.isNotBlank() && targetParsed != null,
            modifier = Modifier.fillMaxWidth(),
        )
        if (goal != null) {
            Spacer(Modifier.height(10.dp))
            Hairline(color = palette.boardLine)
            if (goal.archived) {
                SecondaryAction(
                    text = "Restore goal",
                    onClick = {
                        scope.launch {
                            viewModel.restoreGoal(goal).onSuccess { onDismiss() }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                DangerAction(
                    text = "Archive goal",
                    onClick = {
                        scope.launch {
                            viewModel.archiveGoal(goal).onSuccess { onDismiss() }
                        }
                    },
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
            }
        }
    }
}

@Composable
fun TransferSheet(
    viewModel: FinanceViewModel,
    accounts: List<Account>,
    balances: List<AccountBalance>,
    lastAccountId: Long?,
    onDismiss: () -> Unit,
) {
    val palette = LocalPsychePalette.current
    val scope = rememberCoroutineScope()
    val fromAccounts = accounts.filter { it.type != AccountType.CREDIT }
    val initialFrom = lastAccountId?.takeIf { id -> fromAccounts.any { it.id == id } }
        ?: fromAccounts.firstOrNull()?.id
        ?: -1L
    var amount by rememberSaveable { mutableStateOf("") }
    var fromId by rememberSaveable { mutableStateOf(initialFrom) }
    var toId by rememberSaveable {
        mutableStateOf(
            accounts.firstOrNull { it.id != initialFrom && it.type != AccountType.CREDIT }?.id
                ?: accounts.firstOrNull { it.id != initialFrom }?.id
                ?: -1L,
        )
    }
    var note by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    val amountParsed = Money.parse(amount)
    val from = fromAccounts.firstOrNull { it.id == fromId }
    val to = accounts.firstOrNull { it.id == toId }
    val availableCents = balances.firstOrNull { it.account.id == fromId }
        ?.availableToMoveCents
        ?: 0L
    val cardPayment = from != null && to?.type == AccountType.CREDIT
    val outstandingCents = if (cardPayment) {
        balances.firstOrNull { it.account.id == toId }?.outstandingCents ?: 0L
    } else {
        null
    }
    val maxCents = outstandingCents?.let { minOf(availableCents, it) } ?: availableCents
    val amountError = when {
        error != null -> error
        cardPayment && outstandingCents != null && outstandingCents <= 0L -> {
            "This card has no outstanding balance to pay."
        }
        cardPayment && outstandingCents != null && amountParsed != null &&
            amountParsed > outstandingCents -> {
            "Only ${Money.formatPeso(outstandingCents)} is outstanding on ${to.name}."
        }
        amount.isNotBlank() && amountParsed == null -> "Use a valid amount."
        amountParsed != null && amountParsed > availableCents && from != null -> {
            "Only ${Money.formatPeso(availableCents)} is available in ${from.name}."
        }
        else -> null
    }
    val canMove = fromAccounts.isNotEmpty() &&
        accounts.any { it.id != fromId } &&
        from != null &&
        to != null &&
        from.id != to.id
    val canSave = canMove &&
        amountParsed != null &&
        amountParsed <= maxCents &&
        (!cardPayment || (outstandingCents ?: 0L) > 0L)

    SheetFrame(onDismiss = onDismiss) {
        SheetHeader(
            eyebrow = if (cardPayment) "Cash to card" else "Between accounts",
            title = if (cardPayment) "PAY CARD" else "MOVE MONEY",
            onClose = onDismiss,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = if (cardPayment) {
                "Pay down the card's outstanding balance. This is a transfer, not a new expense."
            } else {
                "Choose where the money comes from and where it lands. Both account balances update together."
            },
            style = MaterialTheme.typography.bodySmall,
            color = palette.muted,
        )
        Spacer(Modifier.height(14.dp))
        MoneyField(
            label = "Amount",
            value = amount,
            onValueChange = {
                amount = it
                error = null
            },
            error = amountError,
        )
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            MonoText(
                text = "FROM",
                color = palette.faint,
                style = PassType.monoLabel.copy(letterSpacing = 1.1.sp),
            )
            Spacer(Modifier.weight(1f))
            MonoText(
                text = "${Money.formatPeso(availableCents)} AVAILABLE",
                color = palette.faint,
                style = PassType.monoMicro.copy(fontSize = 7.sp),
            )
        }
        Spacer(Modifier.height(6.dp))
        if (fromAccounts.isEmpty()) {
            Text(
                text = "Add a cash or debit account to move money. Cards can only receive a payment.",
                style = MaterialTheme.typography.bodySmall,
                color = palette.danger,
            )
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                fromAccounts.forEach { option ->
                    ChoiceChip(
                        label = option.name,
                        selected = option.id == fromId,
                        onClick = {
                            fromId = option.id
                            if (toId == option.id) {
                                toId = accounts.firstOrNull { it.id != option.id }?.id ?: -1L
                            }
                            error = null
                        },
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            MonoText(
                text = "TO",
                color = palette.faint,
                style = PassType.monoLabel.copy(letterSpacing = 1.1.sp),
            )
            if (cardPayment && outstandingCents != null) {
                Spacer(Modifier.weight(1f))
                MonoText(
                    text = "${Money.formatPeso(outstandingCents)} OUTSTANDING",
                    color = palette.faint,
                    style = PassType.monoMicro.copy(fontSize = 7.sp),
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            accounts.filter { it.id != fromId }.forEach { option ->
                ChoiceChip(
                    label = if (option.type == AccountType.CREDIT) "Pay ${option.name}" else option.name,
                    selected = option.id == toId,
                    onClick = {
                        toId = option.id
                        error = null
                    },
                )
            }
        }
        if (fromAccounts.isEmpty() || accounts.size < 2) {
            Spacer(Modifier.height(10.dp))
            Text(
                text = "Add a second cash, debit, or card account before moving money.",
                style = MaterialTheme.typography.bodySmall,
                color = palette.danger,
            )
        }
        Spacer(Modifier.height(12.dp))
        NoteField(value = note, onValueChange = { note = it })
        Spacer(Modifier.height(8.dp))
        Text(
            text = if (cardPayment) {
                "Recorded as a card payment. It reduces cash and the card's outstanding balance, but does not count against the pass again."
            } else {
                "This is recorded as a transfer. It does not count as spending against the pass."
            },
            style = MaterialTheme.typography.bodySmall,
            color = palette.muted,
        )
        Spacer(Modifier.height(18.dp))
        PrimaryAction(
            text = when {
                saving && cardPayment -> "Paying\u2026"
                saving -> "Moving\u2026"
                cardPayment -> "Pay card"
                else -> "Move money"
            },
            onClick = {
                val cents = amountParsed
                if (cents == null) {
                    error = "Enter at least \u20B10.01."
                    return@PrimaryAction
                }
                if (!canMove) {
                    error = "Choose two different accounts."
                    return@PrimaryAction
                }
                if (cents > maxCents) {
                    error = amountError ?: "That is more than this account has available."
                    return@PrimaryAction
                }
                saving = true
                error = null
                scope.launch {
                    viewModel.recordTransfer(
                        amountCents = cents,
                        fromAccountId = from.id,
                        toAccountId = to.id,
                        goalId = null,
                        note = note.trim(),
                    ).onSuccess { onDismiss() }
                        .onFailure { error = it.message ?: "Couldn't move the money." }
                    saving = false
                }
            },
            enabled = !saving && canSave,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
fun ContributionSheet(
    viewModel: FinanceViewModel,
    goal: GoalProgress,
    accounts: List<Account>,
    balances: List<AccountBalance>,
    lastAccountId: Long?,
    onDismiss: () -> Unit,
) {
    val palette = LocalPsychePalette.current
    val scope = rememberCoroutineScope()
    var amount by rememberSaveable { mutableStateOf("") }
    var accountId by rememberSaveable {
        mutableStateOf(
            lastAccountId?.takeIf { id -> accounts.any { it.id == id } }
                ?: accounts.firstOrNull()?.id
                ?: -1L,
        )
    }
    var note by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    val amountParsed = Money.parse(amount)
    val account = accounts.firstOrNull { it.id == accountId }
    val availableCents = balances.firstOrNull { it.account.id == accountId }
        ?.availableToMoveCents
        ?: 0L
    val goalRemainingCents = goal.remainingCents
    val maximumCents = minOf(availableCents, goalRemainingCents)
    val amountError = when {
        error != null -> error
        amount.isNotBlank() && amountParsed == null -> "Use a valid amount."
        amountParsed != null && goalRemainingCents == 0L -> {
            "This goal is already funded. Raise the target to save more."
        }
        amountParsed != null && amountParsed > goalRemainingCents -> {
            "This goal only needs ${Money.formatPeso(goalRemainingCents)} more."
        }
        amountParsed != null && amountParsed > availableCents && account != null -> {
            if (account.type == AccountType.CREDIT) {
                "Only ${Money.formatPeso(availableCents)} of credit is available on ${account.name}."
            } else {
                "Only ${Money.formatPeso(availableCents)} is available in ${account.name}."
            }
        }
        else -> null
    }
    val canSave = account != null &&
        amountParsed != null &&
        goalRemainingCents > 0 &&
        amountParsed <= maximumCents

    SheetFrame(onDismiss = onDismiss) {
        SheetHeader(
            eyebrow = "Transfer to savings",
            title = "CONTRIBUTE",
            onClose = onDismiss,
        )
        Spacer(Modifier.height(10.dp))
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = palette.boardRow,
            border = BorderStroke(1.dp, palette.boardLine),
        ) {
            Column(Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = goal.goal.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.onGround,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = "${(goal.fraction * 100).toInt()}%",
                        style = PassType.monoData,
                        color = palette.accentText,
                    )
                }
                Spacer(Modifier.height(5.dp))
                MonoText(
                    text = "${Money.formatPeso(goal.savedCents)} SAVED OF ${Money.formatPeso(goal.goal.targetCents)}",
                    color = palette.faint,
                    style = PassType.monoMicro,
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
        Spacer(Modifier.height(8.dp))
        Row {
            MonoText(
                text = "${Money.formatPeso(availableCents)} AVAILABLE",
                color = palette.faint,
                style = PassType.monoMicro.copy(fontSize = 7.sp),
            )
            Spacer(Modifier.weight(1f))
            MonoText(
                text = "${Money.formatPeso(goalRemainingCents)} STILL NEEDED",
                color = palette.faint,
                style = PassType.monoMicro.copy(fontSize = 7.sp),
            )
        }
        Spacer(Modifier.height(10.dp))
        MoneyField(
            label = "Contribution",
            value = amount,
            onValueChange = {
                amount = it
                error = null
            },
            error = amountError,
        )
        Spacer(Modifier.height(12.dp))
        MonoText(
            text = "FROM ACCOUNT",
            color = palette.faint,
            style = PassType.monoLabel.copy(letterSpacing = 1.1.sp),
        )
        Spacer(Modifier.height(6.dp))
        if (accounts.isEmpty()) {
            Text(
                text = "Add an account before moving money to a goal.",
                style = MaterialTheme.typography.bodySmall,
                color = palette.danger,
            )
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                accounts.forEach { option ->
                    ChoiceChip(
                        label = option.name,
                        selected = option.id == accountId,
                        onClick = {
                            accountId = option.id
                            error = null
                        },
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        NoteField(value = note, onValueChange = { note = it })
        Spacer(Modifier.height(8.dp))
        Text(
            text = "This is recorded as a transfer. It does not count as spending against the pass.",
            style = MaterialTheme.typography.bodySmall,
            color = palette.muted,
        )
        Spacer(Modifier.height(18.dp))
        PrimaryAction(
            text = if (saving) "Moving\u2026" else "Move to goal",
            onClick = {
                val cents = amountParsed
                if (cents == null) {
                    error = "Enter at least \u20B10.01."
                    return@PrimaryAction
                }
                if (account == null) {
                    error = "Choose an account."
                    return@PrimaryAction
                }
                if (cents > maximumCents) {
                    error = amountError ?: "That is more than this account or goal can take."
                    return@PrimaryAction
                }
                saving = true
                scope.launch {
                    viewModel.contributeToGoal(
                        amountCents = cents,
                        accountId = account.id,
                        goalId = goal.goal.id,
                        note = note.trim(),
                    ).onSuccess { onDismiss() }
                        .onFailure { error = it.message ?: "Couldn't move the money." }
                    saving = false
                }
            },
            enabled = !saving && canSave,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
fun EntryDetailSheet(
    viewModel: FinanceViewModel,
    row: ActivityRow,
    state: FinanceUiState,
    onDismiss: () -> Unit,
) {
    val palette = LocalPsychePalette.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val entry = row.entry
    var editing by rememberSaveable { mutableStateOf(false) }
    var amount by rememberSaveable { mutableStateOf(Money.inputValue(entry.amountCents)) }
    var description by rememberSaveable { mutableStateOf(entry.description) }
    var accountId by rememberSaveable { mutableStateOf(entry.accountId) }
    var transferAccountId by rememberSaveable { mutableStateOf(entry.transferAccountId) }
    var categoryId by rememberSaveable { mutableStateOf(entry.categoryId ?: 0L) }
    var occurredAt by rememberSaveable { mutableStateOf(entry.occurredAt) }
    var error by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    val amountParsed = Money.parse(amount)
    val isGoalContribution = entry.goalId != null
    val isAccountTransfer = entry.type == TransactionType.TRANSFER && !isGoalContribution
    val accounts = state.snapshot.accounts.filter {
        !it.archived || it.id == entry.accountId || it.id == entry.transferAccountId
    }
    val categories = state.snapshot.categories.filter {
        !it.archived || it.id == entry.categoryId
    }
    val account = accounts.firstOrNull { it.id == accountId }
    val category = categories.firstOrNull { it.id == categoryId }

    LaunchedEffect(entry.id) {
        viewModel.markSeen(entry)
    }

    SheetFrame(onDismiss = onDismiss) {
        SheetHeader(
            eyebrow = "The record \u00B7 ${Formats.fullDate(entry.occurredAt)}",
            title = if (editing) "EDIT ENTRY" else "ENTRY",
            onClose = onDismiss,
        )
        Spacer(Modifier.height(14.dp))
        if (!editing) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = palette.pass,
                contentColor = palette.passInk,
                border = if (palette.isDark) null else BorderStroke(1.dp, palette.panelLine),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (row.isCardPayment) "CARD PAYMENT" else typeLabel(entry.type),
                            style = PassType.monoLabel.copy(fontSize = 8.sp, letterSpacing = 1.2.sp),
                            color = palette.passMuted,
                        )
                        Spacer(Modifier.weight(1f))
                        if (entry.needsAttention) {
                            Stamp(
                                text = "Just logged",
                                color = palette.passInk,
                                borderColor = palette.passLine,
                            )
                        }
                    }
                    Spacer(Modifier.height(5.dp))
                    AutoSizeText(
                        text = signedAmount(entry),
                        style = PassType.passAmount,
                        minFontSize = 26.sp,
                        maxFontSize = 46.sp,
                        color = palette.passInk,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = row.displayDescription,
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.passInk,
                    )
                    Spacer(Modifier.height(10.dp))
                    Hairline(color = palette.passLine)
                    Spacer(Modifier.height(4.dp))
                    KeyValueRow(
                        label = "ACCOUNT",
                        value = row.accountName,
                        valueColor = palette.passInk,
                    )
                    if (entry.type == TransactionType.EXPENSE) {
                        KeyValueRow(
                            label = "CATEGORY",
                            value = row.categoryName ?: "Uncategorized",
                            valueColor = palette.passInk,
                        )
                    }
                    if (entry.type == TransactionType.TRANSFER) {
                        KeyValueRow(
                            label = "DESTINATION",
                            value = row.destinationName ?: "Transfer",
                            valueColor = palette.passInk,
                        )
                    }
                    KeyValueRow(
                        label = "DATE",
                        value = Formats.fullDate(entry.occurredAt),
                        valueColor = palette.passInk,
                    )
                    KeyValueRow(
                        label = "TIME",
                        value = Formats.time(entry.occurredAt),
                        valueColor = palette.passInk,
                    )
                    KeyValueRow(
                        label = "LEFT AFTER",
                        value = row.leftAfterCents?.let { Money.formatPeso(it) } ?: "Not set",
                        valueColor = palette.passInk,
                    )
                    if (entry.description.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Hairline(color = palette.passLine)
                        Spacer(Modifier.height(8.dp))
                        MonoText(
                            text = "NOTE",
                            color = palette.passMuted,
                            style = PassType.monoMicro.copy(letterSpacing = 1.1.sp),
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = entry.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.passInk,
                        )
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PrimaryAction(
                    text = "Edit entry",
                    onClick = { editing = true },
                    modifier = Modifier.weight(1f),
                )
                SecondaryAction(
                    text = "Delete",
                    onClick = {
                        viewModel.deleteEntry(entry)
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(8.dp))
            MonoText(
                text = "DELETING LEAVES AN UNDO IN THE SNACKBAR.",
                color = palette.faint,
                style = PassType.monoMicro.copy(fontSize = 7.sp),
            )
        } else {
            MoneyField(
                label = "Amount",
                value = amount,
                onValueChange = {
                    amount = it
                    error = null
                },
                error = if (amount.isNotBlank() && amountParsed == null) {
                    "Enter a valid amount."
                } else {
                    null
                },
            )
            Spacer(Modifier.height(10.dp))
            NoteField(
                value = description,
                onValueChange = { description = it },
                label = "Note",
            )
            if (entry.type != TransactionType.TRANSFER || isGoalContribution) {
                Spacer(Modifier.height(12.dp))
                MonoText(
                    text = if (isGoalContribution) "FROM ACCOUNT" else "ACCOUNT",
                    color = palette.faint,
                    style = PassType.monoLabel.copy(letterSpacing = 1.1.sp),
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    accounts.forEach { option ->
                        ChoiceChip(
                            label = option.name,
                            selected = option.id == accountId,
                            onClick = {
                                accountId = option.id
                                if (isAccountTransfer && transferAccountId == option.id) {
                                    transferAccountId = null
                                }
                            },
                        )
                    }
                }
            }
            if (isAccountTransfer) {
                Spacer(Modifier.height(12.dp))
                MonoText(
                    text = "MOVE FROM",
                    color = palette.faint,
                    style = PassType.monoLabel.copy(letterSpacing = 1.1.sp),
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    accounts
                        .filter { it.type != AccountType.CREDIT || it.id == entry.accountId }
                        .forEach { option ->
                            ChoiceChip(
                                label = option.name,
                                selected = option.id == accountId,
                                onClick = {
                                    accountId = option.id
                                    if (transferAccountId == option.id) transferAccountId = null
                                    error = null
                                },
                            )
                        }
                }
                Spacer(Modifier.height(10.dp))
                MonoText(
                    text = "TO",
                    color = palette.faint,
                    style = PassType.monoLabel.copy(letterSpacing = 1.1.sp),
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    accounts.filter { it.id != accountId }.forEach { option ->
                        ChoiceChip(
                            label = if (option.type == AccountType.CREDIT) "Pay ${option.name}" else option.name,
                            selected = option.id == transferAccountId,
                            onClick = {
                                transferAccountId = option.id
                                error = null
                            },
                        )
                    }
                }
            }
            if (entry.type == TransactionType.EXPENSE) {
                Spacer(Modifier.height(12.dp))
                MonoText(
                    text = "CATEGORY",
                    color = palette.faint,
                    style = PassType.monoLabel.copy(letterSpacing = 1.1.sp),
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    ChoiceChip(
                        label = "Uncategorized",
                        selected = categoryId == 0L,
                        onClick = { categoryId = 0L },
                    )
                    categories.forEach { option ->
                        ChoiceChip(
                            label = option.name,
                            selected = option.id == categoryId,
                            onClick = { categoryId = option.id },
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            SecondaryAction(
                text = "Date \u00B7 ${Formats.fullDate(occurredAt)}",
                onClick = {
                    val zone = ZoneId.systemDefault()
                    val current = Instant.ofEpochMilli(occurredAt).atZone(zone)
                    val picker = DatePickerDialog(
                        context,
                        { _, year, month, day ->
                            val picked = LocalDateTime.of(
                                year,
                                month + 1,
                                day,
                                current.hour,
                                current.minute,
                                current.second,
                            )
                            occurredAt = picked.atZone(zone).toInstant().toEpochMilli()
                        },
                        current.year,
                        current.monthValue - 1,
                        current.dayOfMonth,
                    )
                    picker.datePicker.maxDate = LocalDate.now()
                        .plusDays(1)
                        .atStartOfDay(zone)
                        .toInstant()
                        .toEpochMilli() - 1L
                    picker.show()
                },
                modifier = Modifier.fillMaxWidth(),
            )
            if (error != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = error ?: "",
                    style = PassType.monoDataSmall,
                    color = palette.danger,
                )
            }
            Spacer(Modifier.height(18.dp))
            PrimaryAction(
                text = if (saving) "Saving\u2026" else "Save changes",
                onClick = {
                    val cents = amountParsed
                    if (cents == null) {
                        error = "Enter a valid amount."
                        return@PrimaryAction
                    }
                    if (account == null) {
                        error = "Choose an account."
                        return@PrimaryAction
                    }
                    if (isAccountTransfer && (transferAccountId == null || transferAccountId == account.id)) {
                        error = "Choose a different destination account."
                        return@PrimaryAction
                    }
                    saving = true
                    error = null
                    scope.launch {
                        viewModel.updateEntry(
                            entry.copy(
                                amountCents = cents,
                                description = description.trim(),
                                accountId = account.id,
                                transferAccountId = if (isAccountTransfer) {
                                    transferAccountId
                                } else {
                                    entry.transferAccountId
                                },
                                categoryId = if (entry.type == TransactionType.EXPENSE) {
                                    categoryId.takeIf { it > 0 }
                                } else {
                                    null
                                },
                                occurredAt = occurredAt,
                                needsAttention = false,
                            ),
                        ).onSuccess { onDismiss() }
                            .onFailure { error = it.message ?: "Couldn't save the entry." }
                        saving = false
                    }
                },
                enabled = !saving &&
                    amountParsed != null &&
                    account != null &&
                    (!isAccountTransfer || (transferAccountId != null && transferAccountId != account.id)),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(4.dp))
            QuietAction(
                text = "Cancel",
                onClick = { editing = false },
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }
    }
}

private fun typeLabel(type: TransactionType): String = when (type) {
    TransactionType.EXPENSE -> "EXPENSE"
    TransactionType.INCOME -> "INCOME"
    TransactionType.TRANSFER -> "TRANSFER"
}

private fun signedAmount(entry: LedgerEntry): String {
    val prefix = when (entry.type) {
        TransactionType.EXPENSE -> "\u2212"
        TransactionType.INCOME -> "+"
        TransactionType.TRANSFER -> "\u21C4"
    }
    return "$prefix${Money.formatPeso(entry.amountCents)}"
}

@Composable
fun SettingsSheet(
    viewModel: FinanceViewModel,
    state: FinanceUiState,
    onCopySummary: () -> Unit,
    onManageQuickActions: () -> Unit,
    onAddWidget: () -> Unit,
    onDismiss: () -> Unit,
) {
    val palette = LocalPsychePalette.current
    val settings = state.settings
    SheetFrame(onDismiss = onDismiss) {
        SheetHeader(
            eyebrow = "Psyche \u00B7 local only",
            title = "SETTINGS",
            onClose = onDismiss,
        )
        Spacer(Modifier.height(16.dp))
        SettingsSection("APPEARANCE")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            ThemeMode.entries.forEach { mode ->
                ChoiceChip(
                    label = mode.label,
                    selected = settings.themeMode == mode,
                    onClick = { viewModel.setThemeMode(mode) },
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = "System is the default. The fixed Budget Pass palette changes with your choice; dynamic colour is off.",
            style = MaterialTheme.typography.bodySmall,
            color = palette.muted,
        )
        Spacer(Modifier.height(22.dp))
        SettingsSection("ONE-TAP ACTIONS")
        Text(
            text = "Custom buttons for the home-screen widget. Each tap logs the amount immediately, with no app open.",
            style = MaterialTheme.typography.bodySmall,
            color = palette.muted,
        )
        Spacer(Modifier.height(10.dp))
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = palette.boardRow,
            border = BorderStroke(1.dp, palette.boardLine),
        ) {
            Column(Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MonoText(
                        text = "${state.quickTransactions.size}/$MAX_QUICK_ACTIONS ACTIVE",
                        color = if (state.quickTransactions.isEmpty()) palette.faint else palette.accentText,
                        style = PassType.monoLabel.copy(fontSize = 8.sp, letterSpacing = 1.1.sp),
                    )
                    Spacer(Modifier.weight(1f))
                    MonoText(
                        text = if (state.quickTransactions.isEmpty()) "NOT SET UP" else "WIDGET READY",
                        color = palette.faint,
                        style = PassType.monoMicro.copy(fontSize = 7.sp),
                    )
                }
                Spacer(Modifier.height(10.dp))
                SecondaryAction(
                    text = "Manage one-tap actions",
                    onClick = onManageQuickActions,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(6.dp))
                SecondaryAction(
                    text = "Add widget",
                    onClick = onAddWidget,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        Spacer(Modifier.height(22.dp))
        SettingsSection("THE PASS")
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = palette.boardRow,
            border = BorderStroke(1.dp, palette.boardLine),
        ) {
            Column(Modifier.padding(12.dp)) {
                MonoText(
                    text = "MONTHLY SUMMARY",
                    color = palette.faint,
                    style = PassType.monoMicro.copy(letterSpacing = 1.sp),
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = state.home?.summary?.periodRef ?: "No pass yet",
                    style = PassType.monoData,
                    color = palette.onGround,
                )
                Spacer(Modifier.height(8.dp))
                SecondaryAction(
                    text = "Copy period summary",
                    onClick = onCopySummary,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        Spacer(Modifier.height(22.dp))
        SettingsSection("DATA & PRIVACY")
        Text(
            text = "The ledger is a Room database on this phone. No network permission, no analytics, no account, and no cloud backup.",
            style = MaterialTheme.typography.bodySmall,
            color = palette.muted,
        )
        Spacer(Modifier.height(10.dp))
        Hairline(color = palette.boardLine)
        Spacer(Modifier.height(10.dp))
        Row {
            MonoText(
                text = "PSYCHE \u00B7 FINANCE PASS",
                color = palette.faint,
                style = PassType.monoMicro.copy(fontSize = 7.sp),
            )
            Spacer(Modifier.weight(1f))
            MonoText(
                text = BuildConfigLabel,
                color = palette.faint,
                style = PassType.monoMicro.copy(fontSize = 7.sp),
            )
        }
    }
}

private const val BuildConfigLabel = "VERSION 1.0"

@Composable
private fun SettingsSection(title: String) {
    val palette = LocalPsychePalette.current
    MonoText(
        text = title,
        color = palette.faint,
        style = PassType.monoLabel.copy(letterSpacing = 1.4.sp),
        modifier = Modifier.padding(bottom = 8.dp),
    )
}

@Composable
fun SetupSheet(
    viewModel: FinanceViewModel,
    state: FinanceUiState,
    onDismiss: () -> Unit,
) {
    val palette = LocalPsychePalette.current
    val scope = rememberCoroutineScope()
    val home = state.home
    val hasAccounts = state.snapshot.activeAccounts.isNotEmpty()
    val needsCap = home?.capCents == null
    var accountName by rememberSaveable { mutableStateOf("Cash") }
    var typeName by rememberSaveable { mutableStateOf(AccountType.CASH.name) }
    var opening by rememberSaveable { mutableStateOf("") }
    var limit by rememberSaveable { mutableStateOf("") }
    var lastFour by rememberSaveable { mutableStateOf("") }
    var cap by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    val type = AccountType.valueOf(typeName)
    val isCredit = type == AccountType.CREDIT
    val openingParsed = parseAccountAmount(opening)
    val limitParsed = if (limit.isBlank()) 0L else Money.parse(limit)
    val capParsed = if (!needsCap) 0L else Money.parse(cap)
    val lastFourValid = lastFour.isBlank() || lastFour.length == 4
    val accountValid = accountName.isNotBlank() &&
        openingParsed != null &&
        lastFourValid &&
        (!isCredit || (limitParsed != null && limitParsed > 0))
    val valid = (hasAccounts || accountValid) && (!needsCap || capParsed != null)

    SheetFrame(onDismiss = onDismiss) {
        SheetHeader(
            eyebrow = "First run",
            title = "SET UP THE PASS",
            onClose = onDismiss,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = if (hasAccounts) {
                "Set this month's cap. Your default Cash account is ready at \u20B10; set its opening balance in Accounts when you want. Nothing is prefilled."
            } else {
                "Two honest numbers start the pass: what you hold, and what this month may spend. Nothing is prefilled."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = palette.muted,
        )
        Spacer(Modifier.height(18.dp))
        if (!hasAccounts) {
            MonoText(
                text = "01 \u00B7 STARTING ACCOUNT",
                color = palette.accentText,
                style = PassType.monoLabel.copy(letterSpacing = 1.2.sp),
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = accountName,
                onValueChange = { accountName = it.take(30) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Account name", style = MaterialTheme.typography.bodySmall) },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium,
                colors = fieldColors(),
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                AccountType.entries.forEach { option ->
                    ChoiceChip(
                        label = option.label,
                        selected = option == type,
                        onClick = { typeName = option.name },
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            MoneyField(
                label = if (isCredit) "Outstanding now (optional)" else "Opening balance (optional)",
                value = opening,
                onValueChange = {
                    opening = it
                    error = null
                },
                supporting = if (isCredit) {
                    "What you owe on the card today. Leave blank if it is paid off."
                } else {
                    "Leave blank for \u20B10. This is not invented."
                },
                error = if (opening.isNotBlank() && openingParsed == null) "Use a valid amount." else null,
            )
            if (isCredit) {
                Spacer(Modifier.height(10.dp))
                MoneyField(
                    label = "Credit limit",
                    value = limit,
                    onValueChange = {
                        limit = it
                        error = null
                    },
                    supporting = if (limit.isBlank()) "Required for a credit account." else null,
                    error = if (limit.isNotBlank() && limitParsed == null) "Use a valid limit." else null,
                )
            }
            if (type != AccountType.CASH) {
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = lastFour,
                    onValueChange = { value ->
                        lastFour = value.filter { it.isDigit() }.take(4)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Card last four (optional)", style = MaterialTheme.typography.bodySmall) },
                    singleLine = true,
                    isError = !lastFourValid,
                    supportingText = if (!lastFourValid) {
                        {
                            Text(
                                text = "Enter all four digits, or leave it blank.",
                                style = PassType.monoDataSmall,
                                color = palette.danger,
                            )
                        }
                    } else {
                        null
                    },
                    textStyle = PassType.monoData,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                    ),
                    colors = fieldColors(),
                )
            }
            Spacer(Modifier.height(18.dp))
        } else {
            MonoText(
                text = "ACCOUNT READY \u00B7 ${state.snapshot.activeAccounts.joinToString { it.name }.uppercase(Locale.US)}",
                color = palette.muted,
                style = PassType.monoMicro.copy(letterSpacing = 0.8.sp),
            )
            Spacer(Modifier.height(18.dp))
        }
        if (needsCap) {
            MonoText(
                text = if (hasAccounts) "01 \u00B7 MONTHLY CAP" else "02 \u00B7 MONTHLY CAP",
                color = palette.accentText,
                style = PassType.monoLabel.copy(letterSpacing = 1.2.sp),
            )
            Spacer(Modifier.height(8.dp))
            MoneyField(
                label = "Cap for ${Formats.monthTitle(state.homeMonth)}",
                value = cap,
                onValueChange = {
                    cap = it
                    error = null
                },
                error = if (cap.isNotBlank() && capParsed == null) "Use a valid amount." else null,
                supporting = "Expenses count against this. Income never expands it.",
            )
        }
        if (error != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = error ?: "",
                style = PassType.monoDataSmall,
                color = palette.danger,
            )
        }
        Spacer(Modifier.height(20.dp))
        PrimaryAction(
            text = when {
                saving -> "Setting up\u2026"
                !hasAccounts -> "Create pass"
                needsCap -> "Set cap"
                else -> "Done"
            },
            onClick = {
                if (!valid) {
                    error = "Check the fields before continuing."
                    return@PrimaryAction
                }
                saving = true
                error = null
                scope.launch {
                    if (!hasAccounts) {
                        val enteredOpening = openingParsed ?: 0L
                        val result = viewModel.createAccount(
                            name = accountName,
                            type = type,
                            openingBalanceCents = if (isCredit) -enteredOpening else enteredOpening,
                            creditLimitCents = if (isCredit) limitParsed ?: 0L else 0L,
                            lastFour = lastFour.ifBlank { null },
                        )
                        if (result.isFailure) {
                            error = result.exceptionOrNull()?.message ?: "Couldn't add the account."
                            saving = false
                            return@launch
                        }
                    }
                    if (needsCap) {
                        val result = viewModel.saveBudget(state.homeMonth, capParsed ?: 0L)
                        if (result.isFailure) {
                            error = result.exceptionOrNull()?.message ?: "Couldn't set the cap."
                            saving = false
                            return@launch
                        }
                    }
                    saving = false
                    onDismiss()
                }
            },
            enabled = !saving && valid,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        MonoText(
            text = "YOU CAN CHANGE THE CAP AND ACCOUNTS LATER. THE RECORD STAYS LOCAL.",
            color = palette.faint,
            style = PassType.monoMicro.copy(fontSize = 7.sp),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun QuickTransactionsSheet(
    viewModel: FinanceViewModel,
    quickTransactions: List<QuickTransaction>,
    accounts: List<Account>,
    categories: List<Category>,
    onAdd: () -> Unit,
    onEdit: (QuickTransaction) -> Unit,
    onDismiss: () -> Unit,
) {
    val palette = LocalPsychePalette.current
    val scope = rememberCoroutineScope()
    var error by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    val activeCount = quickTransactions.count { !it.archived }

    SheetFrame(onDismiss = onDismiss) {
        SheetHeader(
            eyebrow = "The widget",
            title = "ONE-TAP ACTIONS",
            onClose = onDismiss,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Preset buttons for the home-screen widget. A tap logs the amount immediately, using the saved account and category.",
            style = MaterialTheme.typography.bodySmall,
            color = palette.muted,
        )
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            MonoText(
                text = "$activeCount/$MAX_QUICK_ACTIONS ACTIVE",
                color = if (activeCount == 0) palette.faint else palette.accentText,
                style = PassType.monoLabel.copy(fontSize = 8.sp, letterSpacing = 1.1.sp),
            )
            Spacer(Modifier.weight(1f))
            MonoText(
                text = "4 SLOTS",
                color = palette.faint,
                style = PassType.monoMicro.copy(fontSize = 7.sp),
            )
        }
        Spacer(Modifier.height(10.dp))
        if (quickTransactions.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = palette.boardRow,
                border = BorderStroke(1.dp, palette.boardLine),
            ) {
                Column(Modifier.padding(14.dp)) {
                    Text(
                        text = "No one-tap actions yet",
                        style = MaterialTheme.typography.titleSmall,
                        color = palette.onGround,
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = "Start with the amount you log most often, like coffee, fare, or load.",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.muted,
                    )
                }
            }
        } else {
            quickTransactions.forEach { quick ->
                val account = accounts.firstOrNull { it.id == quick.accountId }
                val category = quick.categoryId?.let { id ->
                    categories.firstOrNull { it.id == id }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(role = Role.Button) { onEdit(quick) }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = quick.label,
                                style = MaterialTheme.typography.titleSmall,
                                color = if (quick.archived) palette.muted else palette.onGround,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (quick.archived) {
                                Spacer(Modifier.width(6.dp))
                                MonoText(
                                    text = "ARCHIVED",
                                    color = palette.faint,
                                    style = PassType.monoMicro.copy(fontSize = 7.sp),
                                )
                            }
                        }
                        Spacer(Modifier.height(2.dp))
                        MonoText(
                            text = buildString {
                                append(if (quick.type == TransactionType.EXPENSE) "EXPENSE" else "INCOME")
                                append(" \u00B7 ")
                                append(account?.name ?: "MISSING ACCOUNT")
                                if (quick.type == TransactionType.EXPENSE) {
                                    append(" \u00B7 ")
                                    append(
                                        category?.name
                                            ?: if (quick.categoryId != null) {
                                                "ARCHIVED CATEGORY"
                                            } else {
                                                "UNCATEGORIZED"
                                            },
                                    )
                                }
                            }.uppercase(Locale.US),
                            color = palette.faint,
                            style = PassType.monoMicro.copy(fontSize = 7.sp),
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = Money.formatPeso(quick.amountCents),
                        style = PassType.monoData.copy(fontSize = 12.sp),
                        color = if (quick.archived) palette.muted else palette.onGround,
                        maxLines = 1,
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = if (quick.archived) "RESTORE" else "ARCHIVE",
                        style = PassType.monoMicro.copy(fontSize = 7.sp),
                        color = if (quick.archived) palette.accentText else palette.faint,
                        modifier = Modifier
                            .clickable(role = Role.Button) {
                                error = null
                                saving = true
                                scope.launch {
                                    val result = if (quick.archived) {
                                        viewModel.restoreQuickTransaction(quick)
                                    } else {
                                        viewModel.archiveQuickTransaction(quick)
                                    }
                                    result.onFailure {
                                        error = it.message ?: "Couldn't update the action."
                                    }
                                    saving = false
                                }
                            }
                            .padding(8.dp),
                    )
                }
                Hairline(color = palette.boardLine)
            }
        }
        if (error != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = error ?: "",
                style = PassType.monoDataSmall,
                color = palette.danger,
            )
        }
        Spacer(Modifier.height(16.dp))
        PrimaryAction(
            text = if (activeCount >= MAX_QUICK_ACTIONS) "Four slots full" else "Add one-tap action",
            onClick = onAdd,
            enabled = !saving && activeCount < MAX_QUICK_ACTIONS,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = "Add the widget from Settings, or long-press your home screen and open Widgets \u2192 Psyche.",
            style = MaterialTheme.typography.bodySmall,
            color = palette.muted,
        )
    }
}

@Composable
fun QuickTransactionEditorSheet(
    viewModel: FinanceViewModel,
    quick: QuickTransaction?,
    accounts: List<Account>,
    categories: List<Category>,
    lastAccountId: Long?,
    onDismiss: () -> Unit,
) {
    val palette = LocalPsychePalette.current
    val scope = rememberCoroutineScope()
    var label by rememberSaveable { mutableStateOf(quick?.label ?: "") }
    var typeName by rememberSaveable {
        mutableStateOf((quick?.type ?: TransactionType.EXPENSE).name)
    }
    var amount by rememberSaveable {
        mutableStateOf(quick?.amountCents?.let { Money.inputValue(it) } ?: "")
    }
    var accountId by rememberSaveable {
        mutableStateOf(
            quick?.accountId?.takeIf { id -> accounts.any { it.id == id } }
                ?: lastAccountId?.takeIf { id -> accounts.any { it.id == id } }
                ?: accounts.firstOrNull()?.id
                ?: -1L,
        )
    }
    var categoryId by rememberSaveable {
        mutableStateOf(
            quick?.categoryId?.takeIf { id -> categories.any { it.id == id } } ?: 0L,
        )
    }
    var note by rememberSaveable { mutableStateOf(quick?.note ?: "") }
    var error by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }

    val type = TransactionType.valueOf(typeName)
    val amountParsed = Money.parse(amount)
    val account = accounts.firstOrNull { it.id == accountId }
    val valid = label.isNotBlank() &&
        label.length <= 24 &&
        amountParsed != null &&
        account != null

    SheetFrame(onDismiss = onDismiss) {
        SheetHeader(
            eyebrow = if (quick == null) "New preset" else "Preset",
            title = if (quick == null) "ADD ONE-TAP" else "EDIT ONE-TAP",
            onClose = onDismiss,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "This becomes a button on the widget. The amount is logged as soon as it is tapped.",
            style = MaterialTheme.typography.bodySmall,
            color = palette.muted,
        )
        Spacer(Modifier.height(14.dp))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            color = palette.boardRow,
            border = BorderStroke(1.dp, palette.boardLine),
        ) {
            Column(Modifier.padding(12.dp)) {
                MonoText(
                    text = "WIDGET BUTTON PREVIEW",
                    color = palette.faint,
                    style = PassType.monoMicro.copy(letterSpacing = 1.sp),
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = label.ifBlank { "Label" },
                            style = MaterialTheme.typography.titleSmall,
                            color = if (label.isBlank()) palette.faint else palette.onGround,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        MonoText(
                            text = if (type == TransactionType.EXPENSE) "EXPENSE" else "INCOME",
                            color = palette.accentText,
                            style = PassType.monoMicro.copy(fontSize = 7.sp),
                        )
                    }
                    Text(
                        text = amountParsed?.let { Money.formatPeso(it) } ?: "\u20B10",
                        style = PassType.monoData.copy(fontSize = 16.sp),
                        color = if (amountParsed == null) palette.faint else palette.onGround,
                    )
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        OutlinedTextField(
            value = label,
            onValueChange = { label = it.take(24); error = null },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Label", style = MaterialTheme.typography.bodySmall) },
            placeholder = { Text("Coffee", style = MaterialTheme.typography.bodySmall) },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium,
            colors = fieldColors(),
        )
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            ChoiceChip(
                label = "Expense",
                selected = type == TransactionType.EXPENSE,
                onClick = { typeName = TransactionType.EXPENSE.name; error = null },
            )
            ChoiceChip(
                label = "Income",
                selected = type == TransactionType.INCOME,
                onClick = { typeName = TransactionType.INCOME.name; categoryId = 0L; error = null },
            )
        }
        Spacer(Modifier.height(12.dp))
        MoneyField(
            label = "Amount",
            value = amount,
            onValueChange = { amount = it; error = null },
            error = if (amount.isNotBlank() && amountParsed == null) "Use a valid amount." else null,
            supporting = "Logged every time the button is tapped.",
        )
        Spacer(Modifier.height(12.dp))
        MonoText(
            text = "ACCOUNT",
            color = palette.faint,
            style = PassType.monoLabel.copy(letterSpacing = 1.1.sp),
        )
        Spacer(Modifier.height(6.dp))
        if (accounts.isEmpty()) {
            Text(
                text = "Add a cash or card account before making a one-tap action.",
                style = MaterialTheme.typography.bodySmall,
                color = palette.danger,
            )
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                accounts.forEach { option ->
                    ChoiceChip(
                        label = option.name,
                        selected = option.id == accountId,
                        onClick = { accountId = option.id; error = null },
                    )
                }
            }
        }
        if (type == TransactionType.EXPENSE) {
            Spacer(Modifier.height(12.dp))
            MonoText(
                text = "CATEGORY",
                color = palette.faint,
                style = PassType.monoLabel.copy(letterSpacing = 1.1.sp),
            )
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                ChoiceChip(
                    label = "Uncategorized",
                    selected = categoryId == 0L,
                    onClick = { categoryId = 0L; error = null },
                )
                categories.forEach { option ->
                    ChoiceChip(
                        label = option.name,
                        selected = option.id == categoryId,
                        onClick = { categoryId = option.id; error = null },
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        NoteField(
            value = note,
            onValueChange = { note = it },
            label = "Description (optional)",
        )
        if (error != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = error ?: "",
                style = PassType.monoDataSmall,
                color = palette.danger,
            )
        }
        Spacer(Modifier.height(16.dp))
        PrimaryAction(
            text = when {
                saving -> "Saving\u2026"
                quick == null -> "Add one-tap action"
                else -> "Save one-tap action"
            },
            onClick = {
                if (!valid) {
                    error = "Check the label, amount, and account."
                    return@PrimaryAction
                }
                val cents = requireNotNull(amountParsed) { "Amount is required." }
                val selectedAccount = requireNotNull(account) { "Account is required." }
                saving = true
                error = null
                scope.launch {
                    val result = if (quick == null) {
                        viewModel.createQuickTransaction(
                            label = label,
                            type = type,
                            amountCents = cents,
                            accountId = selectedAccount.id,
                            categoryId = categoryId.takeIf {
                                type == TransactionType.EXPENSE && it > 0
                            },
                            note = note,
                        )
                    } else {
                        viewModel.updateQuickTransaction(
                            quick.copy(
                                label = label,
                                type = type,
                                amountCents = cents,
                                accountId = selectedAccount.id,
                                categoryId = categoryId.takeIf {
                                    type == TransactionType.EXPENSE && it > 0
                                },
                                note = note,
                            ),
                        )
                    }
                    result.onSuccess { onDismiss() }
                        .onFailure { error = it.message ?: "Couldn't save the one-tap action." }
                    saving = false
                }
            },
            enabled = !saving && valid,
            modifier = Modifier.fillMaxWidth(),
        )
        if (quick != null) {
            Spacer(Modifier.height(10.dp))
            Hairline(color = palette.boardLine)
            if (quick.archived) {
                SecondaryAction(
                    text = "Restore action",
                    onClick = {
                        scope.launch {
                            viewModel.restoreQuickTransaction(quick).onSuccess { onDismiss() }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                DangerAction(
                    text = "Archive action",
                    onClick = {
                        scope.launch {
                            viewModel.archiveQuickTransaction(quick).onSuccess { onDismiss() }
                        }
                    },
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
                MonoText(
                    text = "Archiving removes the button from the widget. Past entries stay.",
                    color = palette.faint,
                    style = PassType.monoMicro.copy(fontSize = 7.sp),
                )
            }
        }
    }
}

