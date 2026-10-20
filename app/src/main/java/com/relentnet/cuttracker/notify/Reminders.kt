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

    /** Sets both alarms, or cancels the ones that are off (and both before onboarding is done). */
    fun schedule(context: Context, s: Settings) {
        val am = context.getSystemService(AlarmManager::class.java)
        if (s.onboarded && s.weighReminder) scheduleOne(context, KIND_WEIGH, s.weighInTime) else am.cancel(pending(context, KIND_WEIGH))
        if (s.onboarded && s.pickReminder) scheduleOne(context, KIND_PICK, s.pickTime) else am.cancel(pending(context, KIND_PICK))
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

    fun post(context: Context, kind: String, day: java.time.LocalDate? = null) {
        ensureChannel(context)
        val open = PendingIntent.getActivity(
            context, if (kind == KIND_WEIGH) 11 else 12, Intent(context, MainActivity::class.java).putExtra(EXTRA_TAB, "today").putExtra(EXTRA_WEIGH, kind == KIND_WEIGH).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val n = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(if (kind == KIND_WEIGH) "Weigh in" else "Pick a day")
            .setContentText(if (kind == KIND_WEIGH) "Before food. Log it, ignore the number." else "No day picked yet. Tap Yep, Rest day, then weigh in and hit protein to keep the chain.")
            .setStyle(NotificationCompat.BigTextStyle())
            .setContentIntent(open)
            .setAutoCancel(true)
            .apply {
                // The same two answers as Today's evening card, right from the shade.
                if (kind == KIND_PICK) {
                    addAction(0, "Yep, Rest day", pickIntent(context, "REST", day))
                    addAction(0, "I climbed", pickIntent(context, "CLIMB", day))
                }
            }
            .build()
        runCatching { context.getSystemService(NotificationManager::class.java).notify(if (kind == KIND_WEIGH) 1 else 2, n) }
    }
}

private fun pickIntent(context: Context, mode: String, day: java.time.LocalDate?): PendingIntent = PendingIntent.getBroadcast(
    context, if (mode == "REST") 21 else 22,
    Intent(context, PickReceiver::class.java).putExtra(PickReceiver.MODE, mode).putExtra(PickReceiver.DAY, day?.toString()),
    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
)

/** A button on the pick-a-day reminder: sets today's kind the way the evening card does, then clears the reminder. */
class PickReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val mode = intent.getStringExtra(MODE)?.let { m -> com.relentnet.cuttracker.data.DayMode.entries.firstOrNull { it.name == m } } ?: return
        val app = App.of(context)
        val result = goAsync()
        Thread {
            runBlocking {
                val today = app.repo.today()
                // Only for the day it was posted on, and only if that day is still unset: a stale notification changes nothing.
                val day = intent.getStringExtra(DAY)
                if ((day == null || day == today.toString()) && app.db.dayLogs().get(today)?.mode == null) {
                    val pre = if (mode == com.relentnet.cuttracker.data.DayMode.CLIMB) setOf(com.relentnet.cuttracker.data.ProgramSpec.ITEM_CLIMB) else emptySet()
                    app.repo.setMode(mode, pre)
                }
            }
            runCatching { context.getSystemService(NotificationManager::class.java).cancel(2) }
            result.finish()
        }.start()
    }

    companion object { const val MODE = "mode"; const val DAY = "day" }
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
                    // Weighed in already, on the widget or early in the app: nothing to nudge.
                    Reminders.KIND_WEIGH -> if (s.weighReminder && app.db.weighIns().all().none { it.date == app.repo.today() }) Reminders.post(context, kind)
                    Reminders.KIND_PICK -> {
                        val today = app.repo.today()
                        val onRoute = !today.isBefore(s.startDate) && today.isBefore(s.endDate) && s.checkpoints.none { it.free && it.localDate == today }
                        if (s.pickReminder && onRoute && app.db.dayLogs().get(today)?.mode == null) Reminders.post(context, kind, today)
                    }
                }
                Reminders.schedule(context, s)
            }
            result.finish()
        }.start()
    }
}

/** Alarms do not survive a reboot, an update or a clock or time zone change; put them back and roll the day. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = App.of(context)
        val result = goAsync()
        Thread {
            runBlocking { Reminders.schedule(context, app.settings.current()); app.repo.refreshToday() }
            result.finish()
        }.start()
    }
}
