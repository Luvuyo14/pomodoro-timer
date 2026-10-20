package com.luvuyo.pomodoro

enum class SessionType(val label: String) {
    WORK("Work"),
    SHORT_BREAK("Short Break"),
    LONG_BREAK("Long Break")
}

enum class TimerStatus { IDLE, RUNNING, PAUSED }

/** Interval configuration. Iteration 3 will load this from settings. */
data class PomodoroConfig(
    val workMillis: Long = 25 * 60 * 1000L,
    val shortBreakMillis: Long = 5 * 60 * 1000L,
    val longBreakMillis: Long = 15 * 60 * 1000L,
    val sessionsBeforeLongBreak: Int = 4
) {
    fun durationFor(type: SessionType): Long = when (type) {
        SessionType.WORK -> workMillis
        SessionType.SHORT_BREAK -> shortBreakMillis
        SessionType.LONG_BREAK -> longBreakMillis
    }
}

data class TimerUiState(
    val sessionType: SessionType = SessionType.WORK,
    val status: TimerStatus = TimerStatus.IDLE,
    val totalMillis: Long = PomodoroConfig().workMillis,
    val remainingMillis: Long = PomodoroConfig().workMillis,
    val completedWorkSessions: Int = 0,
    val sessionsBeforeLongBreak: Int = 4
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
        else completedWorkSessions % sessionsBeforeLongBreak
}