package dev.syntax6.vani.m0probe

import android.Manifest
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : Activity() {
    private val executor: ExecutorService = Executors.newCachedThreadPool()
    private var server: ProbeServer? = null
    private var lastEvidence: String? = null

    private lateinit var offlineStatus: TextView
    private lateinit var localAddresses: TextView
    private lateinit var hostButton: Button
    private lateinit var hostStatus: TextView
    private lateinit var hostInput: EditText
    private lateinit var payloadInput: EditText
    private lateinit var sendButton: Button
    private lateinit var sendStatus: TextView
    private lateinit var copyEvidenceButton: Button
    private lateinit var evidenceText: TextView
    private lateinit var speechStatus: TextView
    private lateinit var speechLocaleInput: EditText
    private lateinit var ttsLocaleInput: EditText
    private lateinit var ttsTextInput: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        bindViews()
        renderNetworkState()
        addSpeechProbeSection()

        findViewById<Button>(R.id.refreshOfflineButton).setOnClickListener { renderNetworkState() }
        hostButton.setOnClickListener { toggleHost() }
        sendButton.setOnClickListener { sendPayload() }
        copyEvidenceButton.setOnClickListener { copyEvidence() }
    }

    override fun onDestroy() {
        server?.stop()
        executor.shutdownNow()
        super.onDestroy()
    }

    private fun bindViews() {
        offlineStatus = findViewById(R.id.offlineStatus)
        localAddresses = findViewById(R.id.localAddresses)
        hostButton = findViewById(R.id.hostButton)
        hostStatus = findViewById(R.id.hostStatus)
        hostInput = findViewById(R.id.hostInput)
        payloadInput = findViewById(R.id.payloadInput)
        sendButton = findViewById(R.id.sendButton)
        sendStatus = findViewById(R.id.sendStatus)
        copyEvidenceButton = findViewById(R.id.copyEvidenceButton)
        evidenceText = findViewById(R.id.evidenceText)
    }

    private fun renderNetworkState() {
        val state = DeviceState.offlineSnapshot(this)
        val addresses = LocalAddressProvider.ipv4Addresses()
        offlineStatus.text = buildString {
            append("Airplane mode: ${state.airplaneMode}\n")
            append("Validated Internet available: ${state.validatedInternetAvailable}\n")
            append("Network declares Internet capability: ${state.internetCapabilityDeclared}\n")
            append("Active transports: ${state.localRadios.ifEmpty { listOf("none detected") }.joinToString()}")
        }
        localAddresses.text = if (addresses.isEmpty()) {
            "No non-loopback IPv4 address detected. Connect/re-enable local Wi-Fi, then refresh."
        } else {
            "Candidate receiver IPv4 addresses:\n${addresses.joinToString(separator = "\n")}"
        }
    }

    private fun toggleHost() {
        val current = server
        if (current?.isRunning() == true) {
            current.stop()
            server = null
            hostButton.text = "Start host on port $PORT"
            hostStatus.text = "Host stopped"
            return
        }

        val newServer = ProbeServer(
            port = PORT,
            responderInfo = DeviceState.deviceInfoString(this),
            onReceived = { receive ->
                val record = ExperimentEvidence.buildReceiverRecord(this, receive)
                runOnUiThread {
                    hostStatus.text = "Received ${receive.payload.size} bytes; SHA-256 verified; ACK returned."
                    showEvidence(record)
                    renderNetworkState()
                }
            },
            onError = { error ->
                runOnUiThread {
                    hostStatus.text = "Host error: ${error.message ?: error::class.java.simpleName}"
                    hostButton.text = "Start host on port $PORT"
                    server = null
                }
            },
        )
        server = newServer
        newServer.start(executor)
        hostButton.text = "Stop host"
        hostStatus.text = "Listening on TCP port $PORT. Keep this screen open during M0 testing."
        renderNetworkState()
    }

    private fun sendPayload() {
        val host = hostInput.text.toString().trim()
        if (host.isBlank()) {
            hostInput.error = "Enter the receiver IPv4 address"
            return
        }
        val payload = payloadInput.text.toString().toByteArray(Charsets.UTF_8)
        if (payload.isEmpty()) {
            payloadInput.error = "Enter a UTF-8 test payload"
            return
        }
        if (payload.size > LinkProtocol.MAX_PAYLOAD_BYTES) {
            payloadInput.error = "Payload is ${payload.size} bytes; maximum is ${LinkProtocol.MAX_PAYLOAD_BYTES}"
            return
        }

        val run = ExperimentEvidence.begin(this)
        val senderInfo = run.startDevice.shortLabel()
        sendButton.isEnabled = false
        sendStatus.text = "Connecting to $host:$PORT …"
        executor.execute {
            var sendResult: ProbeClient.SendResult? = null
            var failure: Throwable? = null
            try {
                sendResult = ProbeClient.send(host, PORT, senderInfo, payload)
            } catch (t: Throwable) {
                failure = t
            }
            val evidence = ExperimentEvidence.buildSenderRecord(
                run = run,
                endOffline = DeviceState.offlineSnapshot(this),
                endDevice = DeviceState.deviceSnapshot(this),
                host = host,
                port = PORT,
                sendResult = sendResult,
                error = failure,
            )
            runOnUiThread {
                sendButton.isEnabled = true
                sendStatus.text = when {
                    failure != null -> "Transfer failed: ${failure.message ?: failure::class.java.simpleName}"
                    sendResult != null -> String.format(
                        Locale.US,
                        "ACK=%s · integrity=%s · %d bytes · RTT %.2f ms",
                        sendResult!!.receiverAccepted,
                        sendResult!!.integrityMatched,
                        sendResult!!.payloadBytes,
                        sendResult!!.roundTripMillis,
                    )
                    else -> "No transfer result"
                }
                showEvidence(evidence)
                renderNetworkState()
            }
        }
    }

    private fun addSpeechProbeSection() {
        val scroll = findViewById<ViewGroup>(android.R.id.content)
        val root = scroll.getChildAt(0) as? ViewGroup ?: return
        val section = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 18, 0, 0)
        }

        fun title(text: String) = TextView(this).apply {
            this.text = text
            textSize = 18f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setPadding(0, 18, 0, 8)
        }
        fun edit(hint: String, value: String, multiLine: Boolean = false) = EditText(this).apply {
            this.hint = hint
            setText(value)
            if (multiLine) minLines = 3
            layoutParams = LinearLayout.LayoutParams(-1, -2)
        }

        speechStatus = TextView(this)
        speechLocaleInput = edit("ASR locale", "hi-IN")
        ttsLocaleInput = edit("TTS locale", "hi-IN")
        ttsTextInput = edit("TTS text", "नमस्ते, यह ऑफ़लाइन वाणी परीक्षण है।", true)

        section.addView(title("Speech feasibility probe"))
        section.addView(TextView(this).apply {
            text = "Platform-only M0 probe. On-device ASR is used when Android exposes it; TTS reports whether the selected voice requires a network. These results are evidence, not a ten-language product claim."
        })
        section.addView(speechStatus)
        section.addView(speechLocaleInput)
        section.addView(Button(this).apply {
            text = "Probe on-device ASR"
            setOnClickListener { runAsrProbe() }
        })
        section.addView(ttsLocaleInput)
        section.addView(ttsTextInput)
        section.addView(Button(this).apply {
            text = "Probe TTS voice + synthesize"
            setOnClickListener { runTtsProbe() }
        })

        root.addView(section)
        refreshSpeechCapability()
    }

    private fun refreshSpeechCapability() {
        speechStatus.text = buildString {
            append("ASR recognition service: ${android.speech.SpeechRecognizer.isRecognitionAvailable(this@MainActivity)}\n")
            append("On-device ASR API capability: ${SpeechProbe.onDeviceRecognizerAvailable(this@MainActivity)}\n")
            append("Note: EXTRA_PREFER_OFFLINE alone is not proof of offline execution.")
        }
    }

    private fun runAsrProbe() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), REQUEST_AUDIO)
            speechStatus.text = "Microphone permission requested. Press the ASR probe again after granting it."
            return
        }
        val locale = speechLocaleInput.text.toString().trim().ifBlank { "hi-IN" }
        speechStatus.text = "Listening for $locale … speak a short phrase."
        SpeechProbe.runAsr(this, locale) { result ->
            runOnUiThread {
                speechStatus.text = buildString {
                    append("ASR locale: ${result.locale}\n")
                    append("On-device API available: ${result.onDeviceApiAvailable}\n")
                    append("Recognizer available: ${result.recognizerAvailable}\n")
                    append("Succeeded: ${result.succeeded}\n")
                    append("Elapsed: ${result.elapsedMillis} ms\n")
                    append("Transcript: ${result.transcript ?: "—"}\n")
                    append("Error: ${result.errorMessage ?: "—"}")
                }
            }
        }
    }

    private fun runTtsProbe() {
        val locale = ttsLocaleInput.text.toString().trim().ifBlank { "hi-IN" }
        val text = ttsTextInput.text.toString().ifBlank { "VANI offline speech test" }
        speechStatus.text = "Initializing TTS for $locale …"
        SpeechProbe.runTts(this, locale, text) { result ->
            runOnUiThread {
                speechStatus.text = buildString {
                    append("TTS locale: ${result.locale}\n")
                    append("Engine: ${result.engine ?: "—"}\n")
                    append("Voice: ${result.voiceName ?: "—"}\n")
                    append("Language supported: ${result.languageSupported}\n")
                    append("Voice requires network: ${result.networkConnectionRequired ?: "unknown"}\n")
                    append("Synthesis succeeded: ${result.succeeded}\n")
                    append("Elapsed: ${result.elapsedMillis} ms\n")
                    append("Error: ${result.errorMessage ?: "—"}")
                }
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_AUDIO && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Microphone permission granted", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showEvidence(evidence: String) {
        lastEvidence = evidence
        evidenceText.text = evidence
        copyEvidenceButton.isEnabled = true
    }

    private fun copyEvidence() {
        val evidence = lastEvidence ?: return
        getSystemService(ClipboardManager::class.java)
            .setPrimaryClip(ClipData.newPlainText("VANI M0 evidence", evidence))
        Toast.makeText(this, "Evidence JSON copied", Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val PORT = 42_424
        private const val REQUEST_AUDIO = 1001
    }
}
