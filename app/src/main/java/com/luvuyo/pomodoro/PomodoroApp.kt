package com.luvuyo.pomodoro

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel

/** Top-level composable: switches between the timer and the settings screen. */
@Composable
fun PomodoroApp(viewModel: PomodoroViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()
    val config by viewModel.config.collectAsState()
    var showSettings by rememberSaveable { mutableStateOf(false) }

    if (showSettings) {
        BackHandler { showSettings = false }
        SettingsScreen(
            config = config,
            onSave = { newConfig ->
                viewModel.saveSettings(newConfig)
                showSettings = false
            },
            onCancel = { showSettings = false }
        )
    } else {
        PomodoroContent(
            state = state,
            onStart = viewModel::start,
            onPause = viewModel::pause,
            onReset = viewModel::reset,
            onSkipBreak = viewModel::skipBreak,
            onResetAll = viewModel::resetAll,
            onOpenSettings = { showSettings = true }
        )
    }
}