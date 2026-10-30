package com.psyche.app.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.psyche.app.CaptureActivity
import com.psyche.app.MainActivity
import com.psyche.app.R
import com.psyche.app.data.FinanceRepository
import com.psyche.app.data.FinanceSnapshot
import com.psyche.app.util.BudgetMath
import com.psyche.app.util.BudgetStatus
import com.psyche.app.util.Formats
import com.psyche.app.util.Money
import java.time.Duration
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZonedDateTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class BudgetNotifier(
    private val context: Context,
    private val repository: FinanceRepository,
) {
    private val manager = NotificationManagerCompat.from(context)
    private var scope: CoroutineScope? = null

    fun start(scope: CoroutineScope) {
        this.scope = scope
        ensureChannel()
        scope.launch {
            repository.snapshot.collectLatest { snapshot ->
                update(snapshot)
            }
        }
        scope.launch {
            while (isActive) {
                val now = ZonedDateTime.now()
                val nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay(now.zone)
                val waitMillis = Duration.between(now, nextMidnight).toMillis().coerceAtLeast(1_000L)
                delay(waitMillis + 1_000L)
                refresh()
            }
        }
    }

    /** Re-checks permission and system notification toggles after returning to the app. */
    fun refresh() {
        scope?.launch {
            runCatching { update(repository.snapshot.first()) }
        }
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = context.getString(R.string.notification_channel_description)
            setSound(null, null)
            enableVibration(false)
            setShowBadge(false)
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun update(snapshot: FinanceSnapshot) {
        val settings = snapshot.settings
        if (!settings.notificationsEnabled || !canPostNotifications()) {
            manager.cancel(NOTIFICATION_ID)
            return
        }
        val today = LocalDate.now()
        val month = YearMonth.from(today)
        val cap = snapshot.budgets[month]?.capCents
        val summary = BudgetMath.summarize(month, cap, snapshot.entries, today)
        val showAmountOnLockScreen = settings.showAmountOnLockScreen
        val body = when {
            summary.capCents == null -> "Set your cap to give the pass a number."
            summary.status == BudgetStatus.OVER -> "${Money.formatPeso(summary.overByCents)} over the cap."
            else -> "${Money.formatPeso(summary.remainingCents ?: 0)} left this month."
        }
        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val captureIntent = Intent(context, CaptureActivity::class.java).apply {
            action = CaptureActivity.ACTION_CAPTURE
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val mainPending = PendingIntent.getActivity(
            context,
            1,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val capturePending = PendingIntent.getActivity(
            context,
            2,
            captureIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_pass)
            .setColor(0xFFFFD400.toInt())
            .setContentTitle("${context.getString(R.string.notification_title)} \u00B7 ${Formats.monthYear(month)}")
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(mainPending)
            .addAction(0, context.getString(R.string.notification_action), capturePending)
            .setOngoing(true)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(
                if (showAmountOnLockScreen) {
                    NotificationCompat.VISIBILITY_PUBLIC
                } else {
                    NotificationCompat.VISIBILITY_SECRET
                },
            )
            .build()
        runCatching { manager.notify(NOTIFICATION_ID, notification) }
    }

    fun canPostNotifications(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }
        if (!manager.areNotificationsEnabled()) return false
        val channel = manager.getNotificationChannel(CHANNEL_ID)
        return channel == null || channel.importance != NotificationManager.IMPORTANCE_NONE
    }

    companion object {
        const val CHANNEL_ID = "budget_pass"
        const val NOTIFICATION_ID = 4101
    }
}
