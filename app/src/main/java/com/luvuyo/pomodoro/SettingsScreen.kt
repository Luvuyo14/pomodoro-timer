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
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.res.stringResource
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
    calendarState: CalendarUiState,
    onSave: (PomodoroConfig) -> Unit,
    onCancel: () -> Unit,
    onPreviewSound: () -> Unit,
    onConnectCalendar: () -> Unit,
    onDisconnectCalendar: () -> Unit
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
                text = stringResource(R.string.settings),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.semantics { heading() }
            )

            SettingStepper(
                label = stringResource(R.string.setting_work_session),
                valueText = stringResource(R.string.minutes_short, work),
                value = work,
                range = PomodoroConfig.WORK_RANGE,
                onChange = { work = it }
            )
            SettingStepper(
                label = stringResource(R.string.setting_short_break),
                valueText = stringResource(R.string.minutes_short, shortBreak),
                value = shortBreak,
                range = PomodoroConfig.SHORT_BREAK_RANGE,
                onChange = { shortBreak = it }
            )
            SettingStepper(
                label = stringResource(R.string.setting_long_break),
                valueText = stringResource(R.string.minutes_short, longBreak),
                value = longBreak,
                range = PomodoroConfig.LONG_BREAK_RANGE,
                onChange = { longBreak = it }
            )
            SettingStepper(
                label = stringResource(R.string.setting_sessions_before_long_break),
                valueText = stringResource(R.string.sessions_count, sessions),
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
                    Text(
                        text = stringResource(R.string.setting_sound),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = stringResource(if (sound) R.string.state_on else R.string.state_off),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Switch(checked = sound, onCheckedChange = null)
            }

            OutlinedButton(onClick = onPreviewSound) {
                Text(stringResource(R.string.action_preview_sound))
            }

            Text(
                text = stringResource(R.string.settings_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            HorizontalDivider()

            CalendarSection(
                state = calendarState,
                onConnect = onConnectCalendar,
                onDisconnect = onDisconnectCalendar
            )

            HorizontalDivider()

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
                }) { Text(stringResource(R.string.action_save)) }

                OutlinedButton(onClick = onCancel) {
                    Text(stringResource(R.string.action_cancel))
                }
            }

            TextButton(onClick = {
                val defaults = PomodoroConfig()
                work = defaults.workMinutes
                shortBreak = defaults.shortBreakMinutes
                longBreak = defaults.longBreakMinutes
                sessions = defaults.sessionsBeforeLongBreak
                sound = defaults.soundEnabled
            }) { Text(stringResource(R.string.action_restore_defaults)) }
        }
    }
}

/** Connect or disconnect Google Calendar. These actions apply immediately, not on Save. */
@Composable
private fun CalendarSection(
    state: CalendarUiState,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.calendar_title),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.semantics { heading() }
        )
        Text(
            text = stringResource(R.string.calendar_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = stringResource(
                if (state.connected) R.string.calendar_status_connected
                else R.string.calendar_status_not_connected
            ),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )

        if (state.pendingCount > 0) {
            Text(
                text = stringResource(R.string.calendar_pending, state.pendingCount),
                style = MaterialTheme.typography.bodyMedium
            )
        }

        val issueText = when (state.issue) {
            CalendarIssue.NONE -> null
            CalendarIssue.CONNECT_FAILED -> R.string.calendar_issue_connect_failed
            CalendarIssue.NEEDS_RECONNECT -> R.string.calendar_issue_needs_reconnect
            CalendarIssue.SYNC_PENDING -> R.string.calendar_issue_sync_pending
        }
        if (issueText != null) {
            Text(
                text = stringResource(issueText),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (!state.connected) {
                Button(onClick = onConnect, enabled = !state.busy) {
                    Text(stringResource(R.string.calendar_action_connect))
                }
            } else {
                if (state.issue == CalendarIssue.NEEDS_RECONNECT) {
                    Button(onClick = onConnect, enabled = !state.busy) {
                        Text(stringResource(R.string.calendar_action_reconnect))
                    }
                }
                OutlinedButton(onClick = onDisconnect, enabled = !state.busy) {
                    Text(stringResource(R.string.calendar_action_disconnect))
                }
            }
        }

        if (state.connected) {
            Text(
                text = stringResource(R.string.calendar_disconnect_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
    val decreaseDescription = stringResource(R.string.decrease_setting, label)
    val increaseDescription = stringResource(R.string.increase_setting, label)

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
            modifier = Modifier.semantics { contentDescription = decreaseDescription }
        ) {
            Text("−", fontSize = 24.sp, modifier = Modifier.clearAndSetSemantics { })
        }

        Spacer(Modifier.width(12.dp))

        FilledTonalIconButton(
            onClick = { onChange(value + 1) },
            enabled = value < range.last,
            modifier = Modifier.semantics { contentDescription = increaseDescription }
        ) {
            Text("+", fontSize = 24.sp, modifier = Modifier.clearAndSetSemantics { })
        }
    }
}