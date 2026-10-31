package com.psyche.app.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.widget.Toast
import com.psyche.app.PsycheApplication
import com.psyche.app.R
import com.psyche.app.util.Money
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class QuickActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_QUICK) return
        val quickId = intent.getLongExtra(EXTRA_QUICK_ID, -1L)
        if (quickId <= 0) return

        val now = SystemClock.elapsedRealtime()
        synchronized(lastTaps) {
            val previous = lastTaps[quickId] ?: 0L
            if (now - previous < TAP_DEBOUNCE_MS) return
            lastTaps[quickId] = now
        }

        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val app = context.applicationContext as PsycheApplication
                val message = try {
                    val result = app.financeRepository.runQuickTransaction(quickId)
                    context.getString(
                        R.string.widget_logged,
                        result.label,
                        Money.formatPeso(result.amountCents),
                    )
                } catch (throwable: Throwable) {
                    throwable.message ?: context.getString(R.string.widget_log_failed)
                }
                withContext(Dispatchers.Main) {
                    Toast.makeText(context.applicationContext, message, Toast.LENGTH_SHORT).show()
                }
            } finally {
                runCatching { QuickWidgetRenderer.updateAll(context) }
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_QUICK = "com.psyche.app.action.QUICK_LOG"
        const val EXTRA_QUICK_ID = "com.psyche.app.extra.QUICK_ID"
        private const val TAP_DEBOUNCE_MS = 1_000L
        private val lastTaps = mutableMapOf<Long, Long>()
    }
}
