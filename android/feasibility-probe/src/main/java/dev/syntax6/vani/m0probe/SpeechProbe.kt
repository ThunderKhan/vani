package dev.syntax6.vani.m0probe

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Android-platform speech feasibility probe.
 *
 * This intentionally does NOT claim that platform speech is offline merely because
 * EXTRA_PREFER_OFFLINE is set. API 31+ on-device recognition is used when available;
 * the result records capability and runtime observations for M0 rather than declaring
 * product-level model coverage.
 */
object SpeechProbe {
    data class AsrResult(
        val locale: String,
        val onDeviceApiAvailable: Boolean,
        val recognizerAvailable: Boolean,
        val succeeded: Boolean,
        val transcript: String?,
        val elapsedMillis: Long,
        val errorCode: Int?,
        val errorMessage: String?,
    )

    data class TtsResult(
        val locale: String,
        val engine: String?,
        val voiceName: String?,
        val languageSupported: Boolean,
        val networkConnectionRequired: Boolean?,
        val succeeded: Boolean,
        val elapsedMillis: Long,
        val errorCode: Int?,
        val errorMessage: String?,
    )

    fun onDeviceRecognizerAvailable(context: Context): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            SpeechRecognizer.isOnDeviceRecognitionAvailable(context)

    fun runAsr(
        context: Context,
        localeTag: String,
        callback: (AsrResult) -> Unit,
    ): SpeechRecognizer? {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            callback(AsrResult(localeTag, Build.VERSION.SDK_INT >= Build.VERSION_CODES.S, false, false, null, 0, null, "No recognition service available"))
            return null
        }

        val locale = Locale.forLanguageTag(localeTag)
        val onDeviceAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            SpeechRecognizer.isOnDeviceRecognitionAvailable(context)
        val recognizer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && onDeviceAvailable) {
            SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
        } else {
            SpeechRecognizer.createSpeechRecognizer(context)
        }
        val finished = AtomicBoolean(false)
        val startedAt = SystemClock.elapsedRealtime()

        recognizer.setRecognitionListener(object : RecognitionListener {
            private fun finish(result: AsrResult) {
                if (finished.compareAndSet(false, true)) {
                    recognizer.destroy()
                    callback(result)
                }
            }

            override fun onReadyForSpeech(params: Bundle?) = Unit
            override fun onBeginningOfSpeech() = Unit
            override fun onRmsChanged(rmsdB: Float) = Unit
            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onEndOfSpeech() = Unit
            override fun onPartialResults(partialResults: Bundle?) = Unit
            override fun onEvent(eventType: Int, params: Bundle?) = Unit

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                finish(AsrResult(localeTag, onDeviceAvailable, true, !matches.isNullOrEmpty(), matches?.firstOrNull(), SystemClock.elapsedRealtime() - startedAt, null, null))
            }

            override fun onError(error: Int) {
                finish(AsrResult(localeTag, onDeviceAvailable, true, false, null, SystemClock.elapsedRealtime() - startedAt, error, errorName(error)))
            }
        })

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale.toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, locale.toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }
        recognizer.startListening(intent)
        return recognizer
    }

    fun runTts(
        context: Context,
        localeTag: String,
        text: String,
        callback: (TtsResult) -> Unit,
    ): TextToSpeech {
        val locale = Locale.forLanguageTag(localeTag)
        val startedAt = SystemClock.elapsedRealtime()
        var ttsRef: TextToSpeech? = null
        ttsRef = TextToSpeech(context) { initStatus ->
            val tts = ttsRef ?: return@TextToSpeech
            if (initStatus != TextToSpeech.SUCCESS) {
                callback(TtsResult(localeTag, null, null, false, null, false, SystemClock.elapsedRealtime() - startedAt, initStatus, "TextToSpeech initialization failed"))
                tts.shutdown()
                return@TextToSpeech
            }

            val languageResult = tts.setLanguage(locale)
            val languageSupported = languageResult != TextToSpeech.LANG_MISSING_DATA &&
                languageResult != TextToSpeech.LANG_NOT_SUPPORTED
            val voice = tts.voice
            val networkRequired = voice?.isNetworkConnectionRequired
            if (!languageSupported) {
                callback(TtsResult(localeTag, tts.defaultEngine, voice?.name, false, networkRequired, false, SystemClock.elapsedRealtime() - startedAt, languageResult, "Language not supported by selected TTS engine"))
                tts.shutdown()
                return@TextToSpeech
            }

            val utteranceId = "m0-${SystemClock.elapsedRealtime()}"
            tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(id: String?) = Unit

                override fun onDone(id: String?) {
                    if (id == utteranceId) {
                        callback(TtsResult(localeTag, tts.defaultEngine, tts.voice?.name, true, tts.voice?.isNetworkConnectionRequired, true, SystemClock.elapsedRealtime() - startedAt, null, null))
                        tts.shutdown()
                    }
                }

                override fun onError(id: String?) {
                    if (id == utteranceId) {
                        callback(TtsResult(localeTag, tts.defaultEngine, tts.voice?.name, true, tts.voice?.isNetworkConnectionRequired, false, SystemClock.elapsedRealtime() - startedAt, TextToSpeech.ERROR, "TTS synthesis failed"))
                        tts.shutdown()
                    }
                }
            })
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        }
        return ttsRef
    }

    private fun errorName(code: Int): String = when (code) {
        SpeechRecognizer.ERROR_AUDIO -> "ERROR_AUDIO"
        SpeechRecognizer.ERROR_CLIENT -> "ERROR_CLIENT"
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "ERROR_INSUFFICIENT_PERMISSIONS"
        SpeechRecognizer.ERROR_NETWORK -> "ERROR_NETWORK"
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "ERROR_NETWORK_TIMEOUT"
        SpeechRecognizer.ERROR_NO_MATCH -> "ERROR_NO_MATCH"
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "ERROR_RECOGNIZER_BUSY"
        SpeechRecognizer.ERROR_SERVER -> "ERROR_SERVER"
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "ERROR_SPEECH_TIMEOUT"
        else -> "ERROR_$code"
    }
}
