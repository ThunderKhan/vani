package dev.syntax6.vani.m0probe.m1

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import dev.syntax6.vani.m0probe.MainActivity
import java.util.Locale
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/** M1 vertical slice: Android on-device speech + direct local Wi-Fi TCP. */
class M1Activity : Activity() {
    private val executor: ExecutorService = Executors.newCachedThreadPool()
    private lateinit var store: M1DeliveryStore
    private var recognizer: android.speech.SpeechRecognizer? = null
    private var server: M1Transport.Server? = null

    private lateinit var networkStatus: TextView
    private lateinit var stateText: TextView
    private lateinit var transcript: EditText
    private lateinit var localeInput: EditText
    private lateinit var hostInput: EditText
    private lateinit var sendButton: Button
    private lateinit var hostButton: Button
    private lateinit var metrics: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = M1DeliveryStore(this)
        setContentView(buildUi())
        refreshNetworkStatus()
    }

    override fun onDestroy() {
        recognizer?.destroy()
        server?.stop()
        executor.shutdownNow()
        super.onDestroy()
    }

    private fun buildUi(): View {
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(24, 24, 24, 24) }
        fun text(value: String, size: Float = 16f) = TextView(this).apply { text = value; textSize = size }
        fun edit(value: String, hint: String): EditText = EditText(this).apply { setText(value); this.hint = hint; setSingleLine(true) }
        fun button(label: String) = Button(this).apply { text = label }

        root.addView(text("VĀṆI · M1 Offline Loop", 25f))
        root.addView(text("PTT → offline STT → editable text → semantic bundle → local Wi-Fi → offline TTS → ACK", 14f))
        networkStatus = text("").apply { setPadding(0, 16, 0, 8) }
        root.addView(networkStatus)
        root.addView(button("Refresh network state").apply { setOnClickListener { refreshNetworkStatus() } })
        root.addView(button("Open M0 feasibility probe").apply {
            setOnClickListener { startActivity(Intent(this@M1Activity, MainActivity::class.java)) }
        })

        stateText = text("State: IDLE").apply { setPadding(0, 12, 0, 8) }
        root.addView(stateText)
        localeInput = edit("hi-IN", "STT/TTS locale")
        root.addView(localeInput)
        transcript = edit("", "Transcript / editable message")
        transcript.minLines = 3
        transcript.setSingleLine(false)
        root.addView(transcript)

        val ptt = button("HOLD TO SPEAK")
        ptt.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> { startPtt(); true }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> { stopPtt(); true }
                else -> true
            }
        }
        root.addView(ptt)

        hostInput = edit("", "Receiver IPv4 address")
        root.addView(hostInput)
        hostButton = button("Start receiver on TCP ${M1Transport.PORT}").apply { setOnClickListener { toggleHost() } }
        root.addView(hostButton)
        sendButton = button("Send message").apply { isEnabled = false; setOnClickListener { sendMessage() } }
        root.addView(sendButton)
        metrics = text("No M1 run yet.", 13f).apply { setPadding(0, 16, 0, 8); setTextIsSelectable(true) }
        root.addView(metrics)
        return root
    }

    private fun refreshNetworkStatus() {
        val cm = getSystemService(ConnectivityManager::class.java)
        val caps = cm.activeNetwork?.let(cm::getNetworkCapabilities)
        val internet = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true
        val wifi = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        networkStatus.text = "Internet validated: $internet · Wi-Fi transport: $wifi\nM1 judged path must run with Internet disabled; Wi-Fi presence alone is not treated as proof of offline operation."
    }

    private fun startPtt() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), REQUEST_AUDIO)
            stateText.text = "State: MICROPHONE PERMISSION REQUIRED"
            return
        }
        val locale = localeInput.text.toString().trim().ifBlank { "hi-IN" }
        transcript.text.clear()
        stateText.text = "State: LISTENING · endpointing handled by on-device recognizer"
        recognizer = M1Speech.startOfflineAsr(this, locale) { result ->
            runOnUiThread {
                if (!result.transcript.isNullOrBlank()) {
                    transcript.setText(result.transcript)
                    transcript.setSelection(transcript.length())
                    sendButton.isEnabled = true
                    stateText.text = "State: TRANSCRIPT_READY · endpoint=${result.endpointed}"
                } else {
                    stateText.text = "State: ASR_FAILED · ${result.error ?: "unknown error"}"
                }
                metrics.text = "ASR=${result.elapsedMillis} ms · endpointed=${result.endpointed}"
            }
        }
    }

    private fun stopPtt() {
        runCatching { recognizer?.stopListening() }
    }

    private fun toggleHost() {
        if (server?.isRunning() == true) {
            server?.stop()
            server = null
            hostButton.text = "Start receiver on TCP ${M1Transport.PORT}"
            return
        }
        server = M1Transport.Server { message, bytes -> receiveMessage(message, bytes) }
        server!!.start(executor)
        hostButton.text = "Stop receiver"
        Toast.makeText(this, "Receiver listening on ${M1Transport.PORT}", Toast.LENGTH_SHORT).show()
    }

    private fun receiveMessage(message: M1Message, bytes: Int): M1Transport.Server.ReceiveAck {
        if (store.wasPlayed(message.id)) {
            runOnUiThread { stateText.text = "State: DUPLICATE_SUPPRESSED · ${message.id}" }
            return M1Transport.Server.ReceiveAck(true, true, 0L)
        }
        val latch = CountDownLatch(1)
        var success = false
        var firstAudio: Long? = null
        var error: String? = null
        runOnUiThread {
            stateText.text = "State: RECEIVED · ${bytes} byte bundle"
            M1Speech.speakOffline(this, message.languageTag, message.text) { result ->
                success = result.success
                firstAudio = result.firstAudioMillis
                error = result.error
                if (result.success) store.markPlayed(message.id)
                stateText.text = if (result.success) "State: DELIVERED · PLAYED" else "State: DELIVERY_FAILED · ${result.error}"
                metrics.text = "RX bundle=${bytes} bytes · TTS first-audio=${result.firstAudioMillis ?: -1} ms · TTS total=${result.totalMillis} ms"
                latch.countDown()
            }
        }
        latch.await(12, TimeUnit.SECONDS)
        if (!success && error == null) error = "TTS timeout"
        return M1Transport.Server.ReceiveAck(success, false, firstAudio)
    }

    private fun sendMessage() {
        val host = hostInput.text.toString().trim()
        val text = transcript.text.toString().trim()
        val locale = localeInput.text.toString().trim().ifBlank { "hi-IN" }
        if (host.isBlank()) { hostInput.error = "Enter receiver IPv4 address"; return }
        if (text.isBlank()) { transcript.error = "Transcript is empty"; return }
        if (isValidatedInternetAvailable()) {
            stateText.text = "State: BLOCKED · disable Internet before the judged M1/M3 path"
            metrics.text = "No network-isolated claim recorded because validated Internet is currently available."
            return
        }
        val assessment = try { CriticalInformationGuard.assess(text, locale) } catch (error: IllegalArgumentException) {
            stateText.text = "State: SAFETY_ANALYSIS_FAILED"
            metrics.text = error.message ?: "Unable to assess message safety"
            return
        }
        if (assessment.action != SafetyAction.SEND) {
            showSafetyConfirmation(host, locale, assessment)
            return
        }
        transmitMessage(host, locale, assessment)
    }

    private fun showSafetyConfirmation(host: String, locale: String, assessment: SafetyAssessment) {
        val fieldSummary = assessment.fields.joinToString("\n") {
            "• ${it.type.name}: \"${it.value}\" (detector confidence ${"%.2f".format(Locale.US, it.confidence)})"
        }
        AlertDialog.Builder(this)
            .setTitle("Confirm critical content")
            .setMessage("This message contains operationally important content.\n\n$fieldSummary\n\nThe detector does not prove that the transcript is correct. Review the exact text before sending.")
            .setPositiveButton("Send as shown") { _, _ -> transmitMessage(host, locale, assessment) }
            .setNeutralButton("Edit first", null)
            .setNegativeButton("Cancel") { _, _ -> stateText.text = "State: SEND_CANCELLED · safety confirmation" }
            .show()
    }

    private fun transmitMessage(host: String, locale: String, assessment: SafetyAssessment) {
        val message = M1Message(languageTag = locale, text = assessment.rawText, createdElapsedNanos = System.nanoTime(), source = "android-direct", destination = host, safetyAction = assessment.action, criticalFields = assessment.fields)
        val bundle = M1Bundle.encode(message)
        store.setState(message.id, M1DeliveryState.QUEUED)
        stateText.text = "State: QUEUED · safety=${assessment.action} · fields=${assessment.criticalFieldCount}"
        sendButton.isEnabled = false
        executor.execute {
            var result: M1Transport.SendResult? = null
            var error: Throwable? = null
            repeat(2) { attempt ->
                if (result != null) return@repeat
                try { result = M1Transport.send(host, bundle, message.id) }
                catch (t: Throwable) { error = t; if (attempt == 0) Thread.sleep(250) }
            }
            val finalResult = result
            runOnUiThread {
                sendButton.isEnabled = true
                if (finalResult?.transferred == true) store.setState(message.id, M1DeliveryState.TRANSFERRED)
                if (finalResult?.delivered == true) store.setState(message.id, M1DeliveryState.DELIVERED)
                if (finalResult?.acknowledged == true && finalResult.delivered) {
                    store.setState(message.id, M1DeliveryState.ACKNOWLEDGED)
                    stateText.text = "State: ACKNOWLEDGED · ${message.id}"
                    metrics.text = String.format(Locale.US, "message=%s\nbundle=%d bytes\ncritical-fields=%d\nsafety-action=%s\ntransport=%d ms\nreceiver TTS first-audio=%s ms\nend-to-end=%d ms\nduplicate=%s\nretry policy=one retry", message.id, finalResult.bundleBytes, message.criticalFields.size, message.safetyAction, finalResult.transportMillis, finalResult.ttsFirstAudioMillis ?: "n/a", finalResult.endToEndMillis, finalResult.duplicate)
                } else {
                    store.setState(message.id, M1DeliveryState.FAILED)
                    stateText.text = "State: FAILED · peer/ACK unavailable"
                    metrics.text = "Failure: ${error?.message ?: finalResult?.error ?: "receiver rejected delivery"}"
                }
            }
        }
    }
    private fun isValidatedInternetAvailable(): Boolean {
        val cm = getSystemService(ConnectivityManager::class.java)
        val caps = cm.activeNetwork?.let(cm::getNetworkCapabilities) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_AUDIO && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Microphone permission granted; hold PTT again", Toast.LENGTH_SHORT).show()
        }
    }

    companion object { private const val REQUEST_AUDIO = 1101 }
}
