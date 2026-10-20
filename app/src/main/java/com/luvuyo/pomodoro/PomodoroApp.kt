package com.luvuyo.pomodoro

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel

/** Top-level composable: switches between the timer and the settings screen. */
@Composable
fun PomodoroApp(viewModel: PomodoroViewModel = viewModel()) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()
    val config by viewModel.config.collectAsState()
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var askedForNotifications by rememberSaveable { mutableStateOf(false) }

    // The timer works either way; without this permission only the notification is hidden.
    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    val onStart = {
        if (Build.VERSION.SDK_INT >= 33 &&
            !askedForNotifications &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            askedForNotifications = true
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        viewModel.start()
    }

    if (showSettings) {
        BackHandler { showSettings = false }
        SettingsScreen(
            config = config,
            onSave = { newConfig ->
                viewModel.saveSettings(newConfig)
                showSettings = false
            },
            onCancel = { showSettings = false },
            onPreviewSound = viewModel::previewSound
        )
    } else {
        PomodoroContent(
            state = state,
            onStart = onStart,
            onPause = viewModel::pause,
            onReset = viewModel::reset,
            onSkipBreak = viewModel::skipBreak,
            onResetAll = viewModel::resetAll,
            onOpenSettings = { showSettings = true }
        )
    }
}