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
import com.psyche.app.util.BudgetMath
import com.psyche.app.util.BudgetStatus
import com.psyche.app.util.Formats
import com.psyche.app.util.Money
import java.time.LocalDate
import java.time.YearMonth
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
        val today = LocalDate.now()
        val month = YearMonth.from(today)
        val cap = snapshot.budgets[month]?.capCents
        val summary = BudgetMath.summarize(month, cap, snapshot.entries, today)
        val over = summary.status == BudgetStatus.OVER
        val amount = when {
            summary.capCents == null -> context.getString(R.string.widget_no_cap)
            over -> Money.formatPeso(summary.overByCents)
            else -> Money.formatPeso(summary.remainingCents ?: 0)
        }
        val amountSize = when {
            summary.capCents == null -> 17f
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
            context.getColor(if (over) R.color.widget_danger else R.color.widget_ink),
        )
        views.setTextViewText(
            R.id.widget_remaining_label,
            when {
                summary.capCents == null -> ""
                over -> context.getString(R.string.widget_over_label)
                else -> context.getString(R.string.widget_remaining_label)
            },
        )
        views.setTextViewText(
            R.id.widget_status,
            "${Formats.monthYear(month)} \u00B7 ${summary.statusLabel}",
        )
        views.setOnClickPendingIntent(R.id.widget_header, openAppPending(context))
        views.setContentDescription(
            R.id.widget_header,
            "${context.getString(R.string.widget_header_open)}. $amount ${summary.statusLabel}",
        )

        val activeAccountIds = snapshot.activeAccounts.map { it.id }.toSet()
        val actions = snapshot.activeQuickTransactions
            .filter { it.accountId in activeAccountIds }
            .take(MAX_QUICK_ACTIONS)

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
                if (actions.isEmpty()) R.string.widget_footer_empty else R.string.widget_footer_ready,
            ),
        )
        views.setOnClickPendingIntent(R.id.widget_footer, openAppPending(context))
        views.setOnClickPendingIntent(R.id.widget_empty, openAppPending(context))

        views.setViewVisibility(
            R.id.widget_row_2,
            if (actions.size > 2) View.VISIBLE else View.GONE,
        )

        actions.forEachIndexed { index, quick ->
            views.setViewVisibility(slotIds[index], View.VISIBLE)
            views.setTextViewText(labelIds[index], quick.label)
            views.setTextViewText(
                amountIds[index],
                Money.formatPeso(
                    quick.amountCents,
                    showPlus = quick.type == TransactionType.INCOME,
                ),
            )
            views.setTextColor(
                amountIds[index],
                context.getColor(R.color.widget_alert),
            )
            views.setContentDescription(
                slotIds[index],
                "${quick.label}, ${Money.formatPeso(quick.amountCents)}, tap to log",
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
