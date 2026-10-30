package com.psyche.app.ui

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.psyche.app.R
import com.psyche.app.data.Account
import com.psyche.app.data.Category
import com.psyche.app.data.SavingsGoal
import com.psyche.app.notifications.BudgetNotifier
import com.psyche.app.ui.capture.CaptureContent
import com.psyche.app.ui.components.PsycheBottomBar
import com.psyche.app.ui.components.TopFeedbackBanner
import com.psyche.app.ui.screens.AccountsScreen
import com.psyche.app.ui.screens.ActivityScreen
import com.psyche.app.ui.screens.BudgetsScreen
import com.psyche.app.ui.screens.HomeScreen
import com.psyche.app.ui.sheets.AccountEditorSheet
import com.psyche.app.ui.sheets.AllocationsSheet
import com.psyche.app.ui.sheets.BudgetEditorSheet
import com.psyche.app.ui.sheets.CategoriesSheet
import com.psyche.app.ui.sheets.ContributionSheet
import com.psyche.app.ui.sheets.EntryDetailSheet
import com.psyche.app.ui.sheets.GoalEditorSheet
import com.psyche.app.ui.sheets.NotificationIntroSheet
import com.psyche.app.ui.sheets.SettingsSheet
import com.psyche.app.ui.sheets.SetupSheet
import com.psyche.app.ui.sheets.TransferSheet
import com.psyche.app.ui.theme.LocalPsychePalette
import com.psyche.app.util.ActivityRow
import com.psyche.app.util.BudgetMath
import com.psyche.app.util.GoalProgress
import java.time.YearMonth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private sealed interface Overlay {
    data object Capture : Overlay
    data object Settings : Overlay
    data object Setup : Overlay
    data object Transfer : Overlay
    data object Categories : Overlay
    data class BudgetEditor(val month: YearMonth) : Overlay
    data class Allocations(val month: YearMonth) : Overlay
    data class AccountEditor(val accountId: Long?) : Overlay
    data class GoalEditor(val goalId: Long?) : Overlay
    data class Contribution(val goalId: Long) : Overlay
    data class Entry(val row: ActivityRow) : Overlay
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PsycheApp(
    viewModel: FinanceViewModel,
    onRequestNotificationPermission: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val palette = LocalPsychePalette.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pendingFeedback by remember { mutableStateOf<List<UiEvent>>(emptyList()) }
    var activeFeedback by remember { mutableStateOf<UiEvent?>(null) }
    var visibleFeedback by remember { mutableStateOf<UiEvent?>(null) }
    var tabName by rememberSaveable { mutableStateOf(MainTab.HOME.name) }
    val tab = MainTab.valueOf(tabName)
    var overlay by remember { mutableStateOf<Overlay?>(null) }
    var introDismissed by rememberSaveable { mutableStateOf(false) }
    var lifecycleTick by remember { mutableIntStateOf(0) }
    val notificationsAllowed = remember(context, state.settings.notificationsEnabled, lifecycleTick) {
        val permission = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        val notificationManager = androidx.core.app.NotificationManagerCompat.from(context)
        val systemEnabled = notificationManager.areNotificationsEnabled()
        val channel = notificationManager.getNotificationChannel(BudgetNotifier.CHANNEL_ID)
        val channelEnabled = channel == null || channel.importance != android.app.NotificationManager.IMPORTANCE_NONE
        permission && systemEnabled && channelEnabled
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        lifecycleTick += 1
        viewModel.refreshToday()
    }

    val showNotificationIntro = !state.isLoading &&
        !state.settings.notificationPromptSeen &&
        !state.settings.notificationsEnabled &&
        !state.needsSetup &&
        state.snapshot.activeAccounts.isNotEmpty() &&
        overlay == null &&
        !introDismissed

    fun copySummary() {
        val summary = state.home?.summary ?: return
        val clipboard = context.getSystemService(ClipboardManager::class.java)
        clipboard?.setPrimaryClip(
            ClipData.newPlainText(
                "Psyche period summary",
                BudgetMath.periodSummary(summary),
            ),
        )
        viewModel.notify("Period summary copied")
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            pendingFeedback = pendingFeedback + event
        }
    }

    LaunchedEffect(activeFeedback, pendingFeedback) {
        if (activeFeedback == null && pendingFeedback.isNotEmpty()) {
            activeFeedback = pendingFeedback.first()
            pendingFeedback = pendingFeedback.drop(1)
        }
    }

    LaunchedEffect(activeFeedback) {
        val current = activeFeedback ?: return@LaunchedEffect
        visibleFeedback = current
        delay(if (current.actionLabel != null) 6_000L else 3_500L)
        if (activeFeedback == current) activeFeedback = null
    }

    BackHandler(enabled = tab != MainTab.HOME && overlay == null && !showNotificationIntro) {
        tabName = MainTab.HOME.name
    }

    Box(modifier = Modifier.fillMaxSize()) {
    Scaffold(
        containerColor = palette.ground,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            PsycheBottomBar(
                selected = tab,
                onSelect = {
                    tabName = it.name
                    overlay = null
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (state.snapshot.activeAccounts.isEmpty()) {
                        viewModel.notify("Add a starting account before logging money.")
                        overlay = Overlay.AccountEditor(null)
                    } else {
                        overlay = Overlay.Capture
                    }
                },
                modifier = Modifier.size(60.dp),
                shape = CircleShape,
                containerColor = palette.alert,
                contentColor = palette.onAlert,
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 8.dp,
                    pressedElevation = 12.dp,
                ),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_add),
                    contentDescription = if (state.snapshot.activeAccounts.isEmpty()) {
                        "Add an account"
                    } else {
                        "Log expense"
                    },
                    modifier = Modifier.size(26.dp),
                )
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when (tab) {
            MainTab.HOME -> HomeScreen(
                state = state,
                notificationLogOn = state.settings.notificationsEnabled && notificationsAllowed,
                onCapture = { overlay = Overlay.Capture },
                onSettings = { overlay = Overlay.Settings },
                onOpenEntry = { overlay = Overlay.Entry(it) },
                onEditCap = { overlay = Overlay.BudgetEditor(state.homeMonth) },
                onSetup = { overlay = Overlay.Setup },
                onAddAccount = { overlay = Overlay.AccountEditor(null) },
                onAccounts = { tabName = MainTab.ACCOUNTS.name },
                onActivity = { tabName = MainTab.ACTIVITY.name },
                onCopySummary = ::copySummary,
            )

            MainTab.ACTIVITY -> ActivityScreen(
                state = state,
                onOpenEntry = { overlay = Overlay.Entry(it) },
                onSettings = { overlay = Overlay.Settings },
                onPreviousMonth = { viewModel.shiftActivityMonth(-1) },
                onNextMonth = { viewModel.shiftActivityMonth(1) },
                onResetMonth = viewModel::resetActivityMonth,
                onCapture = { overlay = Overlay.Capture },
            )

            MainTab.BUDGETS -> BudgetsScreen(
                state = state,
                onSettings = { overlay = Overlay.Settings },
                onPreviousMonth = { viewModel.shiftBudgetMonth(-1) },
                onNextMonth = { viewModel.shiftBudgetMonth(1) },
                onResetMonth = viewModel::resetBudgetMonth,
                onEditCap = { overlay = Overlay.BudgetEditor(state.budgetMonth) },
                onEditAllocations = { overlay = Overlay.Allocations(state.budgetMonth) },
                onManageCategories = { overlay = Overlay.Categories },
                onCapture = { overlay = Overlay.Capture },
            )

            MainTab.ACCOUNTS -> AccountsScreen(
                state = state,
                onSettings = { overlay = Overlay.Settings },
                onAddAccount = { overlay = Overlay.AccountEditor(null) },
                onTransfer = { overlay = Overlay.Transfer },
                onEditAccount = { overlay = Overlay.AccountEditor(it.id) },
                onRestoreAccount = { account ->
                    scope.launch { viewModel.restoreAccount(account) }
                },
                onAddGoal = { overlay = Overlay.GoalEditor(null) },
                onEditGoal = { overlay = Overlay.GoalEditor(it.id) },
                onContribute = { overlay = Overlay.Contribution(it.goal.id) },
                onRestoreGoal = { goal ->
                    scope.launch { viewModel.restoreGoal(goal) }
                },
            )
        }
        }
    }

    when (val current = overlay) {
        Overlay.Capture -> {
            ModalBottomSheet(
                onDismissRequest = { overlay = null },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor = palette.ground,
                contentColor = palette.onGround,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(
                    topStart = 22.dp,
                    topEnd = 22.dp,
                ),
            ) {
                CaptureContent(
                    viewModel = viewModel,
                    state = state,
                    fromNotification = false,
                    onDismiss = { overlay = null },
                    onTransfer = { overlay = Overlay.Transfer },
                )
            }
        }

        Overlay.Settings -> SettingsSheet(
            viewModel = viewModel,
            state = state,
            notificationPermissionGranted = notificationsAllowed,
            onCopySummary = ::copySummary,
            onEnableNotifications = onRequestNotificationPermission,
            onOpenNotificationSettings = onOpenNotificationSettings,
            onDismiss = { overlay = null },
        )

        Overlay.Setup -> SetupSheet(
            viewModel = viewModel,
            state = state,
            onDismiss = { overlay = null },
        )

        Overlay.Categories -> CategoriesSheet(
            viewModel = viewModel,
            categories = state.snapshot.categories,
            onDismiss = { overlay = null },
        )

        is Overlay.BudgetEditor -> {
            val month = current.month
            val cap = state.snapshot.budgets[month]?.capCents
            val spent = BudgetMath.summarize(
                month = month,
                capCents = cap,
                entries = state.snapshot.entries,
                today = state.today,
            ).spentCents
            BudgetEditorSheet(
                viewModel = viewModel,
                month = month,
                capCents = cap,
                previousCapCents = state.snapshot.budgets[month.minusMonths(1)]?.capCents,
                spentCents = spent,
                onDismiss = { overlay = null },
            )
        }

        is Overlay.Allocations -> {
            val month = current.month
            val cap = state.snapshot.budgets[month]?.capCents ?: 0L
            val allocations = state.snapshot.allocations[month].orEmpty()
            val categories = state.snapshot.categories.filter {
                !it.archived || allocations.containsKey(it.id)
            }
            AllocationsSheet(
                viewModel = viewModel,
                month = month,
                capCents = cap,
                categories = categories,
                allocations = allocations,
                onManageCategories = { overlay = Overlay.Categories },
                onDismiss = { overlay = null },
            )
        }

        Overlay.Transfer -> TransferSheet(
            viewModel = viewModel,
            accounts = state.snapshot.activeAccounts,
            balances = state.accountBalances,
            lastAccountId = state.settings.lastAccountId,
            onDismiss = { overlay = null },
        )

        is Overlay.AccountEditor -> {
            val account: Account? = current.accountId?.let { id ->
                state.snapshot.accounts.firstOrNull { it.id == id }
            }
            AccountEditorSheet(
                viewModel = viewModel,
                account = account,
                onDismiss = { overlay = null },
            )
        }

        is Overlay.GoalEditor -> {
            val goal: SavingsGoal? = current.goalId?.let { id ->
                state.snapshot.goals.firstOrNull { it.id == id }
            }
            GoalEditorSheet(
                viewModel = viewModel,
                goal = goal,
                onDismiss = { overlay = null },
            )
        }

        is Overlay.Contribution -> {
            val goal: GoalProgress? = state.goals.firstOrNull { it.goal.id == current.goalId }
            if (goal == null) {
                overlay = null
            } else {
                ContributionSheet(
                    viewModel = viewModel,
                    goal = goal,
                    accounts = state.snapshot.activeAccounts,
                    balances = state.accountBalances,
                    lastAccountId = state.settings.lastAccountId,
                    onDismiss = { overlay = null },
                )
            }
        }

        is Overlay.Entry -> EntryDetailSheet(
            viewModel = viewModel,
            row = current.row,
            state = state,
            onDismiss = { overlay = null },
        )

        null -> Unit
    }

    if (showNotificationIntro) {
        NotificationIntroSheet(
            onEnable = onRequestNotificationPermission,
            onDismiss = {
                introDismissed = true
                viewModel.markNotificationPromptSeen()
            },
        )
    }

    TopFeedbackBanner(
        visible = activeFeedback != null,
        message = (activeFeedback ?: visibleFeedback)?.message.orEmpty(),
        actionLabel = (activeFeedback ?: visibleFeedback)?.actionLabel,
        onAction = {
            val event = activeFeedback ?: visibleFeedback
            activeFeedback = null
            event?.followUp?.let(viewModel::handleFollowUp)
        },
        modifier = Modifier.align(Alignment.TopCenter),
    )
    }
}
