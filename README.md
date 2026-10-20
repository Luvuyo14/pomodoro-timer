<p align="center">
  <img src="docs/icon.png" alt="Pomodoro Timer app icon" width="128" />
</p>

# Pomodoro Timer

A Pomodoro Technique timer for Android, built with **Kotlin** and **Jetpack Compose**. Work in focused sessions, take short breaks, and earn a long break after every fourth session.

## Features

| Requirement | Implementation |
|---|---|
| Start, pause and resume the timer | Start / Pause / Resume button, plus Reset for the current session |
| Configurable intervals | Settings screen: work (1-120 min), short break (1-60), long break (1-60), sessions before a long break (2-8). Defaults are 25 / 5 / 15 / 4, saved with DataStore |
| Show the current session type | Work, Short Break or Long Break, colour-coded (red, green, blue) |
| Track work sessions | Completed-sessions counter plus dots for the current cycle |
| Sound when a session ends | Default notification sound, with a Settings switch and a preview button |
| Accessible and good-looking on phones and tablets | Responsive portrait/landscape/tablet layouts, light and dark themes, TalkBack labels, scrolling for large font sizes |

Extras: Skip break, Reset all, an adaptive app icon (with a themed-icon layer for Android 13+).

## How it works

- **Cycle:** Work -> Short Break -> Work -> ... -> Long Break after every Nth completed work session, then back to Work. A session that ends waits for you to press Start.
- **Settings changes:** a session that hasn't started picks up new lengths immediately. A running or paused session finishes at its current length.
- **Accurate countdown:** the timer measures against `SystemClock.elapsedRealtime()` instead of counting ticks, so it doesn't drift.

## Project structure

```
app/src/main/java/com/luvuyo/pomodoro/
├── MainActivity.kt            Entry point, theme and safe-area handling
├── PomodoroApp.kt             Switches between the timer and settings screens
├── PomodoroScreen.kt          Timer UI: ring, controls, responsive layouts, previews
├── SettingsScreen.kt          Settings UI
├── PomodoroViewModel.kt       Clock, storage and sound
├── TimerState.kt              State models and the pure cycle rules (unit tested)
├── SettingsRepository.kt      DataStore persistence
├── SoundPlayer.kt             End-of-session sound
└── ui/theme/                  Colours, typography, light and dark themes
```

## Build and run

1. Install the latest stable **Android Studio**.
2. Clone the repository and open it in Android Studio.
3. Let Gradle sync (you may be asked to install Android SDK 37).
4. Run the `app` configuration on an emulator or a device (Android 8.0 / API 26 or higher).

## Tests

The cycle rules, settings validation and time formatting are covered by JVM unit tests (no emulator needed):

```bash
./gradlew testDebugUnitTest
```

## Tech stack

Kotlin, Jetpack Compose (Material 3), ViewModel and StateFlow, Coroutines, Jetpack DataStore, JUnit 4.

## Known limitations

- The timer runs inside the app process. If Android closes the app or you swipe it away mid-session, the timer stops. A foreground service would fix this.
- The end-of-session sound follows the phone's notification volume and silent mode.

## Roadmap

- Foreground service with a persistent notification
- Optional auto-start of the next session
- Session history and statistics
