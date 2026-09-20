package dev.syntax6.vani.m0probe.m1

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.TextToSpeech
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

/** M1 speech adapters. Network-backed recognition is deliberately rejected. */
object M1Speech {
    data class AsrResult(val transcript: String?, val elapsedMillis: Long, val endpointed: Boolean, val error: String?)
    data class TtsResult(val success: Boolean, val firstAudioMillis: Long?, val totalMillis: Long, val error: String?)

    fun startOfflineAsr(context: Context, localeTag: String, callback: (AsrResult) -> Unit): SpeechRecognizer? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || !SpeechRecognizer.isOnDeviceRecognitionAvailable(context)) {
            callback(AsrResult(null, 0, false, "No Android on-device recognizer available for M1"))
            return null
        }
        val recognizer = try {
            SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
        } catch (error: RuntimeException) {
            callback(AsrResult(null, 0, false, "Unable to create on-device recognizer: ${error.message ?: error::class.java.simpleName}"))
            return null
        }
        val started = SystemClock.elapsedRealtime()
        val finished = AtomicBoolean(false)
        var endpointed = false
        fun finish(result: AsrResult) {
            if (finished.compareAndSet(false, true)) {
                recognizer.destroy()
                callback(result)
            }
        }
        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) = Unit
            override fun onBeginningOfSpeech() = Unit
            override fun onRmsChanged(rmsdB: Float) = Unit
            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onEndOfSpeech() { endpointed = true }
            override fun onPartialResults(partialResults: Bundle?) = Unit
            override fun onEvent(eventType: Int, params: Bundle?) = Unit
            override fun onResults(results: Bundle?) {
                val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                finish(AsrResult(text, SystemClock.elapsedRealtime() - started, endpointed, if (text.isNullOrBlank()) "No transcript" else null))
            }
            override fun onError(error: Int) = finish(AsrResult(null, SystemClock.elapsedRealtime() - started, endpointed, "Recognizer error $error"))
        })
        try {
            recognizer.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.forLanguageTag(localeTag).toLanguageTag())
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, localeTag)
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            })
        } catch (error: RuntimeException) {
            finish(AsrResult(null, SystemClock.elapsedRealtime() - started, endpointed, "Unable to start on-device recognizer: ${error.message ?: error::class.java.simpleName}"))
            return null
        }
        return recognizer
    }

    fun speakOffline(context: Context, localeTag: String, text: String, callback: (TtsResult) -> Unit): TextToSpeech {
        val started = SystemClock.elapsedRealtime()
        val finished = AtomicBoolean(false)
        var ttsRef: TextToSpeech? = null
        fun finish(tts: TextToSpeech, result: TtsResult) {
            if (finished.compareAndSet(false, true)) {
                callback(result)
                tts.shutdown()
            }
        }
        ttsRef = TextToSpeech(context) { status ->
            val tts = ttsRef ?: return@TextToSpeech
            if (status != TextToSpeech.SUCCESS) {
                finish(tts, TtsResult(false, null, SystemClock.elapsedRealtime() - started, "TTS initialization failed"))
                return@TextToSpeech
            }
            val languageResult = tts.setLanguage(Locale.forLanguageTag(localeTag))
            val voice = tts.voice
            if (languageResult == TextToSpeech.LANG_MISSING_DATA || languageResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                finish(tts, TtsResult(false, null, SystemClock.elapsedRealtime() - started, "Language not supported"))
                return@TextToSpeech
            }
            if (voice?.isNetworkConnectionRequired == true) {
                finish(tts, TtsResult(false, null, SystemClock.elapsedRealtime() - started, "Selected TTS voice requires a network"))
                return@TextToSpeech
            }
            val utteranceId = "m1-${SystemClock.elapsedRealtimeNanos()}"
            var firstAudio: Long? = null
            tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(id: String?) {
                    if (id == utteranceId && firstAudio == null) firstAudio = SystemClock.elapsedRealtime() - started
                }
                override fun onDone(id: String?) {
                    if (id == utteranceId) finish(tts, TtsResult(true, firstAudio, SystemClock.elapsedRealtime() - started, null))
                }
                override fun onError(id: String?) {
                    if (id == utteranceId) finish(tts, TtsResult(false, firstAudio, SystemClock.elapsedRealtime() - started, "TTS synthesis failed"))
                }
            })
            try {
                val result = tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
                if (result != TextToSpeech.SUCCESS) {
                    finish(tts, TtsResult(false, firstAudio, SystemClock.elapsedRealtime() - started, "TTS speak() rejected the utterance"))
                }
            } catch (error: RuntimeException) {
                finish(tts, TtsResult(false, firstAudio, SystemClock.elapsedRealtime() - started, "TTS speak() failed: ${error.message ?: error::class.java.simpleName}"))
            }
        }
        return ttsRef
    }
}
