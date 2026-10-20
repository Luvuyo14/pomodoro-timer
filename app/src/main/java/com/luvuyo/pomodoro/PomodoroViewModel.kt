package com.luvuyo.pomodoro

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.StateFlow

/**
 * A thin wrapper around the app-wide [TimerEngine], so the screens keep working the same way
 * while the timer itself survives the activity being closed.
 */
class PomodoroViewModel(application: Application) : AndroidViewModel(application) {

    private val engine: TimerEngine = (application as PomodoroApplication).engine

    val uiState: StateFlow<TimerUiState> = engine.uiState

    /** The current saved configuration, used by the settings screen. */
    val config: StateFlow<PomodoroConfig> = engine.config

    fun start() = engine.start()
    fun pause() = engine.pause()
    fun reset() = engine.reset()
    fun skipBreak() = engine.skipBreak()
    fun resetAll() = engine.resetAll()
    fun saveSettings(newConfig: PomodoroConfig) = engine.saveSettings(newConfig)
    fun previewSound() = engine.previewSound()
}