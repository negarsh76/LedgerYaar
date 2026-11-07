package com.ledgeryaar.autoimport

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ledgeryaar.data.repository.LedgerRepository
import com.ledgeryaar.data.repository.SettingsRepository
import com.ledgeryaar.domain.model.PersonType
import com.ledgeryaar.domain.period.todayDate
import com.ledgeryaar.util.formatAmount
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.plus
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.Calendar

/**
 * هر روز حدود ساعت ۱۰ صبح: قسط/بدهی که امروز یا فردا سررسید است + تراکنش‌های بررسی‌نشده.
 * با AlarmManager خود اندروید کار می‌کند تا به کتابخانهٔ WorkManager نیازی نباشد.
 */
class DailyReminderReceiver : BroadcastReceiver(), KoinComponent {
    private val repo: LedgerRepository by inject()
    private val settings: SettingsRepository by inject()

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            schedule(context)
            return
        }
        val app = context.applicationContext
        val pending = goAsync()
        ImportRunner.scope.launch {
            try {
                check(app)
            } catch (_: Exception) {
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun check(context: Context) {
        val today = todayDate()
        val tomorrow = today.plus(1, DateTimeUnit.DAY)
        val currency = settings.settings.first().currency.fa

        val loans = repo.loans().first().associateBy { it.id }
        val dueInstallments = repo.installments().first().filter { !it.paid && it.dueDate >= today && it.dueDate <= tomorrow }
        val dueDebts = repo.people().first().filter { p ->
            val d = p.dueDate
            p.type == PersonType.DEBT && !p.paid && d != null && d <= tomorrow
        }

        val lines = dueInstallments.map { i ->
            val whenTxt = if (i.dueDate == today) "امروز" else "فردا"
            "قسط ${loans[i.loanId]?.title ?: ""} (${i.number}) — ${formatAmount(i.amount)} $currency — $whenTxt"
        } + dueDebts.map { p -> "بدهی به ${p.name} — ${formatAmount(p.amount)} $currency" }
        if (lines.isNotEmpty()) {
            TxNotifications.showReminder(context, 9001, "یادآوری سررسید", lines.joinToString("\n"), "loans")
        }

        val pendingCount = repo.transactions().first().count { it.needsReview }
        if (pendingCount > 0) {
            TxNotifications.showReminder(
                context, 9002,
                "$pendingCount تراکنش منتظر توست",
                "بزن و بگو هر کدام بابت چی بوده — چند ثانیه بیشتر طول نمی‌کشد.",
                "review"
            )
        }
    }

    companion object {
        private const val ACTION_DAILY = "com.ledgeryaar.DAILY_REMINDER"

        fun schedule(context: Context) {
            val am = context.getSystemService(AlarmManager::class.java) ?: return
            val intent = Intent(context, DailyReminderReceiver::class.java).setAction(ACTION_DAILY)
            val pi = PendingIntent.getBroadcast(
                context, 7001, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val first = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 10)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
            }
            // غیردقیق: اجازهٔ خاصی لازم ندارد و باتری را هم کمتر مصرف می‌کند
            am.setInexactRepeating(AlarmManager.RTC_WAKEUP, first.timeInMillis, AlarmManager.INTERVAL_DAY, pi)
        }
    }
}
