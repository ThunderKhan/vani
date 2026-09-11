package dev.syntax6.vani.m0probe.m2

import android.content.Context
import android.os.Build
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import dev.syntax6.vani.m0probe.m1.M1Speech
import java.util.Locale

/**
 * M2 speech facade for the current Android feasibility build.
 *
 * The facade keeps the ten-language contract independent from the platform
 * engine. It never introduces a network-backed speech fallback. Capability
 * probing is asynchronous because TextToSpeech initialization is asynchronous.
 */
object M2Speech {
    data class Capability(
        val language: M2Language,
        val asr: M2ComponentStatus,
        val tts: M2ComponentStatus,
        val detail: String,
    )

    fun probe(context: Context, language: M2Language, callback: (Capability) -> Unit) {
        val asrAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            SpeechRecognizer.isOnDeviceRecognitionAvailable(context)
        val asrStatus = if (asrAvailable) {
            // Android exposes an on-device recognizer, but locale support still
            // requires an actual target-device inference check.
            M2ComponentStatus.REQUIRES_DEVICE_DATA
        } else {
            M2ComponentStatus.UNAVAILABLE
        }

        lateinit var tts: TextToSpeech
        tts = TextToSpeech(context) { initStatus ->
            val ttsStatus = if (initStatus == TextToSpeech.SUCCESS) {
                evaluateTtsLocale(tts, language)
            } else {
                M2ComponentStatus.UNAVAILABLE
            }
            val detail = when {
                !asrAvailable -> "No Android on-device recognizer is exposed on this device"
                ttsStatus == M2ComponentStatus.AVAILABLE -> "On-device ASR capability exposed; TTS locale is locally available"
                ttsStatus == M2ComponentStatus.REQUIRES_DEVICE_DATA -> "On-device ASR capability exposed; TTS requires local voice data"
                else -> "ASR capability exposed; TTS is unavailable for this locale"
            }
            try {
                callback(Capability(language, asrStatus, ttsStatus, detail))
            } finally {
                tts.shutdown()
            }
        }
    }

    fun startAsr(
        context: Context,
        language: M2Language,
        callback: (M1Speech.AsrResult) -> Unit,
    ): SpeechRecognizer? = M1Speech.startOfflineAsr(context, language.localeTag, callback)

    fun speak(
        context: Context,
        language: M2Language,
        text: String,
        callback: (M1Speech.TtsResult) -> Unit,
    ): TextToSpeech = M1Speech.speakOffline(context, language.localeTag, text, callback)

    private fun evaluateTtsLocale(tts: TextToSpeech, language: M2Language): M2ComponentStatus {
        val result = try {
            tts.setLanguage(Locale.forLanguageTag(language.localeTag))
        } catch (_: RuntimeException) {
            TextToSpeech.LANG_NOT_SUPPORTED
        }
        val voice = tts.voice
        return when {
            result == TextToSpeech.LANG_MISSING_DATA -> M2ComponentStatus.REQUIRES_DEVICE_DATA
            result == TextToSpeech.LANG_NOT_SUPPORTED -> M2ComponentStatus.UNAVAILABLE
            voice?.isNetworkConnectionRequired == true -> M2ComponentStatus.UNAVAILABLE
            result >= 0 -> M2ComponentStatus.AVAILABLE
            else -> M2ComponentStatus.VERIFICATION_FAILED
        }
    }
}
