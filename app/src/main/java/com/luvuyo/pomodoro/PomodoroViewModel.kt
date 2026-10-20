package com.luvuyo.pomodoro

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PomodoroViewModel : ViewModel() {

    // Iteration 3 will replace this with values loaded from settings.
    private val config = PomodoroConfig()

    private val _uiState = MutableStateFlow(
        TimerUiState(
            totalMillis = config.workMillis,
            remainingMillis = config.workMillis,
            sessionsBeforeLongBreak = config.sessionsBeforeLongBreak
        )
    )
    val uiState: StateFlow<TimerUiState> = _uiState.asStateFlow()

    private var tickJob: Job? = null

    /** Starts a fresh session or resumes a paused one. */
    fun start() {
        if (_uiState.value.status == TimerStatus.RUNNING) return

        // Use elapsedRealtime so the countdown doesn't drift with delay().
        val endAt = SystemClock.elapsedRealtime() + _uiState.value.remainingMillis
        _uiState.update { it.copy(status = TimerStatus.RUNNING) }

        tickJob?.cancel()
        tickJob = viewModelScope.launch {
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
        _uiState.update {
            it.copy(status = TimerStatus.IDLE, remainingMillis = it.totalMillis)
        }
    }

    /** Skips the current break and goes straight to the next work session. */
    fun skipBreak() {
        if (_uiState.value.sessionType == SessionType.WORK) return
        tickJob?.cancel()
        _uiState.update { it.toSession(SessionType.WORK, it.completedWorkSessions) }
    }

    /** Starts over: back to a fresh Work session with the counter at zero. */
    fun resetAll() {
        tickJob?.cancel()
        _uiState.update { it.toSession(SessionType.WORK, 0) }
    }

    /** Called when the countdown reaches zero: count the session and move to the next one. */
    private fun completeSession() {
        _uiState.update { current ->
            val completed =
                if (current.sessionType == SessionType.WORK) current.completedWorkSessions + 1
                else current.completedWorkSessions

            val next = when (current.sessionType) {
                SessionType.WORK ->
                    if (completed % config.sessionsBeforeLongBreak == 0) SessionType.LONG_BREAK
                    else SessionType.SHORT_BREAK
                SessionType.SHORT_BREAK, SessionType.LONG_BREAK -> SessionType.WORK
            }
            current.toSession(next, completed)
        }
    }

    /** Builds an idle state for [next] with a full countdown. */
    private fun TimerUiState.toSession(next: SessionType, completed: Int): TimerUiState {
        val duration = config.durationFor(next)
        return copy(
            sessionType = next,
            status = TimerStatus.IDLE,
            totalMillis = duration,
            remainingMillis = duration,
            completedWorkSessions = completed
        )
    }

    override fun onCleared() {
        tickJob?.cancel()
        super.onCleared()
    }
}