package dev.syntax6.vani.m0probe

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
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

        findViewById<Button>(R.id.refreshOfflineButton).setOnClickListener {
            renderNetworkState()
        }
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

        val responderInfo = DeviceState.deviceInfoString(this)
        val newServer = ProbeServer(
            port = PORT,
            responderInfo = responderInfo,
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
                sendResult = ProbeClient.send(
                    host = host,
                    port = PORT,
                    senderInfo = senderInfo,
                    payload = payload,
                )
            } catch (t: Throwable) {
                failure = t
            }

            val endOffline = DeviceState.offlineSnapshot(this)
            val endDevice = DeviceState.deviceSnapshot(this)
            val evidence = ExperimentEvidence.buildSenderRecord(
                run = run,
                endOffline = endOffline,
                endDevice = endDevice,
                host = host,
                port = PORT,
                sendResult = sendResult,
                error = failure,
            )

            runOnUiThread {
                sendButton.isEnabled = true
                if (failure != null) {
                    sendStatus.text = "Transfer failed: ${failure.message ?: failure::class.java.simpleName}"
                } else if (sendResult != null) {
                    sendStatus.text = String.format(
                        Locale.US,
                        "ACK=%s · integrity=%s · %d bytes · RTT %.2f ms",
                        sendResult.receiverAccepted,
                        sendResult.integrityMatched,
                        sendResult.payloadBytes,
                        sendResult.roundTripMillis,
                    )
                }
                showEvidence(evidence)
                renderNetworkState()
            }
        }
    }

    private fun showEvidence(evidence: String) {
        lastEvidence = evidence
        evidenceText.text = evidence
        copyEvidenceButton.isEnabled = true
    }

    private fun copyEvidence() {
        val evidence = lastEvidence ?: return
        val clipboard = getSystemService(ClipboardManager::class.java)
        clipboard.setPrimaryClip(ClipData.newPlainText("VANI M0 evidence", evidence))
        Toast.makeText(this, "Evidence JSON copied", Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val PORT = 42_424
    }
}
