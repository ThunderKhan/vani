package dev.syntax6.vani.m0probe.m2

import android.content.Context
import android.os.Build
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import dev.syntax6.vani.m0probe.m1.M1Speech
import java.util.Locale

/**
 * M2 speech facade. It deliberately delegates inference to Android's local
 * speech facilities for this repository's current feasibility build. A device
 * must expose an on-device recognizer; there is no network fallback.
 *
 * This is an integration surface, not evidence that all ten paths are already
 * verified. Physical language verification remains an M2 evidence gate.
 */
object M2Speech {
    data class Capability(
        val language: M2Language,
        val asr: M2ComponentStatus,
        val tts: M2ComponentStatus,
        val detail: String,
    )

    fun probe(context: Context, language: M2Language): Capability {
        val asrAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            SpeechRecognizer.isOnDeviceRecognitionAvailable(context)

        val ttsProbe = TextToSpeech(context) { }
        val ttsResult = try {
            ttsProbe.setLanguage(Locale.forLanguageTag(language.localeTag))
        } catch (_: RuntimeException) {
            TextToSpeech.LANG_NOT_SUPPORTED
        }
        val voice = ttsProbe.voice
        val ttsStatus = when {
            ttsResult == TextToSpeech.LANG_MISSING_DATA -> M2ComponentStatus.REQUIRES_DEVICE_DATA
            ttsResult == TextToSpeech.LANG_NOT_SUPPORTED -> M2ComponentStatus.UNAVAILABLE
            voice?.isNetworkConnectionRequired == true -> M2ComponentStatus.UNAVAILABLE
            ttsResult >= 0 -> M2ComponentStatus.AVAILABLE
            else -> M2ComponentStatus.VERIFICATION_FAILED
        }
        ttsProbe.shutdown()

        val asrStatus = if (asrAvailable) {
            M2ComponentStatus.REQUIRES_DEVICE_DATA
        } else {
            M2ComponentStatus.UNAVAILABLE
        }
        val detail = when {
            !asrAvailable -> "No Android on-device recognizer is exposed on this device"
            ttsStatus == M2ComponentStatus.AVAILABLE -> "On-device ASR capability exposed; TTS locale is available"
            ttsStatus == M2ComponentStatus.REQUIRES_DEVICE_DATA -> "ASR capability exposed; TTS requires local voice data"
            else -> "ASR capability exposed; TTS is unavailable for this locale"
        }
        return Capability(language, asrStatus, ttsStatus, detail)
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
}
