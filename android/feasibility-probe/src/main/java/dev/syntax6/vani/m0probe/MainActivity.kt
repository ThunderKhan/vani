package dev.syntax6.vani.m0probe

import android.Manifest
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.SpeechRecognizer
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : Activity() {
    private val executor: ExecutorService = Executors.newCachedThreadPool()
    private var server: ProbeServer? = null
    private var lastEvidence: String? = null
    private var asr: SpeechRecognizer? = null
    private var session = M1Session()

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
    private lateinit var m1State: TextView
    private lateinit var transcriptText: TextView

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
        asr?.destroy()
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
        speechStatus = findViewById(R.id.speechStatus)
        speechLocaleInput = findViewById(R.id.speechLocale)
        ttsLocaleInput = findViewById(R.id.ttsLocale)
        ttsTextInput = findViewById(R.id.ttsText)
        m1State = findViewById(R.id.m1State)
        transcriptText = findViewById(R.id.transcriptText)
    }

    private fun renderNetworkState() {
        val state = DeviceState.offlineSnapshot(this)
        offlineStatus.text = "Airplane mode: ${state.airplaneMode}\nValidated Internet: ${state.validatedInternetAvailable}\nInternet capability: ${state.internetCapabilityDeclared}\nActive transports: ${state.localRadios.ifEmpty { listOf("none") }.joinToString()}"
        val addresses = LocalAddressProvider.ipv4Addresses()
        localAddresses.text = if (addresses.isEmpty()) "No non-loopback IPv4 address detected." else "Candidate receiver IPv4 addresses:\n${addresses.joinToString("\n")}"
    }

    private fun toggleHost() {
        if (server?.isRunning() == true) {
            server?.stop(); server = null; hostButton.text = "Start receiver on port 42424"; hostStatus.text = "Receiver stopped"; return
        }
        val newServer = ProbeServer(PORT, DeviceState.deviceInfoString(this), { receive ->
            try {
                val bundle = SemanticBundle.decodeUtf8(receive.payload)
                val duplicate = getPreferences(MODE_PRIVATE).getBoolean("seen_${bundle.messageId}", false)
                if (!duplicate) getPreferences(MODE_PRIVATE).edit().putBoolean("seen_${bundle.messageId}", true).apply()
                runOnUiThread {
                    m1State.text = if (duplicate) "M1 state: DELIVERED · duplicate suppressed" else "M1 state: DELIVERED"
                    transcriptText.text = "Transcript: ${bundle.transcript}"
                    hostStatus.text = if (duplicate) "Duplicate ${bundle.messageId} suppressed; ACK returned." else "Bundle ${bundle.messageId} decoded; SHA-256 verified; ACK returned."
                    showEvidence(ExperimentEvidence.buildReceiverRecord(this, receive))
                }
            } catch (e: Throwable) {
                runOnUiThread { hostStatus.text = "Invalid semantic bundle: ${e.message}" }
            }
        }, { error -> runOnUiThread { hostStatus.text = "Receiver error: ${error.message ?: error::class.java.simpleName}" } })
        server = newServer
        newServer.start(executor)
        hostButton.text = "Stop receiver"
        hostStatus.text = "Listening on TCP port $PORT"
    }

    private fun sendPayload() {
        val host = hostInput.text.toString().trim()
        if (host.isBlank()) { hostInput.error = "Enter receiver IPv4"; return }
        val raw = payloadInput.text.toString().toByteArray(Charsets.UTF_8)
        if (raw.isEmpty() || raw.size > LinkProtocol.MAX_PAYLOAD_BYTES) { payloadInput.error = "Payload must be 1..${LinkProtocol.MAX_PAYLOAD_BYTES} bytes"; return }
        sendButton.isEnabled = false
        executor.execute {
            try {
                val result = ProbeClient.send(host, PORT, DeviceState.deviceInfoString(this), raw)
                runOnUiThread {
                    sendButton.isEnabled = true
                    sendStatus.text = String.format(Locale.US, "ACK=%s · integrity=%s · %d bytes · RTT %.2f ms", result.receiverAccepted, result.integrityMatched, result.payloadBytes, result.roundTripMillis)
                    m1State.text = if (result.receiverAccepted && result.integrityMatched) "M1 state: ACKNOWLEDGED" else "M1 state: FAILED"
                }
            } catch (e: Throwable) {
                runOnUiThread { sendButton.isEnabled = true; sendStatus.text = "Transfer failed: ${e.message}"; m1State.text = "M1 state: FAILED" }
            }
        }
    }

    private fun addSpeechProbeSection() {
        speechStatus.text = "ASR recognition available: ${SpeechRecognizer.isRecognitionAvailable(this)}\nOn-device ASR API: ${SpeechProbe.onDeviceRecognizerAvailable(this)}\nEXTRA_PREFER_OFFLINE is not treated as proof."
        findViewById<Button>(R.id.asrButton).setOnClickListener { runAsrProbe() }
        findViewById<Button>(R.id.ttsButton).setOnClickListener { runTtsProbe() }
    }

    private fun runAsrProbe() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), REQUEST_AUDIO); return
        }
        val locale = speechLocaleInput.text.toString().trim().ifBlank { "hi-IN" }
        m1State.text = "M1 state: LISTENING"
        speechStatus.text = "Listening for $locale … release/stop when the phrase ends."
        asr?.destroy()
        asr = SpeechProbe.runAsr(this, locale) { result ->
            runOnUiThread {
                if (result.succeeded && !result.transcript.isNullOrBlank()) {
                    transcriptText.text = "Transcript (editable above only before production send): ${result.transcript}"
                    speechStatus.text = "ASR ${result.locale}: ${result.transcript}\nOn-device API: ${result.onDeviceApiAvailable}\nElapsed: ${result.elapsedMillis} ms"
                    m1State.text = "M1 state: TRANSCRIPT_READY"
                    payloadInput.setText(result.transcript)
                } else {
                    speechStatus.text = "ASR failed: ${result.errorMessage ?: "no transcript"}"
                    m1State.text = "M1 state: FAILED"
                }
            }
        }
    }

    private fun runTtsProbe() {
        val locale = ttsLocaleInput.text.toString().trim().ifBlank { "hi-IN" }
        val text = ttsTextInput.text.toString().ifBlank { transcriptText.text.toString().removePrefix("Transcript: ") }
        speechStatus.text = "Initializing TTS for $locale …"
        SpeechProbe.runTts(this, locale, text) { result ->
            runOnUiThread { speechStatus.text = "TTS ${result.locale}\nEngine: ${result.engine ?: "—"}\nVoice: ${result.voiceName ?: "—"}\nLanguage supported: ${result.languageSupported}\nVoice requires network: ${result.networkConnectionRequired ?: "unknown"}\nSynthesis: ${result.succeeded}\nElapsed: ${result.elapsedMillis} ms" }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_AUDIO && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) Toast.makeText(this, "Microphone permission granted; run ASR again.", Toast.LENGTH_SHORT).show()
    }

    private fun showEvidence(evidence: String) { lastEvidence = evidence; evidenceText.text = evidence; copyEvidenceButton.isEnabled = true }
    private fun copyEvidence() { lastEvidence?.let { getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("VANI evidence", it)); Toast.makeText(this, "Evidence copied", Toast.LENGTH_SHORT).show() } }

    companion object { private const val PORT = 42_424; private const val REQUEST_AUDIO = 1001 }
}
