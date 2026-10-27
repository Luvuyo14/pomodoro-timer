package com.luvuyo.pomodoro

import android.app.Application
import android.content.Intent
import android.content.IntentSender
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * A thin wrapper around the app-wide [TimerEngine], so the screens keep working the same way
 * while the timer itself survives the activity being closed.
 */
class PomodoroViewModel(application: Application) : AndroidViewModel(application) {

    private val engine: TimerEngine = (application as PomodoroApplication).engine

    val uiState: StateFlow<TimerUiState> = engine.uiState

    /** The current saved configuration, used by the settings screen. */
    val config: StateFlow<PomodoroConfig> = engine.config

    val calendarState: StateFlow<CalendarUiState> = engine.calendar.uiState

    fun start() = engine.start()
    fun pause() = engine.pause()
    fun reset() = engine.reset()
    fun skipBreak() = engine.skipBreak()
    fun resetAll() = engine.resetAll()
    fun saveSettings(newConfig: PomodoroConfig) = engine.saveSettings(newConfig)
    fun previewSound() = engine.previewSound()

    /**
     * Starts connecting Google Calendar. If Google needs the user to approve access,
     * [launchConsent] is called with the screen to show.
     */
    fun connectCalendar(launchConsent: (IntentSender) -> Unit) {
        viewModelScope.launch {
            try {
                val result = CalendarAuth.authorize(getApplication())
                val consent = result.pendingIntent
                if (result.hasResolution() && consent != null) {
                    launchConsent(consent.intentSender)
                } else {
                    engine.calendar.onAuthorizationResult(result)
                }
            } catch (e: ApiException) {
                engine.calendar.onConnectFailed()
            }
        }
    }

    /** Called after the Google approval screen closes with RESULT_OK. */
    fun onCalendarAuthResult(data: Intent?) {
        if (data == null) {
            engine.calendar.onConnectFailed()
            return
        }
        try {
            engine.calendar.onAuthorizationResult(
                CalendarAuth.resultFromIntent(getApplication(), data)
            )
        } catch (e: ApiException) {
            engine.calendar.onConnectFailed()
        }
    }

    fun disconnectCalendar() = engine.calendar.disconnect()
}