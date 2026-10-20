package com.luvuyo.pomodoro

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PomodoroConfigTest {

    @Test
    fun defaults_followTheClassicPomodoroTechnique() {
        val config = PomodoroConfig()

        assertEquals(25, config.workMinutes)
        assertEquals(5, config.shortBreakMinutes)
        assertEquals(15, config.longBreakMinutes)
        assertEquals(4, config.sessionsBeforeLongBreak)
        assertTrue(config.soundEnabled)
    }

    @Test
    fun durationFor_convertsMinutesToMilliseconds() {
        val config = PomodoroConfig()

        assertEquals(25 * 60_000L, config.durationFor(SessionType.WORK))
        assertEquals(5 * 60_000L, config.durationFor(SessionType.SHORT_BREAK))
        assertEquals(15 * 60_000L, config.durationFor(SessionType.LONG_BREAK))
    }

    @Test
    fun sanitized_raisesValuesThatAreTooLow() {
        val config = PomodoroConfig(
            workMinutes = 0,
            shortBreakMinutes = -5,
            longBreakMinutes = 0,
            sessionsBeforeLongBreak = 1
        ).sanitized()

        assertEquals(1, config.workMinutes)
        assertEquals(1, config.shortBreakMinutes)
        assertEquals(1, config.longBreakMinutes)
        assertEquals(2, config.sessionsBeforeLongBreak)
    }

    @Test
    fun sanitized_lowersValuesThatAreTooHigh() {
        val config = PomodoroConfig(
            workMinutes = 500,
            shortBreakMinutes = 500,
            longBreakMinutes = 500,
            sessionsBeforeLongBreak = 99
        ).sanitized()

        assertEquals(120, config.workMinutes)
        assertEquals(60, config.shortBreakMinutes)
        assertEquals(60, config.longBreakMinutes)
        assertEquals(8, config.sessionsBeforeLongBreak)
    }

    @Test
    fun sanitized_leavesValidValuesAlone() {
        val config = PomodoroConfig(
            workMinutes = 50,
            shortBreakMinutes = 10,
            longBreakMinutes = 30,
            sessionsBeforeLongBreak = 3,
            soundEnabled = false
        )

        assertEquals(config, config.sanitized())
    }
}