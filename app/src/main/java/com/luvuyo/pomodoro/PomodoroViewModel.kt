package com.luvuyo.pomodoro

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Owns the clock, storage and sound. The rules for what happens when a session ends live in
 * TimerState.kt as pure functions, so they can be unit tested.
 */
class PomodoroViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SettingsRepository(application)
    private val soundPlayer = SoundPlayer(application)

    private val _config = MutableStateFlow(PomodoroConfig())
    /** The current saved configuration, used by the settings screen. */
    val config: StateFlow<PomodoroConfig> = _config.asStateFlow()

    private val _uiState = MutableStateFlow(TimerUiState())
    val uiState: StateFlow<TimerUiState> = _uiState.asStateFlow()

    private var tickJob: Job? = null

    init {
        // Keep the timer in sync with whatever is saved in DataStore.
        viewModelScope.launch {
            repository.config.collect { saved ->
                _config.value = saved
                _uiState.update { it.withConfigApplied(saved) }
            }
        }
    }

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
        _uiState.update { it.afterSkippingBreak(_config.value) }
    }

    /** Starts over: back to a fresh Work session with the counter at zero. */
    fun resetAll() {
        tickJob?.cancel()
        _uiState.update { it.afterResetAll(_config.value) }
    }

    /** Persists new settings. The collector in init applies them to the timer. */
    fun saveSettings(newConfig: PomodoroConfig) {
        viewModelScope.launch { repository.save(newConfig) }
    }

    /** Plays the end-of-session sound so the user can check the volume. Ignores the sound switch. */
    fun previewSound() {
        soundPlayer.playSessionEnd()
    }

    /** Called when the countdown reaches zero: move to the next session and play the sound. */
    private fun completeSession() {
        val config = _config.value
        _uiState.update { it.afterSessionEnds(config) }
        if (config.soundEnabled) soundPlayer.playSessionEnd()
    }

    override fun onCleared() {
        tickJob?.cancel()
        soundPlayer.stop()
        super.onCleared()
    }
}