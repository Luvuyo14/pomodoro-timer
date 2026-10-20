package com.luvuyo.pomodoro

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException

// Must be declared once at top level so there is a single DataStore instance per file.
private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "pomodoro_settings"
)

/** Reads and writes the interval configuration using Jetpack DataStore. */
class SettingsRepository(private val context: Context) {

    private object Keys {
        val WORK_MINUTES = intPreferencesKey("work_minutes")
        val SHORT_BREAK_MINUTES = intPreferencesKey("short_break_minutes")
        val LONG_BREAK_MINUTES = intPreferencesKey("long_break_minutes")
        val SESSIONS_BEFORE_LONG_BREAK = intPreferencesKey("sessions_before_long_break")
    }

    /** Emits the saved config, falling back to defaults for anything not saved yet. */
    val config: Flow<PomodoroConfig> = context.settingsDataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { prefs ->
            val defaults = PomodoroConfig()
            PomodoroConfig(
                workMinutes = prefs[Keys.WORK_MINUTES] ?: defaults.workMinutes,
                shortBreakMinutes = prefs[Keys.SHORT_BREAK_MINUTES] ?: defaults.shortBreakMinutes,
                longBreakMinutes = prefs[Keys.LONG_BREAK_MINUTES] ?: defaults.longBreakMinutes,
                sessionsBeforeLongBreak = prefs[Keys.SESSIONS_BEFORE_LONG_BREAK]
                    ?: defaults.sessionsBeforeLongBreak
            ).sanitized()
        }
        .distinctUntilChanged()

    suspend fun save(config: PomodoroConfig) {
        val safe = config.sanitized()
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.WORK_MINUTES] = safe.workMinutes
            prefs[Keys.SHORT_BREAK_MINUTES] = safe.shortBreakMinutes
            prefs[Keys.LONG_BREAK_MINUTES] = safe.longBreakMinutes
            prefs[Keys.SESSIONS_BEFORE_LONG_BREAK] = safe.sessionsBeforeLongBreak
        }
    }
}