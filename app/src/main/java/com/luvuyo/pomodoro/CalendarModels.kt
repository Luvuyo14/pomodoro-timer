package com.luvuyo.pomodoro

/** A finished work session waiting to be written to Google Calendar. */
data class PendingSession(
    val startMillis: Long,
    val endMillis: Long,
    val sessionNumber: Int,
    val sessionsInCycle: Int
) {
    /** Stored in DataStore as plain text, e.g. "1730000000000|1730001500000|3|4". */
    fun encode(): String = "$startMillis|$endMillis|$sessionNumber|$sessionsInCycle"

    /**
     * A fixed event ID makes retries safe: if a request succeeded but its reply was lost,
     * sending it again just gets a "duplicate" answer instead of a second event.
     * Google only allows the characters a-v and 0-9 in event IDs.
     */
    val eventId: String
        get() = "pomo" + startMillis.toString(32) + endMillis.toString(32)

    companion object {
        fun decode(text: String): PendingSession? {
            val parts = text.split("|")
            if (parts.size != 4) return null
            val numbers = parts.map { it.toLongOrNull() ?: return null }
            return PendingSession(
                startMillis = numbers[0],
                endMillis = numbers[1],
                sessionNumber = numbers[2].toInt(),
                sessionsInCycle = numbers[3].toInt()
            )
        }
    }
}

/** What went wrong, so the settings screen can show a translated message. */
enum class CalendarIssue { NONE, CONNECT_FAILED, NEEDS_RECONNECT, SYNC_PENDING }

data class CalendarUiState(
    val connected: Boolean = false,
    val pendingCount: Int = 0,
    val busy: Boolean = false,
    val issue: CalendarIssue = CalendarIssue.NONE
)