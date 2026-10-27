package com.luvuyo.pomodoro

import android.content.Context
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.auth.api.identity.AuthorizationResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.IOException
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * Writes finished work sessions to Google Calendar. Sessions go into a local queue first and
 * are removed only once Google has accepted them, so nothing is lost while offline.
 */
class CalendarSync(context: Context, private val scope: CoroutineScope) {

    private val appContext = context.applicationContext
    private val store = CalendarStore(appContext)
    private val api = GoogleCalendarApi()
    private val mutex = Mutex()

    private val issue = MutableStateFlow(CalendarIssue.NONE)
    private val busy = MutableStateFlow(false)

    val uiState: StateFlow<CalendarUiState> =
        combine(store.state, issue, busy) { saved, currentIssue, isBusy ->
            CalendarUiState(
                connected = saved.connected,
                pendingCount = saved.pending.size,
                busy = isBusy,
                issue = currentIssue
            )
        }.stateIn(scope, SharingStarted.Eagerly, CalendarUiState())

    /** Called with the result of the Google permission step (screen or silent). */
    fun onAuthorizationResult(result: AuthorizationResult) {
        scope.launch {
            val token = result.accessToken
            if (token == null || !CalendarAuth.hasCalendarAccess(result)) {
                issue.value = CalendarIssue.CONNECT_FAILED
                return@launch
            }
            store.setConnected(true)
            issue.value = CalendarIssue.NONE
            flush(token)
        }
    }

    fun onConnectFailed() {
        issue.value = CalendarIssue.CONNECT_FAILED
    }

    /** Stops logging and forgets the calendar. Events already in Google Calendar stay there. */
    fun disconnect() {
        scope.launch {
            store.clearAll()
            issue.value = CalendarIssue.NONE
        }
    }

    /** Queues a finished work session and tries to send it right away. */
    fun logWorkSession(session: PendingSession) {
        scope.launch {
            if (!store.current().connected) return@launch
            store.addPending(session)
            flush(knownToken = null)
        }
    }

    /** Retries anything left in the queue, for example after the app starts. */
    fun flushPending() {
        scope.launch {
            if (store.current().connected) flush(knownToken = null)
        }
    }

    private suspend fun flush(knownToken: String?) {
        mutex.withLock {
            val initial = store.current()
            if (!initial.connected) return
            // Nothing to send and no fresh token to set up the calendar with.
            if (knownToken == null && initial.pending.isEmpty()) return

            busy.value = true
            try {
                val token = knownToken ?: silentToken()
                if (token == null) {
                    issue.value = CalendarIssue.NEEDS_RECONNECT
                    return
                }

                var calendarId = initial.calendarId ?: createCalendar(token)

                for (session in initial.pending) {
                    try {
                        api.insertEvent(token, calendarId, eventFor(session))
                    } catch (e: CalendarApiException) {
                        when (e.statusCode) {
                            // Already created by an earlier attempt whose reply was lost.
                            409 -> Unit
                            // The user deleted the calendar: make a new one and retry once.
                            404 -> {
                                calendarId = createCalendar(token)
                                api.insertEvent(token, calendarId, eventFor(session))
                            }
                            else -> throw e
                        }
                    }
                    store.removePending(session)
                }
                issue.value = CalendarIssue.NONE
            } catch (e: CalendarApiException) {
                // 401 means the token was rejected. Anything else (rate limit, server error)
                // is temporary, so the sessions stay queued for the next attempt.
                issue.value = if (e.statusCode == 401) CalendarIssue.NEEDS_RECONNECT
                else CalendarIssue.SYNC_PENDING
            } catch (e: IOException) {
                issue.value = CalendarIssue.SYNC_PENDING
            } finally {
                busy.value = false
            }
        }
    }

    /** A token without any screen. Null if the user has to approve access again. */
    private suspend fun silentToken(): String? = try {
        val result = CalendarAuth.authorize(appContext)
        if (result.hasResolution() || !CalendarAuth.hasCalendarAccess(result)) null
        else result.accessToken
    } catch (e: ApiException) {
        null
    }

    private suspend fun createCalendar(token: String): String {
        val id = api.createCalendar(
            accessToken = token,
            name = appContext.getString(R.string.app_name),
            description = appContext.getString(R.string.calendar_description),
            timeZoneId = ZoneId.systemDefault().id
        )
        store.setCalendarId(id)
        return id
    }

    private fun eventFor(session: PendingSession): CalendarEventData {
        val zone = ZoneId.systemDefault()
        val endMillis = maxOf(session.endMillis, session.startMillis + 1_000L)
        return CalendarEventData(
            id = session.eventId,
            title = appContext.getString(R.string.calendar_event_title),
            description = appContext.getString(
                R.string.calendar_event_description,
                session.sessionNumber,
                session.sessionsInCycle
            ),
            start = rfc3339(session.startMillis, zone),
            end = rfc3339(endMillis, zone),
            timeZoneId = zone.id
        )
    }

    private fun rfc3339(millis: Long, zone: ZoneId): String =
        ZonedDateTime.ofInstant(Instant.ofEpochMilli(millis), zone)
            .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
}