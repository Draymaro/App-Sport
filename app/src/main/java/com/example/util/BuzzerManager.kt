package com.example.util

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

class BuzzerManager(private val context: Context) : TextToSpeech.OnInitListener {

    private var toneGenerator: ToneGenerator? = null
    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
        } catch (e: Exception) {
            Log.e("BuzzerManager", "Error initializing ToneGenerator: ${e.message}")
        }

        try {
            tts = TextToSpeech(context.applicationContext, this)
        } catch (e: Exception) {
            Log.e("BuzzerManager", "Error initializing TextToSpeech: ${e.message}")
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.FRENCH)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts?.setLanguage(Locale.getDefault())
            }
            tts?.setSpeechRate(1.2f)
            isTtsReady = true
        } else {
            Log.e("BuzzerManager", "TextToSpeech init failed with status: $status")
        }
    }

    /**
     * Spoken oral countdown for 5... 4... 3... 2... 1...
     */
    fun speakCountdownNumber(number: Int) {
        if (isTtsReady && tts != null) {
            tts?.speak(number.toString(), TextToSpeech.QUEUE_FLUSH, null, "countdown_$number")
        } else {
            playTickSound()
        }
    }

    /**
     * Plays a loud double-beep buzzer tone that overlays on top of active background music
     * (e.g. Spotify, Apple Music) without ducking or stopping playback.
     */
    fun playBuzzerSound() {
        try {
            if (toneGenerator == null) {
                toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
            }
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, 700)
        } catch (e: Exception) {
            Log.e("BuzzerManager", "Error playing tone: ${e.message}")
        }
        triggerVibration()
    }

    /**
     * Plays a countdown tick sound
     */
    fun playTickSound() {
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
        } catch (e: Exception) {
            Log.e("BuzzerManager", "Error playing tick: ${e.message}")
        }
    }

    private fun triggerVibration() {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val pattern = longArrayOf(0, 150, 100, 150, 100, 400)
                    val amplitudes = intArrayOf(0, 255, 0, 255, 0, 255)
                    vibrator.vibrate(VibrationEffect.createWaveform(pattern, amplitudes, -1))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(longArrayOf(0, 200, 100, 200, 100, 400), -1)
                }
            }
        } catch (e: Exception) {
            Log.e("BuzzerManager", "Vibration error: ${e.message}")
        }
    }

    fun release() {
        try {
            toneGenerator?.release()
            toneGenerator = null
        } catch (e: Exception) {
            Log.e("BuzzerManager", "Release toneGenerator error: ${e.message}")
        }
        try {
            tts?.stop()
            tts?.shutdown()
            tts = null
            isTtsReady = false
        } catch (e: Exception) {
            Log.e("BuzzerManager", "Release TTS error: ${e.message}")
        }
    }
}
