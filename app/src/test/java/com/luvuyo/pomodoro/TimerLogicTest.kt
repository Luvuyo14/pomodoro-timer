package com.luvuyo.pomodoro

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class TimerLogicTest {

    private val config = PomodoroConfig()
    private val minute = 60_000L

    private fun idleWork(completed: Int = 0, cfg: PomodoroConfig = config) =
        TimerUiState().toIdleSession(SessionType.WORK, completed, cfg)

    /** Simulates the countdown reaching zero. */
    private fun TimerUiState.finish(cfg: PomodoroConfig = config) =
        copy(status = TimerStatus.RUNNING, remainingMillis = 0L).afterSessionEnds(cfg)

    // ---- Session cycle ----

    @Test
    fun workSessionEnd_movesToShortBreak_andCountsTheSession() {
        val next = idleWork().finish()

        assertEquals(SessionType.SHORT_BREAK, next.sessionType)
        assertEquals(1, next.completedWorkSessions)
        assertEquals(TimerStatus.IDLE, next.status)
        assertEquals(5 * minute, next.totalMillis)
        assertEquals(5 * minute, next.remainingMillis)
    }

    @Test
    fun fourthWorkSessionEnd_movesToLongBreak() {
        val next = idleWork(completed = 3).finish()

        assertEquals(SessionType.LONG_BREAK, next.sessionType)
        assertEquals(4, next.completedWorkSessions)
        assertEquals(15 * minute, next.totalMillis)
    }

    @Test
    fun shortBreakEnd_returnsToWork_withoutCounting() {
        val next = idleWork().finish().finish()

        assertEquals(SessionType.WORK, next.sessionType)
        assertEquals(1, next.completedWorkSessions)
        assertEquals(25 * minute, next.totalMillis)
    }

    @Test
    fun longBreakEnd_returnsToWork_andKeepsTheCount() {
        val next = idleWork(completed = 3).finish().finish()

        assertEquals(SessionType.WORK, next.sessionType)
        assertEquals(4, next.completedWorkSessions)
    }

    @Test
    fun fullCycle_followsTheExpectedSequence() {
        val expected = listOf(
            SessionType.SHORT_BREAK, SessionType.WORK,
            SessionType.SHORT_BREAK, SessionType.WORK,
            SessionType.SHORT_BREAK, SessionType.WORK,
            SessionType.LONG_BREAK, SessionType.WORK,
            SessionType.SHORT_BREAK
        )

        var state = idleWork()
        val actual = mutableListOf<SessionType>()
        repeat(expected.size) {
            state = state.finish()
            actual += state.sessionType
        }

        assertEquals(expected, actual)
        assertEquals(5, state.completedWorkSessions)
    }

    @Test
    fun customCycleLength_changesWhenTheLongBreakHappens() {
        val twoSessions = PomodoroConfig(sessionsBeforeLongBreak = 2)

        val next = idleWork(completed = 1, cfg = twoSessions).finish(twoSessions)

        assertEquals(SessionType.LONG_BREAK, next.sessionType)
        assertEquals(2, next.completedWorkSessions)
    }

    @Test
    fun customDurations_areUsedForTheNextSession() {
        val custom = PomodoroConfig(workMinutes = 50, shortBreakMinutes = 10)

        val breakState = TimerUiState().toIdleSession(SessionType.SHORT_BREAK, 1, custom)
        val next = breakState.finish(custom)

        assertEquals(SessionType.WORK, next.sessionType)
        assertEquals(50 * minute, next.totalMillis)
        assertEquals(10 * minute, breakState.totalMillis)
    }

    // ---- Skip break / reset all ----

    @Test
    fun skippingABreak_goesToWork_withoutCounting() {
        val shortBreak = idleWork().finish()

        val next = shortBreak.afterSkippingBreak(config)

        assertEquals(SessionType.WORK, next.sessionType)
        assertEquals(1, next.completedWorkSessions)
        assertEquals(TimerStatus.IDLE, next.status)
        assertEquals(25 * minute, next.remainingMillis)
    }

    @Test
    fun skippingDuringWork_changesNothing() {
        val work = idleWork(completed = 2)

        assertSame(work, work.afterSkippingBreak(config))
    }

    @Test
    fun resetAll_returnsToFreshWork_withCounterAtZero() {
        val midCycle = idleWork(completed = 3).finish().copy(status = TimerStatus.PAUSED)

        val next = midCycle.afterResetAll(config)

        assertEquals(SessionType.WORK, next.sessionType)
        assertEquals(0, next.completedWorkSessions)
        assertEquals(TimerStatus.IDLE, next.status)
        assertEquals(25 * minute, next.remainingMillis)
    }

    // ---- Cycle dots and progress ring ----

    @Test
    fun cycleProgress_tracksTheCurrentCycle() {
        assertEquals(0, idleWork().cycleProgress)

        val shortBreak = idleWork().finish()
        assertEquals(1, shortBreak.cycleProgress)

        val longBreak = idleWork(completed = 3).finish()
        assertEquals(4, longBreak.cycleProgress)

        val workAfterLongBreak = longBreak.finish()
        assertEquals(0, workAfterLongBreak.cycleProgress)
    }

    @Test
    fun progress_isTheElapsedFractionOfTheSession() {
        val state = TimerUiState(totalMillis = 100L, remainingMillis = 25L)

        assertEquals(0.75f, state.progress, 0.0001f)
    }

    @Test
    fun progress_isZeroWhenTotalIsZero() {
        val state = TimerUiState(totalMillis = 0L, remainingMillis = 0L)

        assertEquals(0f, state.progress, 0.0001f)
    }

    // ---- Applying changed settings ----

    @Test
    fun newSettings_applyImmediately_toASessionThatHasNotStarted() {
        val newConfig = PomodoroConfig(workMinutes = 30, sessionsBeforeLongBreak = 3)

        val next = idleWork().withConfigApplied(newConfig)

        assertEquals(30 * minute, next.totalMillis)
        assertEquals(30 * minute, next.remainingMillis)
        assertEquals(3, next.sessionsBeforeLongBreak)
    }

    @Test
    fun newSettings_doNotChangeARunningSession() {
        val running = idleWork().copy(status = TimerStatus.RUNNING, remainingMillis = 10 * minute)

        val next = running.withConfigApplied(PomodoroConfig(workMinutes = 30, sessionsBeforeLongBreak = 3))

        assertEquals(25 * minute, next.totalMillis)
        assertEquals(10 * minute, next.remainingMillis)
        assertEquals(3, next.sessionsBeforeLongBreak)
    }

    @Test
    fun newSettings_doNotChangeAPausedSession() {
        val paused = idleWork().copy(status = TimerStatus.PAUSED, remainingMillis = 10 * minute)

        val next = paused.withConfigApplied(PomodoroConfig(workMinutes = 30))

        assertEquals(25 * minute, next.totalMillis)
        assertEquals(10 * minute, next.remainingMillis)
    }

    @Test
    fun newSettings_applyToABreakThatHasNotStarted() {
        val shortBreak = idleWork().finish()

        val next = shortBreak.withConfigApplied(PomodoroConfig(shortBreakMinutes = 7))

        assertEquals(7 * minute, next.totalMillis)
        assertEquals(7 * minute, next.remainingMillis)
    }
}