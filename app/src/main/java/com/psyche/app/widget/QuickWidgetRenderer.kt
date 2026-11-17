package com.psyche.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import com.psyche.app.MainActivity
import com.psyche.app.PsycheApplication
import com.psyche.app.R
import com.psyche.app.data.FinanceSnapshot
import com.psyche.app.data.MAX_QUICK_ACTIONS
import com.psyche.app.data.TransactionType
import com.psyche.app.util.LedgerMath
import com.psyche.app.util.Money
import com.psyche.app.util.PositionMath
import kotlinx.coroutines.flow.first

object QuickWidgetRenderer {

    private val slotIds = intArrayOf(
        R.id.widget_slot_1,
        R.id.widget_slot_2,
        R.id.widget_slot_3,
        R.id.widget_slot_4,
    )
    private val labelIds = intArrayOf(
        R.id.widget_label_1,
        R.id.widget_label_2,
        R.id.widget_label_3,
        R.id.widget_label_4,
    )
    private val amountIds = intArrayOf(
        R.id.widget_amount_1,
        R.id.widget_amount_2,
        R.id.widget_amount_3,
        R.id.widget_amount_4,
    )

    suspend fun updateAll(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(
            ComponentName(context, QuickTransactionWidget::class.java),
        )
        if (ids.isEmpty()) return
        val app = context.applicationContext as? PsycheApplication ?: return
        val snapshot = app.financeRepository.snapshot.first()
        val views = build(context, snapshot)
        ids.forEach { id -> manager.updateAppWidget(id, views) }
    }

    fun build(context: Context, snapshot: FinanceSnapshot): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_quick)
        val position = PositionMath.summarize(
            snapshot.accounts,
            snapshot.goals,
            snapshot.entries,
            debtTerms = snapshot.debtTerms,
        )
        val noAccounts = position.accountCount == 0
        val negative = position.netCents < 0
        val amount = if (noAccounts) "\u2014" else Money.formatPeso(position.netCents)
        val amountSize = when {
            noAccounts -> 17f
            amount.length > 9 -> 19f
            amount.length > 7 -> 22f
            else -> 25f
        }
        views.setTextViewText(R.id.widget_remaining, amount)
        views.setTextViewTextSize(
            R.id.widget_remaining,
            TypedValue.COMPLEX_UNIT_SP,
            amountSize,
        )
        views.setTextColor(
            R.id.widget_remaining,
            context.getColor(if (negative) R.color.widget_danger else R.color.widget_ink),
        )
        views.setTextViewText(
            R.id.widget_remaining_label,
            context.getString(
                when {
                    noAccounts -> R.string.widget_no_accounts
                    negative -> R.string.widget_short_label
                    else -> R.string.widget_net_label
                },
            ),
        )
        views.setTextViewText(
            R.id.widget_status,
            if (noAccounts) {
                context.getString(R.string.widget_no_accounts_status)
            } else {
                context.getString(
                    R.string.widget_balance_status,
                    Money.formatPeso(position.moneyCents),
                    Money.formatPeso(position.owedCents),
                )
            },
        )
        views.setOnClickPendingIntent(R.id.widget_header, openAppPending(context))
        views.setContentDescription(
            R.id.widget_header,
            if (noAccounts) {
                "${context.getString(R.string.widget_header_open)}. ${context.getString(R.string.widget_no_accounts_status)}"
            } else {
                "${context.getString(R.string.widget_header_open)}. " +
                    "Net ${Money.formatPeso(position.netCents)}. " +
                    "Money ${Money.formatPeso(position.moneyCents)}. " +
                    "Owed ${Money.formatPeso(position.owedCents)}."
            },
        )

        val activeAccountIds = snapshot.activeSpendAccounts.map { it.id }.toSet()
        val actions = snapshot.activeQuickTransactions
            .filter { it.accountId in activeAccountIds }
            .take(MAX_QUICK_ACTIONS)
        val balances = LedgerMath.accountBalances(snapshot.accounts, snapshot.entries)
            .associateBy { it.account.id }
        val lowById = actions.associate { quick ->
            val available = balances[quick.accountId]?.availableToSpendCents ?: 0L
            quick.id to (quick.type == TransactionType.EXPENSE && quick.amountCents > available)
        }
        val anyLow = lowById.values.any { it }

        views.setViewVisibility(
            R.id.widget_grid,
            if (actions.isEmpty()) View.GONE else View.VISIBLE,
        )
        views.setViewVisibility(
            R.id.widget_empty,
            if (actions.isEmpty()) View.VISIBLE else View.GONE,
        )
        views.setTextViewText(
            R.id.widget_footer,
            context.getString(
                when {
                    actions.isEmpty() -> R.string.widget_footer_empty
                    anyLow -> R.string.widget_footer_low
                    else -> R.string.widget_footer_ready
                },
            ),
        )
        views.setTextColor(
            R.id.widget_footer,
            context.getColor(if (anyLow) R.color.widget_danger else R.color.widget_faint),
        )
        views.setOnClickPendingIntent(R.id.widget_footer, openAppPending(context))
        views.setOnClickPendingIntent(R.id.widget_empty, openAppPending(context))

        views.setViewVisibility(
            R.id.widget_row_2,
            if (actions.size > 2) View.VISIBLE else View.GONE,
        )

        actions.forEachIndexed { index, quick ->
            val low = lowById[quick.id] == true
            val accountName = snapshot.accounts
                .firstOrNull { it.id == quick.accountId }
                ?.name
                ?: "the source account"
            views.setViewVisibility(slotIds[index], View.VISIBLE)
            views.setInt(
                slotIds[index],
                "setBackgroundResource",
                if (low) R.drawable.bg_widget_action_low else R.drawable.bg_widget_action,
            )
            views.setTextViewText(labelIds[index], quick.label)
            views.setTextColor(
                labelIds[index],
                context.getColor(if (low) R.color.widget_danger else R.color.widget_ink),
            )
            views.setTextViewText(
                amountIds[index],
                Money.formatPeso(
                    quick.amountCents,
                    showPlus = quick.type == TransactionType.INCOME,
                ),
            )
            views.setTextColor(
                amountIds[index],
                context.getColor(if (low) R.color.widget_danger else R.color.widget_alert),
            )
            views.setContentDescription(
                slotIds[index],
                if (low) {
                    "${quick.label}, ${Money.formatPeso(quick.amountCents)}, " +
                        "not enough available in $accountName; tap to see the shortfall"
                } else {
                    "${quick.label}, ${Money.formatPeso(quick.amountCents)}, tap to log"
                },
            )
            views.setOnClickPendingIntent(slotIds[index], quickPending(context, quick.id))
        }
        for (index in actions.size until MAX_QUICK_ACTIONS) {
            views.setViewVisibility(slotIds[index], View.GONE)
        }

        return views
    }

    private fun quickPending(context: Context, quickId: Long): PendingIntent {
        val intent = Intent(context, QuickActionReceiver::class.java).apply {
            action = QuickActionReceiver.ACTION_QUICK
            putExtra(QuickActionReceiver.EXTRA_QUICK_ID, quickId)
        }
        return PendingIntent.getBroadcast(
            context,
            (quickId % Int.MAX_VALUE).toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun openAppPending(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            1001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
