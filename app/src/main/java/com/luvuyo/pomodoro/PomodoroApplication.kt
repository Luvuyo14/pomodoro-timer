package com.luvuyo.pomodoro

import android.app.Application

/** Holds the single [TimerEngine] so the screen and the service share the same timer. */
class PomodoroApplication : Application() {

    val engine: TimerEngine by lazy { TimerEngine(this) }

    override fun onCreate() {
        super.onCreate()
        // Channels must exist before any notification is posted, including the
        // "session finished" alert.
        NotificationHelper.createChannels(this)
    }
}