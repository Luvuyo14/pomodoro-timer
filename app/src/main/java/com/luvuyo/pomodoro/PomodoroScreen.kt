package com.luvuyo.pomodoro

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PomodoroContent(
    state: TimerUiState,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onReset: () -> Unit,
    onSkipBreak: () -> Unit,
    onResetAll: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val sessionColor = when (state.sessionType) {
        SessionType.WORK -> MaterialTheme.colorScheme.primary
        SessionType.SHORT_BREAK -> MaterialTheme.colorScheme.tertiary
        SessionType.LONG_BREAK -> MaterialTheme.colorScheme.secondary
    }

    Box(modifier = Modifier.fillMaxSize()) {
        TextButton(
            onClick = onOpenSettings,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
        ) { Text("Settings") }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = state.sessionType.label,
                style = MaterialTheme.typography.headlineMedium,
                color = sessionColor
            )

            Spacer(Modifier.height(24.dp))

            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { state.progress },
                    modifier = Modifier.size(260.dp),
                    color = sessionColor,
                    strokeWidth = 12.dp
                )
                Text(
                    text = formatTime(state.remainingMillis),
                    fontSize = 56.sp,
                    modifier = Modifier.semantics {
                        contentDescription = "Time remaining ${formatTime(state.remainingMillis)}"
                    }
                )
            }

            Spacer(Modifier.height(24.dp))

            CycleDots(
                filled = state.cycleProgress,
                total = state.sessionsBeforeLongBreak,
                color = sessionColor
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Work sessions completed: ${state.completedWorkSessions}",
                style = MaterialTheme.typography.bodyLarge
            )

            Spacer(Modifier.height(24.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                when (state.status) {
                    TimerStatus.RUNNING -> Button(onClick = onPause) { Text("Pause") }
                    TimerStatus.PAUSED -> Button(onClick = onStart) { Text("Resume") }
                    TimerStatus.IDLE -> Button(onClick = onStart) { Text("Start") }
                }
                OutlinedButton(
                    onClick = onReset,
                    enabled = state.status != TimerStatus.IDLE ||
                            state.remainingMillis != state.totalMillis
                ) { Text("Reset") }
            }

            Spacer(Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (state.sessionType != SessionType.WORK) {
                    TextButton(onClick = onSkipBreak) { Text("Skip break") }
                }
                TextButton(
                    onClick = onResetAll,
                    enabled = state.completedWorkSessions > 0 ||
                            state.sessionType != SessionType.WORK ||
                            state.status != TimerStatus.IDLE ||
                            state.remainingMillis != state.totalMillis
                ) { Text("Reset all") }
            }
        }
    }
}

/** One dot per work session in the cycle; filled dots are completed sessions. */
@Composable
fun CycleDots(filled: Int, total: Int, color: Color) {
    val emptyColor = MaterialTheme.colorScheme.surfaceVariant

    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.semantics(mergeDescendants = true) {
            contentDescription = "$filled of $total work sessions done in this cycle"
        }
    ) {
        repeat(total) { index ->
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(if (index < filled) color else emptyColor)
                    .clearAndSetSemantics { }
            )
        }
    }
}

/** Rounds up so the display shows 25:00 at start and 00:00 only when finished. */
fun formatTime(millis: Long): String {
    val totalSeconds = (millis + 999) / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}