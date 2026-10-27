package com.luvuyo.pomodoro

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PendingSessionTest {

    private val session = PendingSession(
        startMillis = 1_730_000_000_000L,
        endMillis = 1_730_001_500_000L,
        sessionNumber = 3,
        sessionsInCycle = 4
    )

    @Test
    fun encodeThenDecode_returnsTheSameSession() {
        assertEquals(session, PendingSession.decode(session.encode()))
    }

    @Test
    fun decode_rejectsMalformedText() {
        assertNull(PendingSession.decode(""))
        assertNull(PendingSession.decode("abc"))
        assertNull(PendingSession.decode("1|2|3"))
        assertNull(PendingSession.decode("1|2|x|4"))
        assertNull(PendingSession.decode("1|2|3|4|5"))
    }

    @Test
    fun eventId_usesOnlyCharactersGoogleCalendarAllows() {
        // Google allows lowercase a-v and digits 0-9, 5 to 1024 characters long.
        assertTrue(Regex("^[a-v0-9]{5,1024}$").matches(session.eventId))
    }

    @Test
    fun eventId_isDifferentForDifferentSessions() {
        val later = session.copy(
            startMillis = session.startMillis + 1_800_000L,
            endMillis = session.endMillis + 1_800_000L
        )

        assertNotEquals(session.eventId, later.eventId)
    }
}