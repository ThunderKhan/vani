package dev.syntax6.vani.m0probe.m2

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import dev.syntax6.vani.m0probe.m1.M1Speech
import java.util.concurrent.atomic.AtomicInteger

/**
 * M2 research surface. It exposes every required language individually and
 * reports capability as observed by the current Android device. It does not
 * label a language as verified until the physical evidence pass is recorded.
 */
class M2Activity : Activity() {
    private lateinit var languageSpinner: Spinner
    private lateinit var status: TextView
    private lateinit var transcript: EditText
    private lateinit var speakText: EditText
    private lateinit var results: TextView
    private var recognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private val probeGeneration = AtomicInteger(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(buildUi())
    }

    override fun onDestroy() {
        probeGeneration.incrementAndGet()
        recognizer?.destroy()
        tts?.shutdown()
        super.onDestroy()
    }

    private fun buildUi(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
        }
        fun label(value: String, size: Float = 16f) = TextView(this).apply {
            text = value
            textSize = size
        }
        fun button(value: String) = Button(this).apply { text = value }

        root.addView(label("VĀṆI · M2 Ten-Language Credibility", 24f))
        root.addView(label("M2 defines and exercises all ten language paths. This screen reports capability, not completed physical evidence.", 14f))

        languageSpinner = Spinner(this)
        languageSpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            M2Language.entries.map { "${it.displayName} · ${it.nativeName} · ${it.localeTag}" },
        )
        root.addView(languageSpinner)

        status = label("Select a language and probe its local speech capabilities.").apply {
            setPadding(0, 16, 0, 8)
            setTextIsSelectable(true)
        }
        root.addView(status)
        root.addView(button("Probe selected language").apply { setOnClickListener { probeSelected() } })
        root.addView(button("Probe all ten languages").apply { setOnClickListener { probeAll() } })

        transcript = EditText(this).apply {
            hint = "ASR transcript"
            minLines = 3
            setSingleLine(false)
        }
        root.addView(transcript)
        root.addView(button("Start offline ASR").apply { setOnClickListener { startAsr() } })

        speakText = EditText(this).apply {
            hint = "Text to synthesize"
            setSingleLine(false)
            setText(M2Fixtures.forLanguage(M2Language.ENGLISH).first().text)
        }
        root.addView(speakText)
        root.addView(button("Speak selected language offline").apply { setOnClickListener { speakSelected() } })

        results = label("No M2 capability results yet.", 13f).apply {
            setPadding(0, 16, 0, 8)
            setTextIsSelectable(true)
        }
        root.addView(results)
        return root
    }

    private fun selectedLanguage(): M2Language = M2Language.entries[languageSpinner.selectedItemPosition]

    private fun probeSelected() {
        probeGeneration.incrementAndGet()
        val language = selectedLanguage()
        status.text = "Probing ${language.displayName}…"
        M2Speech.probe(this, language) { capability ->
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                status.text = "${capability.language.displayName}: ASR=${capability.asr} · TTS=${capability.tts}"
                results.text = capability.detail
            }
        }
    }

    private fun probeAll() {
        val runId = probeGeneration.incrementAndGet()
        results.text = "Probing all ten languages…\n"
        probeNext(M2Language.entries, 0, runId)
    }

    private fun probeNext(languages: List<M2Language>, index: Int, runId: Int) {
        if (runId != probeGeneration.get() || index >= languages.size || isFinishing || isDestroyed) return
        val language = languages[index]
        M2Speech.probe(this, language) { capability ->
            runOnUiThread {
                if (runId != probeGeneration.get() || isFinishing || isDestroyed) return@runOnUiThread
                results.append("${language.languageCode}: ASR=${capability.asr}, TTS=${capability.tts}\n")
                probeNext(languages, index + 1, runId)
            }
        }
    }

    private fun startAsr() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), REQUEST_AUDIO)
            status.text = "Microphone permission required; grant it and repeat the ASR test."
            return
        }
        val language = selectedLanguage()
        recognizer?.destroy()
        transcript.text.clear()
        status.text = "Listening offline as ${language.displayName}…"
        recognizer = M2Speech.startAsr(this, language) { result ->
            runOnUiThread {
                if (!result.transcript.isNullOrBlank()) {
                    transcript.setText(result.transcript)
                    transcript.setSelection(transcript.length())
                    status.text = "ASR result · ${result.elapsedMillis} ms · endpointed=${result.endpointed}"
                } else {
                    status.text = "ASR unavailable/failed · ${result.error ?: "no transcript"}"
                }
            }
        }
    }

    private fun speakSelected() {
        val text = speakText.text.toString().trim()
        if (text.isEmpty()) {
            speakText.error = "Enter text to synthesize"
            return
        }
        val language = selectedLanguage()
        tts?.shutdown()
        status.text = "Synthesizing ${language.displayName} locally…"
        tts = M2Speech.speak(this, language, M2Normalizer.scoringText(language, text)) { result ->
            runOnUiThread {
                status.text = if (result.success) {
                    "TTS completed · first audio=${result.firstAudioMillis ?: -1} ms · total=${result.totalMillis} ms"
                } else {
                    "TTS failed · ${result.error ?: "unknown error"}"
                }
            }
        }
    }

    companion object {
        private const val REQUEST_AUDIO = 1201
    }
}
