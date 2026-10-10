package com.fairwayfit.app.data.pebble

import android.content.Context
import com.fairwayfit.app.data.AppSettings
import com.fairwayfit.app.data.Repository
import com.fairwayfit.app.data.SettingsStore
import com.fairwayfit.app.domain.Plan
import com.fairwayfit.app.domain.estimatedMinutes
import com.fairwayfit.app.reminders.ReminderScheduler
import io.rebble.pebblekit2.client.DefaultPebbleSender
import io.rebble.pebblekit2.common.model.PebbleDictionary
import io.rebble.pebblekit2.common.model.ReceiveResult
import io.rebble.pebblekit2.common.model.TimelineLayout
import io.rebble.pebblekit2.common.model.TimelineLayoutType
import io.rebble.pebblekit2.common.model.TimelinePin
import io.rebble.pebblekit2.common.model.TimelineReminder
import io.rebble.pebblekit2.common.model.TimelineResult
import io.rebble.pebblekit2.common.model.TransmissionResult
import io.rebble.pebblekit2.common.model.WatchIdentifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.DayOfWeek
import java.time.Instant as JavaInstant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/**
 * The only class that talks to the Pebble watch. It sends the workout and daily summary, receives
 * button presses, and keeps one timeline pin for the next scheduled session.
 *
 * Everything is a no-op unless the user has switched Pebble on in Settings. Pebble is optional, so a
 * failed send never throws; the result is shown on the Settings screen via [status] instead.
 */
class PebbleManager(
    context: Context,
    private val repository: Repository,
    private val settings: SettingsStore,
    private val scope: CoroutineScope
) {
    private val sender = DefaultPebbleSender(context.applicationContext)

    // Messages go out one at a time and in order.
    private val sendLock = Mutex()

    private val _commands = MutableSharedFlow<WatchCommand>(extraBufferCapacity = 8)

    /** Button presses from the watch. Only the workout player collects this. */
    val commands: SharedFlow<WatchCommand> = _commands.asSharedFlow()

    private val _status = MutableStateFlow("Nothing sent yet.")
    val status: StateFlow<String> = _status.asStateFlow()

    /** The latest workout state while a workout is running, resent if the watchapp is reopened. */
    @Volatile
    private var activeWorkout: PebbleDictionary? = null

    // ------------------------------------------------------------- workout

    /** Opens the watchapp so it is ready to show the workout. */
    fun startWorkoutSession() {
        scope.launch {
            if (!settings.current().pebbleOn) return@launch
            record(sender.startAppOnTheWatch(PebbleProtocol.APP_UUID))
        }
    }

    fun showWorkout(state: WatchWorkoutState) {
        val dictionary = state.toDictionary()
        activeWorkout = dictionary
        scope.launch { send(dictionary) }
    }

    /** The workout screen closed: put the watch back on the daily summary. */
    fun endWorkoutSession() {
        activeWorkout = null
        scope.launch { sendSummary() }
    }

    // ----------------------------------------------------- from the watch

    fun onWatchAppOpened() {
        scope.launch { send(activeWorkout ?: buildSummary().toDictionary()) }
    }

    fun onWatchMessage(data: PebbleDictionary): ReceiveResult {
        val command = data.toWatchCommand() ?: return ReceiveResult.Nack
        _commands.tryEmit(command)
        return ReceiveResult.Ack
    }

    // ------------------------------------------------------------- summary

    suspend fun sendSummary() {
        send(buildSummary().toDictionary())
    }

    private suspend fun buildSummary(): WatchSummary {
        val config = settings.current()
        val today = LocalDate.now()
        val (date, workout) = Plan.next(config.days, today)
        val dayText = if (date == today) {
            "Today"
        } else {
            date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
        }

        val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val weekDone = repository.workoutLogs.first().count {
            !it.dateMillis.toLocalDate().isBefore(weekStart)
        }
        val weekGoal = if (config.days.isEmpty()) 3 else config.days.size

        return WatchSummary(
            title = workout.name,
            detail = "$dayText · ${workout.estimatedMinutes} min",
            score = repository.sessions.first().lastOrNull()?.overall ?: PebbleProtocol.NO_SCORE,
            week = "$weekDone/$weekGoal this week"
        )
    }

    // ------------------------------------------------------- timeline pin

    /** Inserts or updates the single "next session" pin. Call after the reminder schedule changes. */
    suspend fun syncSessionPin(config: AppSettings) {
        if (!config.pebbleOn) return
        if (!config.remindersOn || config.days.isEmpty()) {
            removeSessionPin()
            return
        }
        val trigger = ReminderScheduler.nextTrigger(config, ZonedDateTime.now())
        val workout = checkNotNull(Plan.sessionFor(config.days, trigger.toLocalDate())) {
            "nextTrigger returned a day that is not a training day"
        }
        val start = Instant.fromEpochMilliseconds(trigger.toInstant().toEpochMilli())

        val pin = TimelinePin(
            id = SESSION_PIN_ID,
            startTime = start,
            duration = workout.estimatedMinutes.minutes,
            layout = TimelineLayout(
                type = TimelineLayoutType.GENERIC_PIN,
                title = workout.name,
                subtitle = "FairwayFit",
                body = "${workout.tagline}. About ${workout.estimatedMinutes} min.",
                tinyIcon = "system://images/SCHEDULED_EVENT"
            ),
            reminders = listOf(
                TimelineReminder(
                    time = start,
                    layout = TimelineLayout(
                        type = TimelineLayoutType.GENERIC_REMINDER,
                        title = "Time to train",
                        subtitle = workout.name,
                        tinyIcon = "system://images/NOTIFICATION_REMINDER"
                    )
                )
            )
        )
        val result = sender.insertTimelinePin(PebbleProtocol.APP_UUID, pin)
        _status.value = describe(result)
    }

    /** Removes the pin. Works even when Pebble is switched off, so turning it off cleans up. */
    suspend fun removeSessionPin() {
        sender.deleteTimelinePin(PebbleProtocol.APP_UUID, SESSION_PIN_ID)
    }

    // ---------------------------------------------------------------- test

    /** For the Settings "Send test" button: opens the app, then sends the summary and the pin. */
    fun sendTest() {
        scope.launch {
            if (!settings.current().pebbleOn) return@launch
            record(sender.startAppOnTheWatch(PebbleProtocol.APP_UUID))
            delay(OPEN_APP_WAIT_MS)
            sendSummary()
            syncSessionPin(settings.current())
        }
    }

    // ------------------------------------------------------------- plumbing

    private suspend fun send(dictionary: PebbleDictionary) {
        if (!settings.current().pebbleOn) return
        val result = sendLock.withLock { sender.sendDataToPebble(PebbleProtocol.APP_UUID, dictionary) }
        record(result)
    }

    private fun record(result: Map<WatchIdentifier, TransmissionResult>?) {
        _status.value = when {
            result == null -> "Pebble app not found. Install the Core Devices Pebble app."
            result.isEmpty() -> "No watch connected."
            else -> {
                val failure = result.values.firstOrNull { it != TransmissionResult.Success }
                if (failure == null) "Sent to watch at ${LocalTime.now().format(TIME_FORMAT)}." else describe(failure)
            }
        }
    }

    private fun describe(result: TransmissionResult): String = when (result) {
        TransmissionResult.Success -> "Sent to watch."
        TransmissionResult.FailedWatchNotConnected -> "Watch is not connected."
        TransmissionResult.FailedWatchNacked -> "Watch rejected the message. Is the FairwayFit watchapp installed?"
        TransmissionResult.FailedTimeout -> "Watch did not respond in time."
        TransmissionResult.FailedDifferentAppOpen -> "Open the FairwayFit app on your watch, then try again."
        TransmissionResult.FailedNoPermissions ->
            "Not allowed: the watchapp's package.json must list com.fairwayfit.app as its companion app."
        is TransmissionResult.Unknown -> "Unknown send result: ${result.message ?: "no details"}"
    }

    private fun describe(result: TimelineResult): String = when (result) {
        TimelineResult.Success -> "Timeline pin updated."
        TimelineResult.FailedNoPermissions ->
            "Pin not allowed: the watchapp's package.json must list com.fairwayfit.app as its companion app."
        TimelineResult.FailedUnsupportedAction -> "Your Pebble app does not support timeline pins yet."
        TimelineResult.FailedUnknownPin -> "Pin not found."
        TimelineResult.FailedNoPebbleApp -> "Pebble app not found. Install the Core Devices Pebble app."
        is TimelineResult.Unknown -> "Unknown pin result: ${result.message ?: "no details"}"
    }

    private fun Long.toLocalDate(): LocalDate =
        JavaInstant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()

    private companion object {
        const val SESSION_PIN_ID = "next-session"
        const val OPEN_APP_WAIT_MS = 2_000L
        val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("h:mm a")
    }
}
