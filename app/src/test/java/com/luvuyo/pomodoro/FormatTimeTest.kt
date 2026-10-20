package com.luvuyo.pomodoro

import org.junit.Assert.assertEquals
import org.junit.Test

class FormatTimeTest {

    @Test
    fun fullWorkSession_showsTwentyFiveMinutes() {
        assertEquals("25:00", formatTime(25 * 60 * 1000L))
    }

    @Test
    fun zero_showsZero() {
        assertEquals("00:00", formatTime(0L))
    }

    @Test
    fun partialSeconds_roundUp_soTheDisplayNeverShowsZeroEarly() {
        assertEquals("00:01", formatTime(1L))
        assertEquals("00:01", formatTime(1_000L))
        assertEquals("00:02", formatTime(1_001L))
    }

    @Test
    fun justUnderAMinute_roundsUpToTheMinute() {
        assertEquals("01:00", formatTime(59_001L))
        assertEquals("01:00", formatTime(60_000L))
    }

    @Test
    fun minutesAndSeconds_arePaddedToTwoDigits() {
        assertEquals("01:01", formatTime(61_000L))
        assertEquals("09:05", formatTime(9 * 60_000L + 5_000L))
    }

    @Test
    fun longestSession_showsThreeDigitMinutes() {
        assertEquals("120:00", formatTime(120 * 60_000L))
    }
}