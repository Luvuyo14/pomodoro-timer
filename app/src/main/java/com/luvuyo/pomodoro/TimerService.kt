package com.luvuyo.pomodoro

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Foreground service that keeps the app process alive while a session is running or paused.
 * It owns no timer logic: it just mirrors the [TimerEngine] state into a notification and
 * stops itself when the timer goes idle.
 */
class TimerService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var wakeLock: PowerManager.WakeLock? = null
    private var isForeground = false

    private val engine: TimerEngine
        get() = (application as PomodoroApplication).engine

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createChannels(this)

        // Re-render only when the status or session type changes. The ticking countdown in the
        // notification is drawn by Android itself, so no per-second updates are needed.
        scope.launch {
            engine.uiState
                .map { it.status to it.sessionType }
                .distinctUntilChanged()
                .collect { render(engine.uiState.value) }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PAUSE -> engine.pause()
            ACTION_RESUME -> engine.start()
            ACTION_STOP -> engine.reset()
        }
        // Always call startForeground (via render) after startForegroundService().
        render(engine.uiState.value)
        return START_NOT_STICKY
    }

    private fun render(state: TimerUiState) {
        if (state.status == TimerStatus.IDLE) {
            // A service started with startForegroundService() must call startForeground()
            // before it can stop, or Android throws.
            if (!isForeground) startForegroundCompat(NotificationHelper.buildTimerNotification(this, state))
            releaseWakeLock()
            stopForeground(STOP_FOREGROUND_REMOVE)
            isForeground = false
            stopSelf()
            return
        }

        startForegroundCompat(NotificationHelper.buildTimerNotification(this, state))

        if (state.status == TimerStatus.RUNNING) {
            acquireWakeLock(state.remainingMillis + WAKE_LOCK_SLACK_MILLIS)
        } else {
            releaseWakeLock()
        }
    }

    private fun startForegroundCompat(notification: Notification) {
        try {
            if (Build.VERSION.SDK_INT >= 34) {
                startForeground(
                    NotificationHelper.NOTIFICATION_TIMER,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
            } else {
                startForeground(NotificationHelper.NOTIFICATION_TIMER, notification)
            }
            isForeground = true
        } catch (e: Exception) {
            // Android refused to start a foreground service (e.g. restricted background start).
            // The timer itself still runs in-process, so just stop the service.
            stopSelf()
        }
    }

    /** Keeps the CPU awake so the countdown and the end sound aren't delayed with the screen off. */
    private fun acquireWakeLock(timeoutMillis: Long) {
        val lock = wakeLock ?: getSystemService(PowerManager::class.java)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "pomodoro:timer")
            .apply { setReferenceCounted(false) }
            .also { wakeLock = it }
        lock.acquire(timeoutMillis)
    }

    private fun releaseWakeLock() {
        wakeLock?.let { if (it.isHeld) it.release() }
    }

    override fun onDestroy() {
        releaseWakeLock()
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        const val ACTION_PAUSE = "com.luvuyo.pomodoro.action.PAUSE"
        const val ACTION_RESUME = "com.luvuyo.pomodoro.action.RESUME"
        const val ACTION_STOP = "com.luvuyo.pomodoro.action.STOP"

        private const val WAKE_LOCK_SLACK_MILLIS = 10_000L

        fun start(context: Context) {
            try {
                ContextCompat.startForegroundService(
                    context,
                    Intent(context, TimerService::class.java)
                )
            } catch (e: Exception) {
                // The timer keeps running in-process even if the service can't start.
            }
        }
    }
}