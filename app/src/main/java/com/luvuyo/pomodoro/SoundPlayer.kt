package com.luvuyo.pomodoro

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.os.Handler
import android.os.Looper

/** Plays the "session ended" sound. Uses the default notification sound, with a beep as fallback. */
class SoundPlayer(context: Context) {

    private val appContext = context.applicationContext
    private val handler = Handler(Looper.getMainLooper())
    private var ringtone: Ringtone? = null

    fun playSessionEnd() {
        stop()
        try {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val tone = RingtoneManager.getRingtone(appContext, uri)
            if (tone != null) {
                tone.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                tone.play()
                ringtone = tone
                return
            }
        } catch (e: Exception) {
            // Fall through to the beep below.
        }
        playFallbackBeep()
    }

    fun stop() {
        try {
            ringtone?.stop()
        } catch (e: Exception) {
            // Nothing to do; the sound is best-effort.
        }
        ringtone = null
    }

    private fun playFallbackBeep() {
        try {
            val generator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
            generator.startTone(ToneGenerator.TONE_PROP_BEEP2, 600)
            handler.postDelayed({ generator.release() }, 800L)
        } catch (e: Exception) {
            // Some devices can't create a ToneGenerator; stay silent instead of crashing.
        }
    }
}