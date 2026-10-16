package dev.syntax6.vani.m0probe.m4

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.speech.SpeechRecognizer
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import dev.syntax6.vani.m0probe.m1.CriticalInformationGuard
import dev.syntax6.vani.m0probe.m1.M1Message
import dev.syntax6.vani.m0probe.m1.M1Speech
import dev.syntax6.vani.m0probe.m1.SafetyAction
import dev.syntax6.vani.m0probe.m2.M2Language
import dev.syntax6.vani.m4.*
import java.security.SecureRandom
import java.util.UUID
import java.util.concurrent.Executors
import javax.crypto.spec.SecretKeySpec

/**
 * M4 *research* integration screen, not the finished field UI.
 * PSKs are entered out-of-band and never saved. No key exchange or peer
 * discovery is claimed. Peer receipts are NOT human acknowledgements.
 */
class M4DirectActivity : Activity() {
    private val executor = Executors.newCachedThreadPool()
    private lateinit var store: M4DeliveryStore
    private var host: M4LocalWifiTransport.Server? = null
    private var recognizer: SpeechRecognizer? = null

    private lateinit var ownId: EditText
    private lateinit var destId: EditText
    private lateinit var remoteIp: EditText
    private lateinit var sharedKey: EditText
    private lateinit var textInput: EditText
    private lateinit var languageInput: Spinner
    private lateinit var hostButton: Button
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = M4DeliveryStore(this)
        setContentView(buildUi())
    }

    override fun onDestroy() {
        recognizer?.destroy()
        host?.stop()
        executor.shutdownNow()
        store.close()
        super.onDestroy()
    }

    private fun buildUi(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
        }
        fun label(s: String, size: Float = 15f) = TextView(this).apply { text = s; textSize = size }
        fun edit(hintText: String) = EditText(this).apply {
            hint = hintText
            setSingleLine(true)
        }
        fun button(s: String, action: () -> Unit) = Button(this).apply {
            text = s
            setOnClickListener { action() }
        }
        root.addView(label("VĀṆI · M4 Authenticated Direct Link", 23f))
        root.addView(label(
            "Engineering diagnostic. Local TCP/fragmented binary/AES-GCM. " +
            "No BLE, automatic discovery, authenticated human ACK or compliant open-source speech model yet.", 13f))
        ownId = edit("Local device ID, e.g. phone-a")
        destId = edit("Recipient device ID, e.g. phone-b")
        remoteIp = edit("Recipient numeric IPv4 address")
        sharedKey = edit("Shared 64-character hex AES key (never stored)").apply {
            inputType = android.text.InputType.TYPE_CLASS_TEXT or
                android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        root.addView(ownId)
        root.addView(destId)
        root.addView(remoteIp)
        root.addView(sharedKey)
        root.addView(button("Generate 256-bit demonstration key") { generateKey() })
        root.addView(label("Both endpoints require the same key, exchanged securely outside VĀṆI. " +
            "Generating a new key invalidates access to old queued ciphertext.", 12f))
        languageInput = Spinner(this).apply {
            adapter = ArrayAdapter(this@M4DirectActivity,
                android.R.layout.simple_spinner_dropdown_item,
                M2Language.entries.map { "${it.displayName} · ${it.localeTag}" })
        }
        root.addView(languageInput)
        textInput = edit("Exact message text; speech transcription is experimental").apply {
            setSingleLine(false)
            minLines = 3
        }
        root.addView(textInput)
        root.addView(button("Try on-device speech recognition") { startSpeech() })
        root.addView(button("Stop speech capture") { recognizer?.stopListening() })
        hostButton = button("Start authenticated receiver") { toggleHost() }
        root.addView(hostButton)
        root.addView(button("Queue and transfer protected message") { sendMessage() })
        root.addView(button("Retry durable queued bundles (same IDs)") { retryQueued() })
        status = label("Not connected. No messages received.", 13f).apply {
            setTextIsSelectable(true)
            setPadding(0, 18, 0, 8)
        }
        root.addView(status)
        return ScrollView(this).apply { fillViewport = true; addView(root) }
    }

    private fun generateKey() {
        val bytes = ByteArray(32).also(SecureRandom()::nextBytes)
        sharedKey.setText(bytes.joinToString("") { "%02x".format(it.toInt() and 0xff) })
        status.text = "New demonstration key generated locally. Share it only by a secure external channel."
    }

    private fun key(): SecretKeySpec {
        val hex = sharedKey.text.toString().trim()
        require(hex.matches(Regex("^[0-9a-fA-F]{64}$"))) { "Enter exactly 64 hexadecimal key characters" }
        val bytes = ByteArray(32) { index ->
            hex.substring(index * 2, index * 2 + 2).toInt(16).toByte()
        }
        return SecretKeySpec(bytes, "AES")
    }

    private fun selectedLanguage() = M2Language.entries[languageInput.selectedItemPosition]

    private fun startSpeech() {
        if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(android.Manifest.permission.RECORD_AUDIO), 1401)
            status.text = "Grant microphone permission and start capture again."
            return
        }
        recognizer?.destroy()
        status.text = "Trying Android platform on-device recognizer; not proof of open-source compliance"
        recognizer = M1Speech.startOfflineAsr(this, selectedLanguage().localeTag) { result ->
            runOnUiThread {
                if (result.transcript.isNullOrBlank()) status.text = "ASR failed: ${result.error}"
                else {
                    textInput.setText(result.transcript)
                    status.text = "Transcript ready; inspect and correct exact values before sending."
                }
            }
        }
    }

    private fun toggleHost() {
        if (host?.isRunning() == true) {
            host?.stop()
            host = null
            hostButton.text = "Start authenticated receiver"
            status.text = "Receiver stopped."
            return
        }
        val localId = ownId.text.toString().trim()
        val secret = try { key() } catch (error: IllegalArgumentException) {
            status.text = "Cannot start: ${error.message}"
            return
        }
        if (localId.isBlank() || localId.length > 128) {
            ownId.error = "Enter a local ID (1–128 characters)"
            return
        }
        val engine = M4DeliveryEngine(store, AesGcmMessageProtector(secret), localId)
        host = M4LocalWifiTransport.Server { protectedBytes ->
            try {
                val bundle = engine.acceptBundle(protectedBytes)
                runOnUiThread {
                    if (isFinishing || isDestroyed) return@runOnUiThread
                    status.text = "Authenticated destination message ${bundle.messageId}. " +
                        "TTS is starting. Device receipt is not human acknowledgement."
                    M1Speech.speakOffline(this, bundle.languageTag, bundle.transcript) { tts ->
                        if (tts.success) {
                            runCatching { engine.markPlaybackStarted(bundle.messageId) }
                        }
                        runOnUiThread {
                            if (!isFinishing && !isDestroyed) {
                                status.text = "RX ${bundle.messageId}: " +
                                    (if (tts.success) "device TTS completed" else "TTS FAILED: ${tts.error}")
                            }
                        }
                    }
                }
                true // End-to-end authenticated and durably received; TTS may still fail later.
            } catch (error: Exception) {
                runOnUiThread {
                    if (!isFinishing && !isDestroyed) status.text =
                        "Rejected invalid/duplicate/foreign M4 bundle: ${error.message}"
                }
                false
            }
        }.also { it.start(executor) }
        hostButton.text = "Stop authenticated receiver"
        status.text = "Listening on local TCP ${M4LocalWifiTransport.PORT}. Key exists only in memory."
    }

    /**
     * Manual store/carry/forward retry: the encrypted durable bundle and message ID
     * are reused after a contact becomes available. This is not automatic discovery.
     */
    private fun retryQueued() {
        val peer = remoteIp.text.toString().trim()
        if (peer.isBlank()) {
            remoteIp.error = "Enter the currently reachable peer IPv4 address"
            return
        }
        status.text = "Checking durable outbox. Receiver contact must already exist."
        executor.execute {
            try {
                val pending = store.pending(Direction.OUTBOX)
                    .filter { it.state == DeliveryState.QUEUED }
                var accepted = 0
                var rejected = 0
                var exhausted = 0
                for (record in pending) {
                    if (!store.incrementRetry(record.messageId)) {
                        store.transition(record.messageId, DeliveryState.FAILED)
                        exhausted++
                        continue
                    }
                    try {
                        val frames = M4FrameCodec.fragment(record.messageId, record.bundle)
                        val receipt = M4LocalWifiTransport().send(PeerId(peer), frames)
                        if (receipt.accepted) {
                            store.transition(record.messageId, DeliveryState.TRANSFERRED)
                            accepted++
                        } else rejected++
                    } catch (_: Exception) {
                        rejected++
                    }
                }
                runOnUiThread {
                    if (!isFinishing && !isDestroyed) {
                        status.text = "Retried ${pending.size} durable queued IDs: " +
                            "peer accepted=$accepted, no peer receipt=$rejected, " +
                            "retry limit exhausted=$exhausted. No human ACK implied."
                    }
                }
            } catch (error: Exception) {
                runOnUiThread {
                    if (!isFinishing && !isDestroyed) status.text = "Retry failed: ${error.message}"
                }
            }
        }
    }

    private fun sendMessage() {
        val own = ownId.text.toString().trim()
        val dest = destId.text.toString().trim()
        val peer = remoteIp.text.toString().trim()
        val exactText = textInput.text.toString().trim()
        if (own.isBlank() || dest.isBlank() || peer.isBlank() || exactText.isBlank()) {
            status.text = "Enter local ID, destination ID, peer IPv4, and message."
            return
        }
        val secret = try { key() } catch (error: IllegalArgumentException) {
            status.text = "Cannot send: ${error.message}"
            return
        }
        val language = selectedLanguage()
        val assessment = try { CriticalInformationGuard.assess(exactText, language.localeTag) }
            catch (error: IllegalArgumentException) {
                status.text = "Safety guard failed: ${error.message}"
                return
            }
        val action = {
            val now = System.currentTimeMillis()
            val m1 = M1Message(
                languageTag = language.localeTag,
                text = assessment.rawText,
                createdElapsedNanos = System.nanoTime(),
                source = own,
                destination = dest,
                safetyAction = assessment.action,
                expiresAfterMillis = 3_600_000L, // One-hour bounded store-carry-forward window.
                criticalFields = assessment.fields,
            )
            val bundle = M4ProtocolAdapter.fromM1(m1, now).copy(
                hopLimit = 1, copyBudget = 1, ackPolicy = M4AckPolicy.DEVICE_RECEIVED,
            )
            status.text = "Encoding and queuing authenticated bundle; no remote delivery claim yet."
            executor.execute {
                try {
                    val engine = M4DeliveryEngine(store, AesGcmMessageProtector(secret), own)
                    val encrypted = engine.enqueue(bundle)
                    val frames = M4FrameCodec.fragment(bundle.messageId, encrypted)
                    val receipt = M4LocalWifiTransport().send(PeerId(peer), frames)
                    if (receipt.accepted) store.transition(bundle.messageId, DeliveryState.TRANSFERRED)
                    runOnUiThread {
                        status.text = if (receipt.accepted) {
                            "Peer accepted ${frames.size} frames / ${encrypted.size} encrypted bytes. " +
                                "NOT destination/human acknowledged. Message: ${bundle.messageId}"
                        } else {
                            "Peer rejected message. Bundle remains queued for manual retry."
                        }
                    }
                } catch (error: Exception) {
                    runOnUiThread {
                        status.text = "Bundle remains queued or failed to encode: ${error.message}"
                    }
                }
            }
        }
        if (assessment.action == SafetyAction.SEND) action()
        else AlertDialog.Builder(this)
            .setTitle("Confirm safety-critical transcript")
            .setMessage("Detected ${assessment.fields.size} critical fields. Verify numbers, names and negation. " +
                "The detector confidence is NOT calibrated speech-recognition confidence.\n\n${assessment.rawText}")
            .setPositiveButton("Send exact text") { _, _ -> action() }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
