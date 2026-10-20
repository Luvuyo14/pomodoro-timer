package com.luvuyo.pomodoro

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.luvuyo.pomodoro.ui.theme.PomodoroTimerTheme

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
    val color = sessionColor(state.sessionType)

    Column(modifier = Modifier.fillMaxSize()) {
        // Own row (not floating over the content) so it can never overlap the timer.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onOpenSettings) { Text("Settings") }
        }

        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            val screenWidth = maxWidth
            val screenHeight = maxHeight
            val isWide = screenWidth > screenHeight

            // Scrolls if the content is taller than the screen (small phones, big fonts),
            // and centres it when there is spare room.
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = screenHeight)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isWide) {
                        // Landscape / large screens: ring on the left, controls on the right.
                        val ringSize = maxOf(
                            140.dp,
                            minOf(screenHeight - 128.dp, screenWidth * 0.4f, 340.dp)
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(48.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                SessionLabel(state.sessionType, color)
                                Spacer(Modifier.height(16.dp))
                                TimerRing(state, color, ringSize)
                            }
                            CycleAndControls(
                                state = state,
                                color = color,
                                onStart = onStart,
                                onPause = onPause,
                                onReset = onReset,
                                onSkipBreak = onSkipBreak,
                                onResetAll = onResetAll,
                                modifier = Modifier.widthIn(max = 320.dp)
                            )
                        }
                    } else {
                        // Portrait: a single column. The ring grows on tablets, capped at 340dp.
                        val ringSize = maxOf(
                            160.dp,
                            minOf(screenWidth - 48.dp, screenHeight * 0.45f, 340.dp)
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            SessionLabel(state.sessionType, color)
                            Spacer(Modifier.height(24.dp))
                            TimerRing(state, color, ringSize)
                            Spacer(Modifier.height(24.dp))
                            CycleAndControls(
                                state = state,
                                color = color,
                                onStart = onStart,
                                onPause = onPause,
                                onReset = onReset,
                                onSkipBreak = onSkipBreak,
                                onResetAll = onResetAll
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun sessionColor(type: SessionType): Color = when (type) {
    SessionType.WORK -> MaterialTheme.colorScheme.primary
    SessionType.SHORT_BREAK -> MaterialTheme.colorScheme.tertiary
    SessionType.LONG_BREAK -> MaterialTheme.colorScheme.secondary
}

/** Heading that screen readers announce politely whenever the session type changes. */
@Composable
private fun SessionLabel(type: SessionType, color: Color) {
    Text(
        text = type.label,
        style = MaterialTheme.typography.headlineMedium,
        color = color,
        modifier = Modifier.semantics {
            heading()
            liveRegion = LiveRegionMode.Polite
        }
    )
}

/** Progress ring with the remaining time. Read by screen readers as one item. */
@Composable
private fun TimerRing(state: TimerUiState, color: Color, size: Dp) {
    val time = formatTime(state.remainingMillis)

    // Digits keep the same physical size whatever the system font scale, so they can't
    // outgrow the ring. They are already very large, so readability is not affected.
    val digitSize = with(LocalDensity.current) { (size * 0.2f).toSp() }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(size)
            .semantics(mergeDescendants = true) {
                contentDescription = "${state.sessionType.label}, $time remaining"
            }
    ) {
        CircularProgressIndicator(
            progress = { state.progress },
            modifier = Modifier.fillMaxSize(),
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            strokeWidth = size / 22
        )
        Text(
            text = time,
            style = TextStyle(
                fontSize = digitSize,
                fontWeight = FontWeight.Medium,
                fontFeatureSettings = "tnum" // fixed-width digits: no jitter while counting
            ),
            maxLines = 1,
            modifier = Modifier.clearAndSetSemantics { }
        )
    }
}

/** Cycle dots, session counter and all the buttons. */
@Composable
private fun CycleAndControls(
    state: TimerUiState,
    color: Color,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onReset: () -> Unit,
    onSkipBreak: () -> Unit,
    onResetAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CycleDots(
            filled = state.cycleProgress,
            total = state.sessionsBeforeLongBreak,
            color = color
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Work sessions completed: ${state.completedWorkSessions}",
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(Modifier.height(24.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val mainLabel = when (state.status) {
                TimerStatus.RUNNING -> "Pause"
                TimerStatus.PAUSED -> "Resume"
                TimerStatus.IDLE -> "Start"
            }
            Button(
                onClick = if (state.status == TimerStatus.RUNNING) onPause else onStart,
                modifier = Modifier.defaultMinSize(minWidth = 140.dp, minHeight = 56.dp)
            ) {
                Text(mainLabel, style = MaterialTheme.typography.titleMedium)
            }
            OutlinedButton(
                onClick = onReset,
                modifier = Modifier.defaultMinSize(minHeight = 56.dp),
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

// ---------- Previews: check layouts in Android Studio without running the app ----------

@Composable
private fun PreviewHost(state: TimerUiState) {
    PomodoroTimerTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            PomodoroContent(
                state = state,
                onStart = {},
                onPause = {},
                onReset = {},
                onSkipBreak = {},
                onResetAll = {},
                onOpenSettings = {}
            )
        }
    }
}

private val previewState = TimerUiState(
    status = TimerStatus.RUNNING,
    remainingMillis = 14 * 60 * 1000L + 32_000L,
    completedWorkSessions = 2
)

@Preview(name = "Phone portrait", widthDp = 360, heightDp = 780, showBackground = true)
@Composable
private fun PhonePortraitPreview() = PreviewHost(previewState)

@Preview(name = "Phone landscape", widthDp = 780, heightDp = 360, showBackground = true)
@Composable
private fun PhoneLandscapePreview() = PreviewHost(previewState)

@Preview(name = "Tablet portrait", widthDp = 800, heightDp = 1280, showBackground = true)
@Composable
private fun TabletPreview() = PreviewHost(previewState)

@Preview(
    name = "Dark, short break",
    widthDp = 360,
    heightDp = 780,
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun DarkBreakPreview() = PreviewHost(
    previewState.copy(sessionType = SessionType.SHORT_BREAK, completedWorkSessions = 2)
)