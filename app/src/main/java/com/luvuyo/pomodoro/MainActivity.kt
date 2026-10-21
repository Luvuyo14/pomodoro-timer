package com.luvuyo.pomodoro

import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.luvuyo.pomodoro.ui.theme.PomodoroTimerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Must be called before super.onCreate().
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        // On a cold start, hold the splash a little so the whole timer animation can play.
        // Not on rotation or other recreation (savedInstanceState != null).
        val startedAt = SystemClock.uptimeMillis()
        splashScreen.setKeepOnScreenCondition {
            savedInstanceState == null &&
                    SystemClock.uptimeMillis() - startedAt < SPLASH_HOLD_MILLIS
        }

        // Fade the splash out instead of cutting to the app.
        splashScreen.setOnExitAnimationListener { splash ->
            splash.view.animate()
                .alpha(0f)
                .setDuration(SPLASH_FADE_MILLIS)
                .withEndAction { splash.remove() }
                .start()
        }

        enableEdgeToEdge()
        setContent {
            PomodoroTimerTheme {
                // The surface fills the whole window (including behind the system bars);
                // only the content is inset.
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Box(modifier = Modifier.safeDrawingPadding()) {
                        PomodoroApp()
                    }
                }
            }
        }
    }

    private companion object {
        const val SPLASH_HOLD_MILLIS = 700L
        const val SPLASH_FADE_MILLIS = 200L
    }
}