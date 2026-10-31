package com.psyche.app.ui.capture

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.psyche.app.R
import com.psyche.app.data.Account
import com.psyche.app.data.Category
import com.psyche.app.ui.CaptureDraft
import com.psyche.app.ui.CaptureType
import com.psyche.app.ui.FinanceUiState
import com.psyche.app.ui.FinanceViewModel
import com.psyche.app.ui.components.AutoSizeText
import com.psyche.app.ui.components.ChoiceChip
import com.psyche.app.ui.components.Hairline
import com.psyche.app.ui.components.MonoText
import com.psyche.app.ui.components.PrimaryAction
import com.psyche.app.ui.components.SecondaryAction
import com.psyche.app.ui.components.SheetHeader
import com.psyche.app.ui.theme.LocalPsychePalette
import com.psyche.app.ui.theme.PassType
import com.psyche.app.util.Money
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun CaptureContent(
    viewModel: FinanceViewModel,
    state: FinanceUiState,
    onDismiss: () -> Unit,
    onTransfer: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val palette = LocalPsychePalette.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val accounts = state.snapshot.activeAccounts
    val categories = state.snapshot.activeCategories

    var rawAmount by rememberSaveable { mutableStateOf("") }
    var typeName by rememberSaveable { mutableStateOf(CaptureType.EXPENSE.name) }
    var accountId by rememberSaveable { mutableStateOf(-1L) }
    var categoryId by rememberSaveable { mutableStateOf(0L) }
    var note by rememberSaveable { mutableStateOf("") }
    var detailsOpen by rememberSaveable { mutableStateOf(false) }
    var defaultsApplied by rememberSaveable { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    var savedId by remember { mutableStateOf<Long?>(null) }
    var savedAmount by remember { mutableStateOf(0L) }
    var savedSummary by remember { mutableStateOf("") }
    var changingCategory by remember { mutableStateOf(false) }
    var confirmToken by remember { mutableIntStateOf(0) }
    var categoryName by remember { mutableStateOf("Uncategorized") }

    val type = CaptureType.valueOf(typeName)
    val selectedAccount = accounts.firstOrNull { it.id == accountId }
    val selectedCategory = categories.firstOrNull { it.id == categoryId }
    val availableCents = state.accountBalances
        .firstOrNull { it.account.id == accountId }
        ?.availableToMoveCents
        ?: 0L
    val amountCents = Money.parse(rawAmount)
    val exceedsAvailable = type == CaptureType.EXPENSE &&
        amountCents != null &&
        amountCents > availableCents &&
        selectedAccount != null
    val availabilityError = if (exceedsAvailable) {
        if (selectedAccount.type == com.psyche.app.data.AccountType.CREDIT) {
            "Only ${Money.formatPeso(availableCents)} of credit is available on ${selectedAccount.name}."
        } else {
            "Only ${Money.formatPeso(availableCents)} is available in ${selectedAccount.name}."
        }
    } else {
        null
    }
    val shownError = availabilityError ?: error
    val displayAmount = if (rawAmount.isEmpty()) "0" else Money.groupTypedAmount(rawAmount)

    LaunchedEffect(accounts, categories) {
        if (!defaultsApplied && accounts.isNotEmpty()) {
            accountId = state.settings.lastAccountId
                ?.takeIf { id -> accounts.any { it.id == id } }
                ?: accounts.first().id
            categoryId = state.settings.lastCategoryId
                ?.takeIf { id -> categories.any { it.id == id } }
                ?: 0L
            defaultsApplied = true
        }
    }

    LaunchedEffect(savedId, confirmToken, changingCategory) {
        if (savedId != null && !changingCategory) {
            delay(2_800)
            onDismiss()
        }
    }

    fun save() {
        if (saving) return
        val parsed = amountCents
        if (parsed == null) {
            error = "Enter at least \u20B10.01."
            return
        }
        val account = selectedAccount
        if (account == null) {
            error = "Add an account before capturing."
            return
        }
        if (type == CaptureType.EXPENSE && parsed > availableCents) {
            error = if (account.type == com.psyche.app.data.AccountType.CREDIT) {
                "Only ${Money.formatPeso(availableCents)} of credit is available on ${account.name}."
            } else {
                "Only ${Money.formatPeso(availableCents)} is available in ${account.name}."
            }
            return
        }
        error = null
        saving = true
        scope.launch {
            viewModel.recordCapture(
                CaptureDraft(
                    type = type,
                    amountCents = parsed,
                    accountId = account.id,
                    categoryId = if (type == CaptureType.EXPENSE && categoryId > 0) categoryId else null,
                    note = note.trim(),
                    occurredAt = System.currentTimeMillis(),
                ),
                announce = false,
            ).onSuccess { id ->
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                savedId = id
                savedAmount = parsed
                savedSummary = buildString {
                    append(account.name)
                    if (type == CaptureType.EXPENSE) {
                        append(" \u00B7 ")
                        append(selectedCategory?.name ?: "Uncategorized")
                    }
                }
                viewModel.setLastDefaults(
                    accountId = account.id,
                    categoryId = if (type == CaptureType.EXPENSE && categoryId > 0) categoryId else null,
                )
            }.onFailure {
                error = it.message ?: "Couldn't save. Try again."
            }
            saving = false
        }
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = palette.ground,
        contentColor = palette.onGround,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 8.dp, bottom = 16.dp),
        ) {
            if (savedId != null) {
                SavedConfirmation(
                    amountCents = savedAmount,
                    summary = savedSummary,
                    type = type,
                    categories = categories,
                    changingCategory = changingCategory,
                    selectedCategoryId = categoryId,
                    onUndo = {
                        val id = savedId ?: return@SavedConfirmation
                        saving = true
                        scope.launch {
                            viewModel.undoCaptured(id)
                            onDismiss()
                        }
                    },
                    onChangeCategory = {
                        changingCategory = !changingCategory
                        confirmToken += 1
                    },
                    onDone = onDismiss,
                    onPickCategory = { picked ->
                        val id = savedId ?: return@SavedConfirmation
                        saving = true
                        scope.launch {
                            viewModel.changeCapturedCategory(
                                entryId = id,
                                categoryId = picked.takeIf { it > 0 },
                            ).onSuccess {
                                categoryId = picked
                                categoryName = categories.firstOrNull { it.id == picked }?.name
                                    ?: "Uncategorized"
                                savedSummary = buildString {
                                    append(savedSummary.substringBefore(" \u00B7 "))
                                    append(" \u00B7 ")
                                    append(categoryName)
                                }
                                changingCategory = false
                                confirmToken += 1
                            }.onFailure {
                                error = it.message ?: "Couldn't change the category."
                            }
                            saving = false
                        }
                    },
                    saving = saving,
                )
            } else {
                SheetHeader(
                    eyebrow = "QUICK CAPTURE",
                    title = if (type == CaptureType.EXPENSE) "LOG EXPENSE" else "LOG INCOME",
                    onClose = onDismiss,
                )
                Spacer(Modifier.height(12.dp))
                CaptureTypeToggle(
                    selected = type,
                    onSelect = {
                        typeName = it.name
                        error = null
                    },
                )
                if (onTransfer != null && accounts.size >= 2) {
                    Spacer(Modifier.height(8.dp))
                    SecondaryAction(
                        text = "Move money between accounts",
                        onClick = onTransfer,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "AMOUNT",
                    style = PassType.monoMicro.copy(letterSpacing = 1.2.sp),
                    color = palette.faint,
                )
                Spacer(Modifier.height(2.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Text(
                        text = "\u20B1",
                        style = PassType.passAmount.copy(fontSize = 30.sp, lineHeight = 38.sp),
                        color = when {
                            rawAmount.isEmpty() -> palette.faint
                            exceedsAvailable -> palette.danger
                            else -> palette.onGround
                        },
                        modifier = Modifier.padding(bottom = 7.dp),
                    )
                    Spacer(Modifier.width(3.dp))
                    AutoSizeText(
                        text = displayAmount,
                        style = PassType.passAmount,
                        minFontSize = 30.sp,
                        maxFontSize = 58.sp,
                        color = when {
                            rawAmount.isEmpty() -> palette.faint
                            exceedsAvailable -> palette.danger
                            else -> palette.onGround
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
                Hairline(color = palette.boardLine)
                Spacer(Modifier.height(6.dp))
                DetailToggleRow(
                    summary = buildString {
                        append(selectedAccount?.name?.uppercase(Locale.US) ?: "NO ACCOUNT")
                        if (type == CaptureType.EXPENSE) {
                            append(" \u00B7 ")
                            append(selectedCategory?.name?.uppercase(Locale.US) ?: "UNCATEGORIZED")
                        }
                    },
                    open = detailsOpen,
                    onToggle = { detailsOpen = !detailsOpen },
                )
                if (selectedAccount != null) {
                    Spacer(Modifier.height(3.dp))
                    MonoText(
                        text = if (type == CaptureType.EXPENSE) {
                            "${Money.formatPeso(availableCents)} AVAILABLE"
                        } else {
                            "INCOME GOES TO ${selectedAccount.name.uppercase(Locale.US)}"
                        },
                        color = if (exceedsAvailable) palette.danger else palette.faint,
                        style = PassType.monoMicro.copy(fontSize = 7.sp),
                    )
                }
                if (detailsOpen) {
                    Spacer(Modifier.height(8.dp))
                    CaptureDetails(
                        accounts = accounts,
                        categories = categories,
                        type = type,
                        accountId = accountId,
                        categoryId = categoryId,
                        note = note,
                        onAccount = {
                            accountId = it
                            error = null
                        },
                        onCategory = {
                            categoryId = it
                            error = null
                        },
                        onNote = { note = it },
                    )
                }
                if (accounts.isEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = palette.boardRow,
                        border = BorderStroke(1.dp, palette.danger),
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text(
                                text = "No account yet",
                                style = PassType.monoLabel,
                                color = palette.danger,
                            )
                            Text(
                                text = "Add one on the Accounts tab, then this pad will save.",
                                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                                color = palette.muted,
                            )
                        }
                    }
                }
                if (shownError != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = shownError,
                        style = PassType.monoDataSmall,
                        color = palette.danger,
                    )
                }
                Spacer(Modifier.height(10.dp))
                CaptureKeypad(
                    onKey = { key ->
                        rawAmount = if (key == BACKSPACE) {
                            Money.backspace(rawAmount)
                        } else {
                            Money.appendAmountKey(rawAmount, key)
                        }
                        error = null
                    },
                )
                Spacer(Modifier.height(12.dp))
                PrimaryAction(
                    text = when {
                        saving -> "Saving\u2026"
                        type == CaptureType.EXPENSE -> "Save expense"
                        else -> "Save income"
                    },
                    onClick = ::save,
                    enabled = !saving &&
                        amountCents != null &&
                        selectedAccount != null &&
                        !exceedsAvailable,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                MonoText(
                    text = "PHP \u00B7 EN-PH \u00B7 12-HOUR TIME",
                    color = palette.faint,
                    style = PassType.monoMicro.copy(fontSize = 7.sp, letterSpacing = 0.9.sp),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

private const val BACKSPACE = '\u232B'

@Composable
private fun CaptureTypeToggle(
    selected: CaptureType,
    onSelect: (CaptureType) -> Unit,
) {
    val palette = LocalPsychePalette.current
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = palette.boardRow,
        border = BorderStroke(1.dp, palette.boardLine),
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            CaptureType.entries.forEach { type ->
                val isSelected = type == selected
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 42.dp)
                        .background(
                            if (isSelected) palette.alert else androidx.compose.ui.graphics.Color.Transparent,
                        )
                        .selectable(
                            selected = isSelected,
                            role = Role.RadioButton,
                            onClick = { onSelect(type) },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (type == CaptureType.EXPENSE) "EXPENSE" else "INCOME",
                        style = PassType.monoLabel.copy(letterSpacing = 1.2.sp),
                        color = if (isSelected) palette.onAlert else palette.muted,
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailToggleRow(
    summary: String,
    open: Boolean,
    onToggle: () -> Unit,
) {
    val palette = LocalPsychePalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onToggle)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            MonoText(
                text = "ACCOUNT / CATEGORY",
                color = palette.faint,
                style = PassType.monoMicro.copy(letterSpacing = 1.1.sp),
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = summary,
                style = PassType.monoData.copy(fontSize = 11.sp),
                color = palette.onGround,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            )
        }
        Text(
            text = if (open) "HIDE" else "EDIT",
            style = PassType.monoLabel.copy(letterSpacing = 1.1.sp),
            color = palette.muted,
        )
    }
}

@Composable
private fun CaptureDetails(
    accounts: List<Account>,
    categories: List<Category>,
    type: CaptureType,
    accountId: Long,
    categoryId: Long,
    note: String,
    onAccount: (Long) -> Unit,
    onCategory: (Long) -> Unit,
    onNote: (String) -> Unit,
) {
    val palette = LocalPsychePalette.current
    Column(Modifier.fillMaxWidth()) {
        MonoText(
            text = "ACCOUNT",
            color = palette.faint,
            style = PassType.monoMicro.copy(letterSpacing = 1.1.sp),
        )
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            accounts.forEach { account ->
                ChoiceChip(
                    label = account.name,
                    selected = account.id == accountId,
                    onClick = { onAccount(account.id) },
                )
            }
        }
        if (type == CaptureType.EXPENSE) {
            Spacer(Modifier.height(12.dp))
            MonoText(
                text = "CATEGORY",
                color = palette.faint,
                style = PassType.monoMicro.copy(letterSpacing = 1.1.sp),
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
                    onClick = { onCategory(0L) },
                )
                categories.forEach { category ->
                    ChoiceChip(
                        label = category.name,
                        selected = category.id == categoryId,
                        onClick = { onCategory(category.id) },
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MonoText(
                text = "NOTE (OPTIONAL)",
                color = palette.faint,
                style = PassType.monoLabel.copy(letterSpacing = 1.1.sp),
            )
            Spacer(Modifier.weight(1f))
            MonoText(
                text = "${note.length}/80",
                color = if (note.length >= 72) palette.accentText else palette.faint,
                style = PassType.monoMicro.copy(fontSize = 7.sp),
            )
        }
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(
            value = note,
            onValueChange = { if (it.length <= 80) onNote(it) },
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    text = "Optional",
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                )
            },
            singleLine = true,
            textStyle = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = palette.onGround,
                unfocusedTextColor = palette.onGround,
                cursorColor = palette.onGround,
                focusedBorderColor = palette.onGround,
                unfocusedBorderColor = palette.panelLine,
                focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
            ),
        )
    }
}

@Composable
private fun CaptureKeypad(
    onKey: (Char) -> Unit,
) {
    val palette = LocalPsychePalette.current
    val rows = listOf(
        listOf('1', '2', '3'),
        listOf('4', '5', '6'),
        listOf('7', '8', '9'),
        listOf('.', '0', BACKSPACE),
    )
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                row.forEach { key ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(role = Role.Button) { onKey(key) },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (key == BACKSPACE) {
                            Icon(
                                painter = painterResource(R.drawable.ic_backspace),
                                contentDescription = "Backspace",
                                tint = palette.onGround,
                                modifier = Modifier.size(22.dp),
                            )
                        } else {
                            Text(
                                text = key.toString(),
                                style = PassType.monoData.copy(
                                    fontSize = 21.sp,
                                    fontWeight = FontWeight.Medium,
                                ),
                                color = palette.onGround,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SavedConfirmation(
    amountCents: Long,
    summary: String,
    type: CaptureType,
    categories: List<Category>,
    changingCategory: Boolean,
    selectedCategoryId: Long,
    onUndo: () -> Unit,
    onDone: () -> Unit,
    onChangeCategory: () -> Unit,
    onPickCategory: (Long) -> Unit,
    saving: Boolean,
) {
    val palette = LocalPsychePalette.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 20.dp, bottom = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(palette.alert, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_check),
                contentDescription = null,
                tint = palette.onAlert,
                modifier = Modifier.size(26.dp),
            )
        }
        Spacer(Modifier.height(14.dp))
        Text(
            text = if (type == CaptureType.EXPENSE) "EXPENSE LOGGED" else "INCOME LOGGED",
            style = PassType.sectionTitle.copy(fontSize = 20.sp, letterSpacing = 1.8.sp),
            color = palette.onGround,
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = Money.formatPeso(amountCents),
            style = PassType.passAmount.copy(fontSize = 52.sp, lineHeight = 52.sp),
            color = palette.onGround,
        )
        Spacer(Modifier.height(2.dp))
        MonoText(
            text = summary.uppercase(Locale.US),
            color = palette.muted,
            style = PassType.monoDataSmall.copy(letterSpacing = 0.6.sp),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(10.dp))
        if (changingCategory) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                ChoiceChip(
                    label = "Uncategorized",
                    selected = selectedCategoryId == 0L,
                    onClick = { onPickCategory(0L) },
                    enabled = !saving,
                )
                categories.forEach { category ->
                    ChoiceChip(
                        label = category.name,
                        selected = category.id == selectedCategoryId,
                        onClick = { onPickCategory(category.id) },
                        enabled = !saving,
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SecondaryAction(
                text = "Undo",
                onClick = onUndo,
                modifier = Modifier.weight(1f),
                enabled = !saving,
            )
            if (type == CaptureType.EXPENSE) {
                PrimaryAction(
                    text = if (changingCategory) "Done" else "Change category",
                    onClick = onChangeCategory,
                    modifier = Modifier.weight(1.5f),
                    enabled = !saving,
                )
            } else {
                PrimaryAction(
                    text = "Done",
                    onClick = onDone,
                    modifier = Modifier.weight(1.5f),
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        MonoText(
            text = "THIS PANEL CLOSES IN A MOMENT \u00B7 UNDO IS ALWAYS HERE",
            color = palette.faint,
            style = PassType.monoMicro.copy(fontSize = 7.sp, letterSpacing = 0.8.sp),
            textAlign = TextAlign.Center,
        )
    }
}
