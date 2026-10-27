package com.luvuyo.pomodoro

import android.content.Context
import android.os.SystemClock
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The timer itself: clock, settings, sound, notifications and calendar logging. It lives for as
 * long as the app process does, so it keeps counting when the screen is closed. The rules for
 * what happens when a session ends are the pure functions in TimerState.kt.
 */
class TimerEngine(context: Context) {

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val repository = SettingsRepository(appContext)
    private val soundPlayer = SoundPlayer(appContext)

    /** Logs finished work sessions to Google Calendar when the user has connected it. */
    val calendar = CalendarSync(appContext, scope)

    private val _config = MutableStateFlow(PomodoroConfig())
    /** The current saved configuration, used by the settings screen. */
    val config: StateFlow<PomodoroConfig> = _config.asStateFlow()

    private val _uiState = MutableStateFlow(TimerUiState())
    val uiState: StateFlow<TimerUiState> = _uiState.asStateFlow()

    private var tickJob: Job? = null

    /** Wall-clock time the current session began, for the calendar event. Kept across pauses. */
    private var sessionStartedAtMillis: Long? = null

    init {
        // Keep the timer in sync with whatever is saved in DataStore.
        scope.launch {
            repository.config.collect { saved ->
                _config.value = saved
                _uiState.update { it.withConfigApplied(saved) }
            }
        }
        // Send any sessions that couldn't be logged last time (for example when offline).
        calendar.flushPending()
    }

    /** Starts a fresh session or resumes a paused one. */
    fun start() {
        if (_uiState.value.status == TimerStatus.RUNNING) return

        NotificationHelper.cancelSessionFinished(appContext)

        // A fresh start (not a resume) marks when the session really began.
        if (_uiState.value.status == TimerStatus.IDLE) {
            sessionStartedAtMillis = System.currentTimeMillis()
        }

        // Use elapsedRealtime so the countdown doesn't drift with delay().
        val endAt = SystemClock.elapsedRealtime() + _uiState.value.remainingMillis
        _uiState.update { it.copy(status = TimerStatus.RUNNING) }

        // Keeps the process alive and shows the ongoing notification.
        TimerService.start(appContext)

        tickJob?.cancel()
        tickJob = scope.launch {
            while (true) {
                val left = endAt - SystemClock.elapsedRealtime()
                if (left <= 0L) {
                    completeSession()
                    break
                }
                _uiState.update { it.copy(remainingMillis = left) }
                delay(200L)
            }
        }
    }

    fun pause() {
        if (_uiState.value.status != TimerStatus.RUNNING) return
        tickJob?.cancel()
        _uiState.update { it.copy(status = TimerStatus.PAUSED) }
    }

    /** Resets the current session back to its full length. */
    fun reset() {
        tickJob?.cancel()
        sessionStartedAtMillis = null
        _uiState.update {
            it.copy(status = TimerStatus.IDLE, remainingMillis = it.totalMillis)
        }
    }

    /** Skips the current break and goes straight to the next work session. */
    fun skipBreak() {
        if (_uiState.value.sessionType == SessionType.WORK) return
        tickJob?.cancel()
        sessionStartedAtMillis = null
        _uiState.update { it.afterSkippingBreak(_config.value) }
    }

    /** Starts over: back to a fresh Work session with the counter at zero. */
    fun resetAll() {
        tickJob?.cancel()
        sessionStartedAtMillis = null
        _uiState.update { it.afterResetAll(_config.value) }
    }

    /** Persists new settings. The collector in init applies them to the timer. */
    fun saveSettings(newConfig: PomodoroConfig) {
        scope.launch { repository.save(newConfig) }
    }

    /** Plays the end-of-session sound so the user can check the volume. Ignores the sound switch. */
    fun previewSound() {
        soundPlayer.playSessionEnd()
    }

    /** Called when the countdown reaches zero: move on, log it, play the sound, notify. */
    private fun completeSession() {
        val config = _config.value
        val before = _uiState.value
        val finished = before.sessionType

        _uiState.update { it.afterSessionEnds(config) }
        val after = _uiState.value
        val endedAt = System.currentTimeMillis()

        if (finished == SessionType.WORK) {
            val cycle = config.sessionsBeforeLongBreak.coerceAtLeast(1)
            calendar.logWorkSession(
                PendingSession(
                    startMillis = sessionStartedAtMillis ?: (endedAt - before.totalMillis),
                    endMillis = endedAt,
                    sessionNumber = (after.completedWorkSessions - 1) % cycle + 1,
                    sessionsInCycle = cycle
                )
            )
        }
        sessionStartedAtMillis = null

        if (config.soundEnabled) soundPlayer.playSessionEnd()

        // Only needed when the user can't see the app; otherwise the screen already shows it.
        if (!isAppVisible()) {
            NotificationHelper.notifySessionFinished(appContext, finished, after.sessionType)
        }
    }

    private fun isAppVisible(): Boolean =
        ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
}