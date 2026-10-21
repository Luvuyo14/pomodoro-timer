package com.luvuyo.pomodoro

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

/** Builds the ongoing timer notification and the "session finished" alert. */
object NotificationHelper {

    private const val CHANNEL_TIMER = "timer"
    private const val CHANNEL_SESSION_END = "session_end"

    const val NOTIFICATION_TIMER = 1
    private const val NOTIFICATION_SESSION_END = 2

    fun createChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)

        val timer = NotificationChannel(
            CHANNEL_TIMER,
            context.getString(R.string.channel_timer_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = context.getString(R.string.channel_timer_description)
            setShowBadge(false)
        }

        val sessionEnd = NotificationChannel(
            CHANNEL_SESSION_END,
            context.getString(R.string.channel_end_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.channel_end_description)
            // The app plays its own sound, so it can respect the in-app sound switch.
            setSound(null, null)
            enableVibration(true)
        }

        manager.createNotificationChannels(listOf(timer, sessionEnd))
    }

    /** The ongoing notification. While running, Android ticks the countdown by itself. */
    fun buildTimerNotification(context: Context, state: TimerUiState): Notification {
        val running = state.status == TimerStatus.RUNNING

        val builder = NotificationCompat.Builder(context, CHANNEL_TIMER)
            .setSmallIcon(R.drawable.ic_stat_timer)
            .setContentTitle(context.getString(state.sessionType.labelRes()))
            .setContentIntent(openAppIntent(context))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)

        if (running) {
            builder
                .setContentText(context.getString(R.string.time_remaining))
                .setShowWhen(true)
                .setUsesChronometer(true)
                .setChronometerCountDown(true)
                .setWhen(System.currentTimeMillis() + state.remainingMillis)
                .addAction(
                    0,
                    context.getString(R.string.action_pause),
                    serviceIntent(context, TimerService.ACTION_PAUSE)
                )
        } else {
            builder
                .setContentText(
                    context.getString(R.string.paused_at, formatTime(state.remainingMillis))
                )
                .setShowWhen(false)
                .addAction(
                    0,
                    context.getString(R.string.action_resume),
                    serviceIntent(context, TimerService.ACTION_RESUME)
                )
        }
        builder.addAction(
            0,
            context.getString(R.string.action_stop),
            serviceIntent(context, TimerService.ACTION_STOP)
        )

        return builder.build()
    }

    /** Heads-up alert for a finished session. Skipped if notifications aren't allowed. */
    @SuppressLint("MissingPermission") // the permission is checked right below
    fun notifySessionFinished(context: Context, finished: SessionType, next: SessionType) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) return

        val title = when (finished) {
            SessionType.WORK -> R.string.notification_work_done
            SessionType.SHORT_BREAK -> R.string.notification_break_done
            SessionType.LONG_BREAK -> R.string.notification_long_break_done
        }
        val text = when (next) {
            SessionType.SHORT_BREAK -> R.string.notification_next_short_break
            SessionType.LONG_BREAK -> R.string.notification_next_long_break
            SessionType.WORK -> R.string.notification_next_work
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_SESSION_END)
            .setSmallIcon(R.drawable.ic_stat_timer)
            .setContentTitle(context.getString(title))
            .setContentText(context.getString(text))
            .setContentIntent(openAppIntent(context))
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_SESSION_END, notification)
    }

    fun cancelSessionFinished(context: Context) {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_SESSION_END)
    }

    private fun openAppIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun serviceIntent(context: Context, action: String): PendingIntent {
        val intent = Intent(context, TimerService::class.java).setAction(action)
        return PendingIntent.getService(
            context,
            action.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}