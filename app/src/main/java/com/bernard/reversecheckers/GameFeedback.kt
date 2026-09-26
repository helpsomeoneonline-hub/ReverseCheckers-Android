package com.bernard.reversecheckers

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator

class GameFeedback(context: Context) {
    private val tone = ToneGenerator(AudioManager.STREAM_MUSIC, 72)
    private val handler = Handler(Looper.getMainLooper())
    private val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator

    var soundEnabled: Boolean = true
    var vibrationEnabled: Boolean = true

    fun select() {
        play(ToneGenerator.TONE_PROP_BEEP, 35)
    }

    fun move() {
        play(ToneGenerator.TONE_PROP_ACK, 55)
        vibrate(longArrayOf(0, 14))
    }

    fun capture() {
        play(ToneGenerator.TONE_PROP_NACK, 75)
        handler.postDelayed({
            play(ToneGenerator.TONE_PROP_BEEP2, 70)
        }, 58)
        vibrate(longArrayOf(0, 28, 22, 46))
    }

    fun king() {
        play(ToneGenerator.TONE_PROP_BEEP2, 130)
        handler.postDelayed({
            play(ToneGenerator.TONE_PROP_ACK, 140)
        }, 120)
        vibrate(longArrayOf(0, 30, 35, 30, 35, 60))
    }

    fun win() {
        play(ToneGenerator.TONE_PROP_ACK, 180)
        handler.postDelayed({
            play(ToneGenerator.TONE_PROP_BEEP2, 180)
        }, 165)
        handler.postDelayed({
            play(ToneGenerator.TONE_PROP_ACK, 220)
        }, 330)
        vibrate(longArrayOf(0, 45, 45, 45, 45, 100))
    }

    fun godMove() {
        play(ToneGenerator.TONE_PROP_NACK, 45)
    }

    fun clockPress() {
        play(ToneGenerator.TONE_PROP_ACK, 45)
        handler.postDelayed({
            play(ToneGenerator.TONE_PROP_BEEP, 35)
        }, 48)
        vibrate(longArrayOf(0, 18))
    }

    fun release() {
        handler.removeCallbacksAndMessages(null)
        tone.release()
    }

    private fun play(type: Int, durationMs: Int) {
        if (!soundEnabled) return
        runCatching {
            tone.startTone(type, durationMs)
        }
    }

    @Suppress("DEPRECATION")
    private fun vibrate(pattern: LongArray) {
        if (!vibrationEnabled) return
        val v = vibrator ?: return

        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                v.vibrate(pattern, -1)
            }
        }
    }
}
