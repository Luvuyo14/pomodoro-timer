package com.luvuyo.pomodoro

enum class SessionType { WORK, SHORT_BREAK, LONG_BREAK }

enum class TimerStatus { IDLE, RUNNING, PAUSED }

/** Interval configuration, stored in minutes. Defaults follow the classic Pomodoro Technique. */
data class PomodoroConfig(
    val workMinutes: Int = 25,
    val shortBreakMinutes: Int = 5,
    val longBreakMinutes: Int = 15,
    val sessionsBeforeLongBreak: Int = 4,
    val soundEnabled: Boolean = true
) {
    fun durationFor(type: SessionType): Long = when (type) {
        SessionType.WORK -> workMinutes
        SessionType.SHORT_BREAK -> shortBreakMinutes
        SessionType.LONG_BREAK -> longBreakMinutes
    } * 60_000L

    /** Returns a copy with every value forced into its allowed range. */
    fun sanitized(): PomodoroConfig = copy(
        workMinutes = workMinutes.coerceIn(WORK_RANGE),
        shortBreakMinutes = shortBreakMinutes.coerceIn(SHORT_BREAK_RANGE),
        longBreakMinutes = longBreakMinutes.coerceIn(LONG_BREAK_RANGE),
        sessionsBeforeLongBreak = sessionsBeforeLongBreak.coerceIn(SESSIONS_RANGE)
    )

    companion object {
        val WORK_RANGE = 1..120
        val SHORT_BREAK_RANGE = 1..60
        val LONG_BREAK_RANGE = 1..60
        val SESSIONS_RANGE = 2..8
    }
}

data class TimerUiState(
    val sessionType: SessionType = SessionType.WORK,
    val status: TimerStatus = TimerStatus.IDLE,
    val totalMillis: Long = PomodoroConfig().durationFor(SessionType.WORK),
    val remainingMillis: Long = PomodoroConfig().durationFor(SessionType.WORK),
    val completedWorkSessions: Int = 0,
    val sessionsBeforeLongBreak: Int = PomodoroConfig().sessionsBeforeLongBreak
) {
    /** 0f..1f, how much of the session has elapsed. */
    val progress: Float
        get() = if (totalMillis == 0L) 0f
        else 1f - (remainingMillis.toFloat() / totalMillis.toFloat())

    /**
     * How many work sessions are done in the current cycle (0..sessionsBeforeLongBreak).
     * During the long break the cycle shows full; it empties when the next work session starts.
     */
    val cycleProgress: Int
        get() = if (sessionType == SessionType.LONG_BREAK) sessionsBeforeLongBreak
        else completedWorkSessions % sessionsBeforeLongBreak.coerceAtLeast(1)
}

// ---------------------------------------------------------------------------------------------
// Cycle rules. Pure functions (no Android, no clock) so they can be unit tested on the JVM.
// ---------------------------------------------------------------------------------------------

/** An idle session of [type] with a full countdown and the given work session count. */
fun TimerUiState.toIdleSession(
    type: SessionType,
    completedWorkSessions: Int,
    config: PomodoroConfig
): TimerUiState {
    val duration = config.durationFor(type)
    return copy(
        sessionType = type,
        status = TimerStatus.IDLE,
        totalMillis = duration,
        remainingMillis = duration,
        completedWorkSessions = completedWorkSessions,
        sessionsBeforeLongBreak = config.sessionsBeforeLongBreak
    )
}

/**
 * The state right after the countdown reaches zero: a finished work session is counted, then
 * Work -> Short Break (or Long Break after every Nth work session), and any break -> Work.
 */
fun TimerUiState.afterSessionEnds(config: PomodoroConfig): TimerUiState {
    val completed =
        if (sessionType == SessionType.WORK) completedWorkSessions + 1 else completedWorkSessions
    val cycleLength = config.sessionsBeforeLongBreak.coerceAtLeast(1)

    val next = when (sessionType) {
        SessionType.WORK ->
            if (completed % cycleLength == 0) SessionType.LONG_BREAK else SessionType.SHORT_BREAK
        SessionType.SHORT_BREAK, SessionType.LONG_BREAK -> SessionType.WORK
    }
    return toIdleSession(next, completed, config)
}

/** Skipping a break goes straight to Work without counting anything. Skipping Work does nothing. */
fun TimerUiState.afterSkippingBreak(config: PomodoroConfig): TimerUiState =
    if (sessionType == SessionType.WORK) this
    else toIdleSession(SessionType.WORK, completedWorkSessions, config)

/** Back to a fresh Work session with the counter at zero. */
fun TimerUiState.afterResetAll(config: PomodoroConfig): TimerUiState =
    toIdleSession(SessionType.WORK, 0, config)

/**
 * Applies a changed config. A session that hasn't started yet picks up the new length
 * immediately; a running or paused session keeps its length until it ends.
 */
fun TimerUiState.withConfigApplied(config: PomodoroConfig): TimerUiState {
    val untouched = status == TimerStatus.IDLE && remainingMillis == totalMillis
    val updated = copy(sessionsBeforeLongBreak = config.sessionsBeforeLongBreak)

    return if (untouched) {
        val duration = config.durationFor(sessionType)
        updated.copy(totalMillis = duration, remainingMillis = duration)
    } else {
        updated
    }
}