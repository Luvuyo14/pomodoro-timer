package com.luvuyo.pomodoro

import android.app.Application

/** Holds the single [TimerEngine] so the screen and the service share the same timer. */
class PomodoroApplication : Application() {
    val engine: TimerEngine by lazy { TimerEngine(this) }
}