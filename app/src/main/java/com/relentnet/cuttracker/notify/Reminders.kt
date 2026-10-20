package com.relentnet.cuttracker.notify

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.relentnet.cuttracker.App
import com.relentnet.cuttracker.MainActivity
import com.relentnet.cuttracker.R
import com.relentnet.cuttracker.data.Settings
import kotlinx.coroutines.runBlocking
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * Two daily alarms: "Weigh in" at the weigh-in time, "Pick a day" at the pick time (posted only when no mode is set).
 * Exact when the OS allows it, otherwise an inexact while-idle alarm. Nothing else, no streak nagging.
 */
object Reminders {
    const val CHANNEL = "reminders"
    const val KIND = "kind"
    const val KIND_WEIGH = "weigh"
    const val KIND_PICK = "pick"
    const val EXTRA_TAB = "tab"
    /** The weigh-in reminder lands on Today with the weigh-in row open; the row is the one place to type it. */
    const val EXTRA_WEIGH = "weigh"

    fun ensureChannel(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CHANNEL) == null) {
            nm.createNotificationChannel(NotificationChannel(CHANNEL, "Reminders", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Weigh-in and the day pick"
            })
        }
    }

    fun schedule(context: Context, s: Settings) {
        scheduleOne(context, KIND_WEIGH, s.weighInTime)
        scheduleOne(context, KIND_PICK, s.pickTime)
    }

    private fun scheduleOne(context: Context, kind: String, at: LocalTime) {
        val am = context.getSystemService(AlarmManager::class.java)
        val now = LocalDateTime.now()
        var next = now.toLocalDate().atTime(at)
        if (!next.isAfter(now)) next = next.plusDays(1)
        val millis = next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val pi = pending(context, kind)
        val exact = Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms()
        if (exact) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pi)
        else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pi)
    }

    private fun pending(context: Context, kind: String): PendingIntent = PendingIntent.getBroadcast(
        context, if (kind == KIND_WEIGH) 1 else 2,
        Intent(context, AlarmReceiver::class.java).putExtra(KIND, kind),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    fun post(context: Context, kind: String) {
        ensureChannel(context)
        val open = PendingIntent.getActivity(
            context, 10, Intent(context, MainActivity::class.java).putExtra(EXTRA_TAB, "today").putExtra(EXTRA_WEIGH, kind == KIND_WEIGH).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val n = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(if (kind == KIND_WEIGH) "Weigh in" else "Pick a day")
            .setContentText(if (kind == KIND_WEIGH) "Before food. Log it, ignore the number." else "No day picked yet. Guessing Rest day. Weigh in and hit protein and the chain is safe.")
            .setStyle(NotificationCompat.BigTextStyle())
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        runCatching { context.getSystemService(NotificationManager::class.java).notify(if (kind == KIND_WEIGH) 1 else 2, n) }
    }
}

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val kind = intent.getStringExtra(Reminders.KIND) ?: return
        val app = App.of(context)
        val result = goAsync()
        Thread {
            runBlocking {
                val s = app.settings.current()
                when (kind) {
                    Reminders.KIND_WEIGH -> Reminders.post(context, kind)
                    Reminders.KIND_PICK -> {
                        val today = app.repo.today()
                        if (app.db.dayLogs().get(today)?.mode == null) Reminders.post(context, kind)
                    }
                }
                Reminders.schedule(context, s)
            }
            result.finish()
        }.start()
    }
}

/** Alarms do not survive a reboot or an update; put them back. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = App.of(context)
        val result = goAsync()
        Thread {
            runBlocking { Reminders.schedule(context, app.settings.current()) }
            result.finish()
        }.start()
    }
}
