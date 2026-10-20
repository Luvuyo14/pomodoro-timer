package com.luvuyo.pomodoro

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SettingsScreen(
    config: PomodoroConfig,
    onSave: (PomodoroConfig) -> Unit,
    onCancel: () -> Unit,
    onPreviewSound: () -> Unit
) {
    // Draft values survive rotation; they reset if the saved config changes underneath.
    var work by rememberSaveable(config) { mutableStateOf(config.workMinutes) }
    var shortBreak by rememberSaveable(config) { mutableStateOf(config.shortBreakMinutes) }
    var longBreak by rememberSaveable(config) { mutableStateOf(config.longBreakMinutes) }
    var sessions by rememberSaveable(config) { mutableStateOf(config.sessionsBeforeLongBreak) }
    var sound by rememberSaveable(config) { mutableStateOf(config.soundEnabled) }

    // Centre a readable-width column on tablets and in landscape.
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 560.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.semantics { heading() }
            )

            SettingStepper(
                label = "Work session",
                valueText = "$work min",
                value = work,
                range = PomodoroConfig.WORK_RANGE,
                onChange = { work = it }
            )
            SettingStepper(
                label = "Short break",
                valueText = "$shortBreak min",
                value = shortBreak,
                range = PomodoroConfig.SHORT_BREAK_RANGE,
                onChange = { shortBreak = it }
            )
            SettingStepper(
                label = "Long break",
                valueText = "$longBreak min",
                value = longBreak,
                range = PomodoroConfig.LONG_BREAK_RANGE,
                onChange = { longBreak = it }
            )
            SettingStepper(
                label = "Work sessions before long break",
                valueText = "$sessions sessions",
                value = sessions,
                range = PomodoroConfig.SESSIONS_RANGE,
                onChange = { sessions = it }
            )

            // The whole row is one toggle target, which is easier to hit and reads well in TalkBack.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .toggleable(
                        value = sound,
                        role = Role.Switch,
                        onValueChange = { sound = it }
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Sound on session end", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = if (sound) "On" else "Off",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Switch(checked = sound, onCheckedChange = null)
            }

            OutlinedButton(onClick = onPreviewSound) { Text("Preview sound") }

            Text(
                text = "A session that hasn't started picks up changes right away. " +
                        "A running or paused session finishes at its current length. " +
                        "The sound follows your notification volume.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(onClick = {
                    onSave(
                        PomodoroConfig(
                            workMinutes = work,
                            shortBreakMinutes = shortBreak,
                            longBreakMinutes = longBreak,
                            sessionsBeforeLongBreak = sessions,
                            soundEnabled = sound
                        )
                    )
                }) { Text("Save") }

                OutlinedButton(onClick = onCancel) { Text("Cancel") }
            }

            TextButton(onClick = {
                val defaults = PomodoroConfig()
                work = defaults.workMinutes
                shortBreak = defaults.shortBreakMinutes
                longBreak = defaults.longBreakMinutes
                sessions = defaults.sessionsBeforeLongBreak
                sound = defaults.soundEnabled
            }) { Text("Restore defaults") }
        }
    }
}

/** A label, its current value, and minus/plus buttons that stay inside [range]. */
@Composable
private fun SettingStepper(
    label: String,
    valueText: String,
    value: Int,
    range: IntRange,
    onChange: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = valueText,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        FilledTonalIconButton(
            onClick = { onChange(value - 1) },
            enabled = value > range.first,
            modifier = Modifier.semantics { contentDescription = "Decrease $label" }
        ) {
            Text("−", fontSize = 24.sp, modifier = Modifier.clearAndSetSemantics { })
        }

        Spacer(Modifier.width(12.dp))

        FilledTonalIconButton(
            onClick = { onChange(value + 1) },
            enabled = value < range.last,
            modifier = Modifier.semantics { contentDescription = "Increase $label" }
        ) {
            Text("+", fontSize = 24.sp, modifier = Modifier.clearAndSetSemantics { })
        }
    }
}