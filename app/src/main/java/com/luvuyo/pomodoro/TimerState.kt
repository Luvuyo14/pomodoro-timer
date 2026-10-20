package com.luvuyo.pomodoro

enum class SessionType(val label: String) {
    WORK("Work"),
    SHORT_BREAK("Short Break"),
    LONG_BREAK("Long Break")
}

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
        else completedWorkSessions % sessionsBeforeLongBreak
}