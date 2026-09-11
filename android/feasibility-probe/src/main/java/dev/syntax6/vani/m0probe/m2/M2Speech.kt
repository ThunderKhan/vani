package dev.syntax6.vani.m0probe.m2

import android.content.Context
import android.os.Build
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import dev.syntax6.vani.m0probe.m1.M1Speech
import java.util.Locale

/**
 * M2 speech facade. This feasibility build delegates inference to Android's
 * local speech facilities. It never falls back to a network recognizer.
 *
 * Capability probing is asynchronous because TextToSpeech initialization is
 * asynchronous. A capability result is therefore only emitted after the TTS
 * engine reports its initialization status.
 */
object M2Speech {
    data class Capability(
        val language: M2Language,
        val asr: M2ComponentStatus,
        val tts: M2ComponentStatus,
        val detail: String,
    )

    fun probe(context: Context, language: M2Language, callback: (Capability) -> Unit): TextToSpeech {
        val asrAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            SpeechRecognizer.isOnDeviceRecognitionAvailable(context)
        val asrStatus = if (asrAvailable) {
            // Android exposes recognizer capability, but actual locale support
            // still has to be exercised on the target device.
            M2ComponentStatus.REQUIRES_DEVICE_DATA
        } else {
            M2ComponentStatus.UNAVAILABLE
        }

        var ttsRef: TextToSpeech? = null
        ttsRef = TextToSpeech(context) { initStatus ->
            val tts = ttsRef ?: return@TextToSpeech
            val ttsStatus = if (initStatus != TextToSpeech.SUCCESS) {
                M2ComponentStatus.UNAVAILABLE
            } else {
                evaluateTtsLocale(tts, language)
            }
            val detail = when {
                !asrAvailable -> "No Android on-device recognizer is exposed on this device"
                ttsStatus == M2ComponentStatus.AVAILABLE -> "On-device ASR capability exposed; TTS locale is locally available"
                ttsStatus == M2ComponentStatus.REQUIRES_DEVICE_DATA -> "On-device ASR capability exposed; TTS requires local voice data"
                else -> "ASR capability exposed; TTS is unavailable for this locale"
            }
            callback(Capability(language, asrStatus, ttsStatus, detail))
            tts.shutdown()
        }
        return ttsRef
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
