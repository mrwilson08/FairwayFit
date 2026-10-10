package com.fairwayfit.app.data.pebble

import io.rebble.pebblekit2.common.model.PebbleDictionary
import io.rebble.pebblekit2.common.model.PebbleDictionaryItem
import java.util.UUID

/**
 * The message contract between this app and the FairwayFit watchapp.
 *
 * KEEP IN SYNC with pebble-watchapp/package.json ("messageKeys" and "uuid") and
 * pebble-watchapp/src/c/main.c (the PHASE_ and CMD_ values). Every number is sent as an Int32 so the
 * watch can always read it with `tuple->value->int32`.
 */
object PebbleProtocol {
    val APP_UUID: UUID = UUID.fromString("89E32D04-9641-4ACE-9AA5-70DFCD8150B7")

    // Message keys (must match package.json "messageKeys")
    const val KEY_TYPE = 0u
    const val KEY_TITLE = 1u
    const val KEY_DETAIL = 2u
    const val KEY_PHASE = 3u
    const val KEY_AMOUNT = 4u
    const val KEY_PROGRESS = 5u
    const val KEY_SCORE = 6u
    const val KEY_WEEK = 7u
    const val KEY_COMMAND = 8u

    // Message types, phone -> watch
    const val TYPE_WORKOUT = 1
    const val TYPE_SUMMARY = 2

    // Workout phases, phone -> watch
    const val PHASE_REPS = 1
    const val PHASE_HOLD_READY = 2
    const val PHASE_HOLD_RUNNING = 3
    const val PHASE_REST = 4
    const val PHASE_DONE = 5

    // Commands, watch -> phone
    const val CMD_PRIMARY = 1
    const val CMD_ADD_REST = 2

    /** Sent as the score when the user has not taken the fitness test yet. */
    const val NO_SCORE = -1
}

// Keep strings short: the watch inbox buffer is small (see the 256-byte inbox in main.c).
private const val MAX_TEXT = 32

enum class WatchCommand { PRIMARY, ADD_REST }

/** What the watch shows during a workout. [amount] is the rep count, or the seconds left on a hold or rest. */
data class WatchWorkoutState(
    val phase: Int,
    val title: String,
    val detail: String,
    val amount: Int,
    val progress: Int
)

data class WatchSummary(
    val title: String,
    val detail: String,
    val score: Int,
    val week: String
)

private fun String.clip(): String = if (length <= MAX_TEXT) this else take(MAX_TEXT - 1) + "."

fun WatchWorkoutState.toDictionary(): PebbleDictionary = mapOf(
    PebbleProtocol.KEY_TYPE to PebbleDictionaryItem.Int32(PebbleProtocol.TYPE_WORKOUT),
    PebbleProtocol.KEY_TITLE to PebbleDictionaryItem.Text(title.clip()),
    PebbleProtocol.KEY_DETAIL to PebbleDictionaryItem.Text(detail.clip()),
    PebbleProtocol.KEY_PHASE to PebbleDictionaryItem.Int32(phase),
    PebbleProtocol.KEY_AMOUNT to PebbleDictionaryItem.Int32(amount),
    PebbleProtocol.KEY_PROGRESS to PebbleDictionaryItem.Int32(progress)
)

fun WatchSummary.toDictionary(): PebbleDictionary = mapOf(
    PebbleProtocol.KEY_TYPE to PebbleDictionaryItem.Int32(PebbleProtocol.TYPE_SUMMARY),
    PebbleProtocol.KEY_TITLE to PebbleDictionaryItem.Text(title.clip()),
    PebbleProtocol.KEY_DETAIL to PebbleDictionaryItem.Text(detail.clip()),
    PebbleProtocol.KEY_SCORE to PebbleDictionaryItem.Int32(score),
    PebbleProtocol.KEY_WEEK to PebbleDictionaryItem.Text(week.clip())
)

/** Reads the command number from a watch message. Received numbers arrive as Int32 or UInt32. */
fun PebbleDictionary.toWatchCommand(): WatchCommand? {
    val code = when (val item = this[PebbleProtocol.KEY_COMMAND]) {
        is PebbleDictionaryItem.Int32 -> item.value
        is PebbleDictionaryItem.UInt32 -> item.value.toInt()
        else -> return null
    }
    return when (code) {
        PebbleProtocol.CMD_PRIMARY -> WatchCommand.PRIMARY
        PebbleProtocol.CMD_ADD_REST -> WatchCommand.ADD_REST
        else -> null
    }
}
