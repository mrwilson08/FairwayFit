package com.fairwayfit.app.reminders

import android.Manifest
import android.annotation.SuppressLint
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
import androidx.work.CoroutineWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.fairwayfit.app.FairwayFitApp
import com.fairwayfit.app.MainActivity
import com.fairwayfit.app.R
import com.fairwayfit.app.data.AppSettings
import com.fairwayfit.app.data.SettingsStore
import com.fairwayfit.app.domain.Plan
import com.fairwayfit.app.domain.estimatedMinutes
import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

object ReminderScheduler {
    const val CHANNEL_ID = "workout_reminders"
    private const val TAG = "workout_reminder"

    fun createChannel(context: Context) {
        val channel = NotificationChannel(CHANNEL_ID, "Workout reminders", NotificationManager.IMPORTANCE_DEFAULT)
        channel.description = "Morning nudges on your training days"
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    /** Cancels any pending reminder and schedules the next one from [settings]. Call from app/UI code. */
    fun apply(context: Context, settings: AppSettings) {
        WorkManager.getInstance(context).cancelAllWorkByTag(TAG)
        enqueueNext(context, settings)
    }

    /** Schedules the next reminder without cancelling anything (safe to call from the worker itself). */
    fun enqueueNext(context: Context, settings: AppSettings) {
        if (!settings.remindersOn || settings.days.isEmpty()) return
        val now = ZonedDateTime.now()
        val delayMs = Duration.between(now, nextTrigger(settings, now)).toMillis()
        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .addTag(TAG)
            .build()
        WorkManager.getInstance(context).enqueue(request)
    }

    fun nextTrigger(settings: AppSettings, now: ZonedDateTime): ZonedDateTime {
        for (offset in 0..7) {
            val date = now.toLocalDate().plusDays(offset.toLong())
            if (date.dayOfWeek.value in settings.days) {
                val candidate = date.atTime(settings.hour, settings.minute).atZone(now.zone)
                if (candidate.isAfter(now)) return candidate
            }
        }
        error("unreachable: days is non-empty so a trigger must exist within a week")
    }
}

class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val settings = SettingsStore(applicationContext).current()
        if (!settings.remindersOn) return Result.success()
        notify(settings)
        ReminderScheduler.enqueueNext(applicationContext, settings)
        // The timeline pin we just notified about is now in the past: point it at the next session.
        (applicationContext as FairwayFitApp).container.pebble.syncSessionPin(settings)
        return Result.success()
    }

    @SuppressLint("MissingPermission")
    private fun notify(settings: AppSettings) {
        val granted = Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) return

        val workout = Plan.sessionFor(settings.days, LocalDate.now())
        val body = if (workout != null) {
            "Today: ${workout.name} · about ${workout.estimatedMinutes} min"
        } else {
            "Time to move — your session is waiting."
        }
        val openApp = PendingIntent.getActivity(
            applicationContext, 0,
            Intent(applicationContext, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notification = NotificationCompat.Builder(applicationContext, ReminderScheduler.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Time to train")
            .setContentText(body)
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(applicationContext).notify(1001, notification)
    }
}
