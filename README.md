<p align="center">
  <img src="doc/image/PorodoroTimer Icon.png" alt="Pomodoro Timer app icon" width="128" />
</p>

# Pomodoro Timer

A Pomodoro Technique timer for Android, built with **Kotlin** and **Jetpack Compose**. Work in focused sessions, take short breaks, and earn a long break after every fourth session. The timer keeps running when you lock your phone or leave the app.

## Features

| Requirement | Implementation |
|---|---|
| Start, pause and resume the timer | Start / Pause / Resume button, plus Reset for the current session |
| Configurable intervals | Settings screen: work (1-120 min), short break (1-60), long break (1-60), sessions before a long break (2-8). Defaults are 25 / 5 / 15 / 4, saved with DataStore |
| Show the current session type | Work, Short Break or Long Break, colour-coded (red, green, blue) |
| Track work sessions | Completed-sessions counter plus dots for the current cycle |
| Sound when a session ends | Default notification sound, with a Settings switch and a preview button |
| Accessible and good-looking on phones and tablets | Responsive portrait, landscape and tablet layouts, light and dark themes, TalkBack labels, scrolling for large font sizes |
| Keeps running in the background | Foreground service with a live countdown notification (Pause / Resume / Stop), plus a heads-up alert when a session finishes |

Extras: Skip break, Reset all, and an adaptive app icon with a themed-icon layer for Android 13+.

## How it works

- **Cycle:** Work -> Short Break -> Work -> ... -> Long Break after every Nth completed work session, then back to Work. When a session ends, the next one waits for you to press Start.
- **Settings changes:** a session that hasn't started picks up new lengths immediately. A running or paused session finishes at its current length.
- **Accurate countdown:** the timer measures against `SystemClock.elapsedRealtime()` instead of counting ticks, so it doesn't drift.
- **Background timer:** the countdown lives in an app-wide `TimerEngine`, not in the screen. While a session is running or paused, a foreground service keeps the app process alive and shows an ongoing notification. Android draws the ticking countdown itself, so the app doesn't update the notification every second. A partial wake lock stops the CPU sleeping through the end-of-session sound.
- **Finished alert:** when a session ends and the app isn't on screen, a heads-up notification says what comes next. If the app is open, you just hear the sound.

## Permissions

| Permission | Why |
|---|---|
| `FOREGROUND_SERVICE` and `FOREGROUND_SERVICE_SPECIAL_USE` | Keep the countdown running in the background |
| `POST_NOTIFICATIONS` (Android 13+) | Show the countdown and the "session finished" alert. Asked once, the first time you press Start. The timer works without it; only the notifications are hidden |
| `WAKE_LOCK` | Keep the CPU awake while a session is running, so the end sound isn't delayed with the screen off |

## Project structure

```
app/src/main/java/com/luvuyo/pomodoro/
├── MainActivity.kt            Entry point, theme and safe-area handling
├── PomodoroApplication.kt     Holds the app-wide timer engine
├── PomodoroApp.kt             Switches between screens, asks for notification permission
├── PomodoroScreen.kt          Timer UI: ring, controls, responsive layouts, previews
├── SettingsScreen.kt          Settings UI
├── PomodoroViewModel.kt       Thin wrapper around the engine for the UI
├── TimerEngine.kt             Clock, settings, sound and notifications (app-scoped)
├── TimerService.kt            Foreground service that keeps the timer alive
├── NotificationHelper.kt      Ongoing timer notification and session-finished alert
├── TimerState.kt              State models and the pure cycle rules (unit tested)
├── SettingsRepository.kt      DataStore persistence
├── SoundPlayer.kt             End-of-session sound
└── ui/theme/                  Colours, typography, light and dark themes

app/src/test/java/com/luvuyo/pomodoro/
├── TimerLogicTest.kt          Session cycle, skip break, reset all, applying settings
├── PomodoroConfigTest.kt      Defaults, duration conversion, range validation
└── FormatTimeTest.kt          Countdown text formatting and rounding
```

## Build and run

1. Install the latest stable **Android Studio**.
2. Clone the repository and open it in Android Studio.
3. Let Gradle sync (you may be asked to install Android SDK 37).
4. Run the `app` configuration on an emulator or a device (Android 8.0 / API 26 or higher).

To test the background behaviour, use a real phone if you can: lock the screen, swipe the app out of the recents list, and check that the notification keeps counting.

## Tests

The cycle rules, settings validation and time formatting are covered by JVM unit tests (no emulator needed):

```bash
./gradlew testDebugUnitTest
```

On Windows, use `gradlew.bat testDebugUnitTest`.

## Tech stack

Kotlin, Jetpack Compose (Material 3), ViewModel and StateFlow, Coroutines, Jetpack DataStore, a foreground service with notifications, JUnit 4.

## Design

The app icon was designed in Figma: a timer ring (your progress) with a tomato-leaf crown, on the same red as the Work session colour. The launcher icon is an adaptive vector drawable with a monochrome layer for themed icons.

## Known limitations

- If Android force-stops the app, or a manufacturer's battery saver kills it, the timer stops. Setting the app's battery usage to **Unrestricted** avoids this on aggressive phones (Xiaomi, Oppo, Samsung and others).
- The end-of-session sound follows the phone's notification volume and silent mode.
- The timer is not restored if the app process is killed completely.

## Roadmap

- Optional auto-start of the next session
- Session history and statistics
- Restore a running timer after the process is killed

## Version history

| Version | Highlights |
|---|---|
| 1.1.0 | Background timer with foreground service, countdown notification and finished alert |
| 1.0.0 | Timer, session cycle and counter, settings, sound, responsive layouts, dark theme, app icon, unit tests |
