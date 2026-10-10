package com.fairwayfit.app.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

/** [days] uses java.time.DayOfWeek values: Monday = 1 ... Sunday = 7. */
data class AppSettings(
    val remindersOn: Boolean = false,
    val hour: Int = 6,
    val minute: Int = 30,
    val days: Set<Int> = setOf(1, 3, 5),
    val pebbleOn: Boolean = false
)

class SettingsStore(context: Context) {
    private val store = context.applicationContext.dataStore

    private object Keys {
        val remindersOn = booleanPreferencesKey("reminders_on")
        val hour = intPreferencesKey("reminder_hour")
        val minute = intPreferencesKey("reminder_minute")
        val days = stringPreferencesKey("reminder_days")
        val pebbleOn = booleanPreferencesKey("pebble_on")
    }

    val settings: Flow<AppSettings> = store.data.map { it.toSettings() }

    /** Applies [transform] to the stored settings and returns the new value. */
    suspend fun update(transform: (AppSettings) -> AppSettings): AppSettings {
        var result = AppSettings()
        store.edit { prefs ->
            result = transform(prefs.toSettings())
            prefs[Keys.remindersOn] = result.remindersOn
            prefs[Keys.hour] = result.hour
            prefs[Keys.minute] = result.minute
            prefs[Keys.days] = result.days.sorted().joinToString(",")
            prefs[Keys.pebbleOn] = result.pebbleOn
        }
        return result
    }

    suspend fun current(): AppSettings = settings.first()

    private fun Preferences.toSettings(): AppSettings {
        val defaults = AppSettings()
        val daysText = this[Keys.days]
        val days = when {
            daysText == null -> defaults.days
            daysText.isEmpty() -> emptySet()
            else -> daysText.split(",").map { it.toInt() }.toSet()
        }
        return AppSettings(
            remindersOn = this[Keys.remindersOn] ?: defaults.remindersOn,
            hour = this[Keys.hour] ?: defaults.hour,
            minute = this[Keys.minute] ?: defaults.minute,
            days = days,
            pebbleOn = this[Keys.pebbleOn] ?: defaults.pebbleOn
        )
    }
}
