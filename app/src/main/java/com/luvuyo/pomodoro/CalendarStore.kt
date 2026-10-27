package com.luvuyo.pomodoro

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.calendarDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "calendar_state"
)

/** Remembers whether Calendar is connected, which calendar to use, and sessions still to sync. */
class CalendarStore(private val context: Context) {

    data class State(
        val connected: Boolean = false,
        val calendarId: String? = null,
        val pending: List<PendingSession> = emptyList()
    )

    private object Keys {
        val CONNECTED = booleanPreferencesKey("connected")
        val CALENDAR_ID = stringPreferencesKey("calendar_id")
        val PENDING = stringSetPreferencesKey("pending_sessions")
    }

    val state: Flow<State> = context.calendarDataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { prefs ->
            State(
                connected = prefs[Keys.CONNECTED] ?: false,
                calendarId = prefs[Keys.CALENDAR_ID],
                pending = (prefs[Keys.PENDING] ?: emptySet())
                    .mapNotNull { PendingSession.decode(it) }
                    .sortedBy { it.startMillis }
            )
        }
        .distinctUntilChanged()

    suspend fun current(): State = state.first()

    suspend fun setConnected(connected: Boolean) {
        context.calendarDataStore.edit { it[Keys.CONNECTED] = connected }
    }

    suspend fun setCalendarId(id: String) {
        context.calendarDataStore.edit { it[Keys.CALENDAR_ID] = id }
    }

    suspend fun addPending(session: PendingSession) {
        context.calendarDataStore.edit {
            it[Keys.PENDING] = (it[Keys.PENDING] ?: emptySet()) + session.encode()
        }
    }

    suspend fun removePending(session: PendingSession) {
        context.calendarDataStore.edit {
            it[Keys.PENDING] = (it[Keys.PENDING] ?: emptySet()) - session.encode()
        }
    }

    /** Forgets the connection, the calendar and any queued sessions. */
    suspend fun clearAll() {
        context.calendarDataStore.edit { it.clear() }
    }
}