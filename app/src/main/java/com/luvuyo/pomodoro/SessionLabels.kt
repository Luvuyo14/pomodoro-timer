package com.luvuyo.pomodoro

import androidx.annotation.StringRes

/** The translated name of a session type. Kept out of TimerState.kt so that file stays pure Kotlin. */
@StringRes
fun SessionType.labelRes(): Int = when (this) {
    SessionType.WORK -> R.string.session_work
    SessionType.SHORT_BREAK -> R.string.session_short_break
    SessionType.LONG_BREAK -> R.string.session_long_break
}