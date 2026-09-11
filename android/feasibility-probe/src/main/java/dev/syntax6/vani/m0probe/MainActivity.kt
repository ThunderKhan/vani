package dev.syntax6.vani.m0probe

import android.Manifest
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.SpeechRecognizer
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/** Disposable M0 transport and speech feasibility instrument. */
class MainActivity : Activity() {
    private val executor: ExecutorService = Executors.newCachedThreadPool()
    private var server: ProbeServer? = null
    private var asr: SpeechRecognizer? = null
    private var lastEvidence: String? = null
    private var activeRun: ExperimentEvidence.RunContext? = null

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        bindViews()
        renderNetworkState()
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
    }

    private fun renderNetworkState() {
        val state = DeviceState.offlineSnapshot(this)
        offlineStatus.text = "Airplane mode: ${state.airplaneMode}\nValidated Internet: ${state.validatedInternetAvailable}\nInternet capability: ${state.internetCapabilityDeclared}\nActive transports: ${state.localRadios.ifEmpty { listOf("none") }.joinToString()}"
        val addresses = LocalAddressProvider.ipv4Addresses()
        localAddresses.text = if (addresses.isEmpty()) {
            "No non-loopback IPv4 address detected."
        } else {
            "Candidate receiver IPv4 addresses:\n${addresses.joinToString("\n")}"
        }
    }

    private fun toggleHost() {
        if (server?.isRunning() == true) {
            server?.stop()
            server = null
            hostButton.text = "Start host on port 42424"
            hostStatus.text = "Host stopped"
            return
        }

        val newServer = ProbeServer(
            port = PORT,
            responderInfo = DeviceState.deviceInfoString(this),
            onReceived = { receive ->
                runOnUiThread {
                    hostStatus.text = "Received ${receive.payload.size} bytes from ${receive.remoteAddress}; SHA-256 verified."
                    evidenceText.text = ExperimentEvidence.buildReceiverRecord(this, receive)
                    lastEvidence = evidenceText.text.toString()
                    copyEvidenceButton.isEnabled = true
                }
            },
            onError = { error ->
                runOnUiThread { hostStatus.text = "Receiver error: ${error.message ?: error::class.java.simpleName}" }
            },
        )
        server = newServer
        newServer.start(executor)
        hostButton.text = "Stop host"
        hostStatus.text = "Listening on TCP port $PORT"
    }

    private fun sendPayload() {
        val host = hostInput.text.toString().trim()
        if (host.isBlank()) {
            hostInput.error = "Enter receiver IPv4"
            return
        }
        val raw = payloadInput.text.toString().toByteArray(Charsets.UTF_8)
        if (raw.isEmpty() || raw.size > LinkProtocol.MAX_PAYLOAD_BYTES) {
            payloadInput.error = "Payload must be 1..${LinkProtocol.MAX_PAYLOAD_BYTES} bytes"
            return
        }

        if (activeRun == null) activeRun = ExperimentEvidence.begin(this)
        sendButton.isEnabled = false
        sendStatus.text = "Sending ${raw.size} UTF-8 bytes..."
        executor.execute {
            var result: ProbeClient.SendResult? = null
            var error: Throwable? = null
            try {
                result = ProbeClient.send(host, PORT, DeviceState.deviceInfoString(this), raw)
            } catch (t: Throwable) {
                error = t
            }
            val endOffline = DeviceState.offlineSnapshot(this)
            val endDevice = DeviceState.deviceSnapshot(this)
            val run = activeRun
            val evidence = if (run != null) {
                ExperimentEvidence.buildSenderRecord(run, endOffline, endDevice, host, PORT, result, error)
            } else null
            runOnUiThread {
                sendButton.isEnabled = true
                sendStatus.text = if (result != null) {
                    String.format(
                        Locale.US,
                        "ACK=%s · integrity=%s · %d bytes · RTT %.2f ms",
                        result.receiverAccepted,
                        result.integrityMatched,
                        result.payloadBytes,
                        result.roundTripMillis,
                    )
                } else {
                    "Transfer failed: ${error?.message ?: "unknown error"}"
                }
                evidence?.let {
                    evidenceText.text = it
                    lastEvidence = it
                    copyEvidenceButton.isEnabled = true
                }
            }
        }
    }

    private fun copyEvidence() {
        lastEvidence?.let {
            getSystemService(ClipboardManager::class.java)
                .setPrimaryClip(ClipData.newPlainText("VANI evidence", it))
            Toast.makeText(this, "Evidence copied", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        private const val PORT = 42_424
    }
}
