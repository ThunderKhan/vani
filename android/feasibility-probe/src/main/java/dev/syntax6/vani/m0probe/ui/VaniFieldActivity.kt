package dev.syntax6.vani.m0probe.ui

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.speech.SpeechRecognizer
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import dev.syntax6.vani.m0probe.MainActivity
import dev.syntax6.vani.m0probe.m1.CriticalInformationGuard
import dev.syntax6.vani.m0probe.m1.M1Message
import dev.syntax6.vani.m0probe.m1.M1Speech
import dev.syntax6.vani.m0probe.m1.SafetyAction
import dev.syntax6.vani.m0probe.m2.M2Activity
import dev.syntax6.vani.m0probe.m2.M2Language
import dev.syntax6.vani.m0probe.m4.M4DirectActivity
import dev.syntax6.vani.m4.*
import java.security.SecureRandom
import java.text.DateFormat
import java.util.Date
import java.util.UUID
import java.util.concurrent.Executors
import javax.crypto.spec.SecretKeySpec

/**
 * Original VĀṆI field interface, inspired by Jetchat's readable composition
 * patterns and by off-grid device-oriented UX. No external application UI code
 * or assets are copied. Uses Android Views to preserve the existing toolchain.
 *
 * No simulated messages, peers, connectivity status or metrics are displayed.
 */
class VaniFieldActivity : Activity() {
    private val executor = Executors.newCachedThreadPool()
    private val prefs by lazy { getSharedPreferences("vani_field_ui", MODE_PRIVATE) }
    private lateinit var store: M4DeliveryStore
    private var wifiListener: M4LocalWifiTransport.Server? = null
    private var bluetoothListener: M4BluetoothTransport.Server? = null
    private var recognizer: SpeechRecognizer? = null
    private var sessionKey: ByteArray? = null

    private var selfId = ""
    private var recipientId = ""
    private var wifiAddress = ""
    private var bluetoothAddress = ""
    private var draft = ""
    private var language = M2Language.HINDI
    private var urgency = M4Priority.ROUTINE
    private var useBluetooth = false
    private var autoSpeak = false
    private var urgentAlerts = false
    private var page = Page.MESSAGES
    private var notice = "Prepare a shared key and select a local transport to begin."

    private lateinit var stage: FrameLayout
    private lateinit var navigation: LinearLayout
    private lateinit var noticeLabel: TextView
    private lateinit var linkLabel: TextView
    private var composer: EditText? = null

    private enum class Page { MESSAGES, COMPOSE, CONNECT, TOOLS }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = M4DeliveryStore(this)
        selfId = prefs.getString("self", null).orEmpty().ifBlank {
            "node-" + UUID.randomUUID().toString().take(8)
        }
        recipientId = prefs.getString("recipient", "").orEmpty()
        wifiAddress = prefs.getString("wifi", "").orEmpty()
        bluetoothAddress = prefs.getString("bluetooth", "").orEmpty()
        useBluetooth = prefs.getBoolean("bluetooth_mode", false)
        autoSpeak = prefs.getBoolean("auto_speak", false)
        urgentAlerts = prefs.getBoolean("urgent_notify", false)
        prefs.edit().putString("self", selfId).apply()
        window.statusBarColor = DARK
        window.navigationBarColor = DARK
        setContentView(buildShell())
        showPage(Page.MESSAGES)
    }

    override fun onDestroy() {
        stopListeners()
        runCatching { recognizer?.destroy() }
        sessionKey?.fill(0)
        sessionKey = null
        executor.shutdownNow()
        store.close()
        super.onDestroy()
    }

    private fun buildShell(): View {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(DARK)
        }
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(14))
        }
        header.addView(text("VĀṆI", 28f, FG, true))
        header.addView(text("OFFLINE SEMANTIC COMMUNICATION", 11f, MUTED).apply {
            letterSpacing = .13f
            setPadding(0, dp(3), 0, dp(10))
        })
        linkLabel = text("", 12f, ACCENT)
        header.addView(linkLabel)
        layout.addView(header)

        stage = FrameLayout(this)
        layout.addView(stage, LinearLayout.LayoutParams(-1, 0, 1f))
        noticeLabel = text("", 12f, MUTED).apply {
            setPadding(dp(18), dp(10), dp(18), dp(10))
            minHeight = dp(40)
            setBackgroundColor(SURFACE)
            maxLines = 3
        }
        layout.addView(noticeLabel)

        navigation = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(8), dp(8), dp(8), dp(12))
            setBackgroundColor(PANEL)
        }
        layout.addView(navigation)
        return layout
    }

    private fun showPage(next: Page) {
        page = next
        composer = null
        stage.removeAllViews()
        val scroll = ScrollView(this).apply {
            isFillViewport = true
            clipToPadding = false
        }
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(10), dp(18), dp(30))
        }
        scroll.addView(col)
        stage.addView(scroll)
        when (next) {
            Page.MESSAGES -> renderMessages(col)
            Page.COMPOSE -> renderCompose(col)
            Page.CONNECT -> renderConnect(col)
            Page.TOOLS -> renderTools(col)
        }
        renderNav()
        renderStatus()
    }

    private fun renderNav() {
        navigation.removeAllViews()
        val tabs = listOf(
            Page.MESSAGES to "Inbox",
            Page.COMPOSE to "Speak / send",
            Page.CONNECT to "Links",
            Page.TOOLS to "Tools"
        )
        for ((route, title) in tabs) {
            val selected = route == page
            val item = TextView(this).apply {
                text = title
                textSize = 12f
                gravity = Gravity.CENTER
                minHeight = dp(48)
                setTextColor(if (selected) DARK else MUTED)
                typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
                background = shape(if (selected) ACCENT else PANEL, 14)
                contentDescription = "Open " + title
                setOnClickListener { showPage(route) }
            }
            navigation.addView(item, LinearLayout.LayoutParams(0, dp(50), 1f).apply {
                setMargins(dp(2), 0, dp(2), 0)
            })
        }
    }

    private fun renderStatus() {
        val listener = when {
            wifiListener?.isRunning() == true -> "Wi-Fi receiver"
            bluetoothListener?.isRunning() == true -> "Bluetooth receiver"
            else -> "No receiver"
        }
        val cm = getSystemService(ConnectivityManager::class.java)
        val network = cm.activeNetwork?.let { cm.getNetworkCapabilities(it) }
        val internet = network?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true
        linkLabel.text = listener + "  •  " + (if (internet) "Internet detected — offline test not proven" else "No validated Internet route")
        noticeLabel.text = notice
    }

    private fun renderMessages(col: LinearLayout) {
        sectionTitle(col, "Message ledger", "Only real local records. No sample conversations or fabricated delivery indicators.")
        val history = try { store.history(120) } catch (error: Exception) {
            notice = "Unable to read local ledger: " + error.message
            emptyList()
        }
        val inbox = history.count { it.direction == Direction.INBOX }
        val queued = history.count { it.direction == Direction.OUTBOX && it.state == DeliveryState.QUEUED }
        val tiles = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        tiles.addView(statCard(inbox.toString(), "Received"), LinearLayout.LayoutParams(0, dp(88), 1f).apply {
            rightMargin = dp(8)
        })
        tiles.addView(statCard(queued.toString(), "Queued"), LinearLayout.LayoutParams(0, dp(88), 1f))
        col.addView(tiles)
        col.addView(spacing(18))
        if (sessionKey == null || recipientId.isBlank() ||
            (if (useBluetooth) bluetoothAddress.isBlank() else wifiAddress.isBlank())) {
            val setup = surfaceCard()
            setup.addView(text("FINISH LOCAL SETUP", 11f, GOLD, true))
            setup.addView(text("Select an adjacent peer, assign its identity, and load a shared key before transmitting.",
                13f, FG))
            setup.addView(spacing(8))
            setup.addView(button("Configure secure link", true) { showPage(Page.CONNECT) })
            col.addView(setup)
            col.addView(spacing(12))
        }
        col.addView(button("Refresh messages", false) { showPage(Page.MESSAGES) })
        col.addView(spacing(12))
        if (history.isEmpty()) {
            val empty = surfaceCard()
            empty.addView(text("No local messages yet", 19f, FG, true))
            empty.addView(text(
                "Set up a shared key in Links, then send a short message or start a receiver. " +
                    "Received text stays encrypted at rest in the M4 ledger.", 14f, MUTED
            ))
            empty.addView(spacing(12))
            empty.addView(button("Compose a message", true) { showPage(Page.COMPOSE) })
            col.addView(empty)
        } else {
            history.forEach { record ->
                val card = surfaceCard()
                val direction = if (record.direction == Direction.INBOX) "INBOUND" else record.direction.name
                card.addView(text(direction + "  ·  " + record.priority.name, 11f, ACCENT, true))
                card.addView(text(
                    if (record.direction == Direction.INBOX) record.sourceId else record.destination,
                    17f, FG, true
                ))
                card.addView(text(
                    "State: " + stateDescription(record.state) + "  •  " +
                        DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                            .format(Date(record.updatedAtEpochMillis)),
                    12f, MUTED
                ))
                card.addView(text(
                    "Encrypted payload: " + record.bundle.size + " bytes  ·  Attempts: " + record.retries,
                    12f, MUTED
                ))
                card.setOnClickListener { openMessage(record) }
                card.contentDescription = "Open " + direction + " message " + record.messageId
                col.addView(card)
                col.addView(spacing(9))
            }
        }
        col.addView(text(
            "A peer receipt is not a signed delivery proof or human acknowledgement. " +
                "Messages expire according to their bundle lifetime.", 12f, MUTED
        ))
    }

    private fun stateDescription(state: DeliveryState): String = when (state) {
        DeliveryState.CREATED -> "Created"
        DeliveryState.VALIDATED -> "Validated"
        DeliveryState.QUEUED -> "Waiting for a peer"
        DeliveryState.TRANSFERRED -> "Peer accepted transfer"
        DeliveryState.RELAYED -> "Forwarded, destination unconfirmed"
        DeliveryState.DELIVERED_DEVICE -> "Authenticated local receipt"
        DeliveryState.PLAYBACK_STARTED -> "Playback reported"
        DeliveryState.ACKNOWLEDGED_PERSON -> "Person acknowledged"
        DeliveryState.EXPIRED -> "Expired"
        DeliveryState.FAILED -> "Failed"
    }

    private fun openMessage(record: DeliveryRecord) {
        val bytes = sessionKey ?: run {
            alert("Encrypted local record", "Load the correct shared session key in Links before viewing or replaying the content.")
            return
        }
        val payload = try {
            val plain = AesGcmMessageProtector(SecretKeySpec(bytes.copyOf(), "AES"))
                .unprotect(record.bundle)
            CanonicalBinaryBundleCodec().decode(plain)
        } catch (error: Exception) {
            alert("Cannot decrypt this record", "The key may not match this message. The stored ciphertext is unchanged.")
            return
        }
        val detail = "Language: " + payload.languageTag +
            "\nPriority: " + payload.priority.name +
            "\nStatus: " + stateDescription(record.state) +
            "\nCritical fields: " + payload.criticalFields.size +
            "\n\n" + payload.transcript
        AlertDialog.Builder(this)
            .setTitle(if (record.direction == Direction.INBOX) "Received message" else "Outgoing message")
            .setMessage(detail)
            .setPositiveButton("Replay speech") { _, _ ->
                M1Speech.speakOffline(this, payload.languageTag, payload.transcript) { result ->
                    updateNotice(if (result.success) "Device-local TTS finished." else "Speech playback unavailable: " + result.error)
                }
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun renderCompose(col: LinearLayout) {
        sectionTitle(col, "Transmit meaning", "Speak locally, review the exact transcript, then send a compact encrypted bundle.")
        val info = surfaceCard()
        info.addView(text("DESTINATION", 11f, ACCENT, true))
        info.addView(text(recipientId.ifBlank { "No recipient configured" }, 19f, FG, true))
        info.addView(text(
            (if (useBluetooth) "Bluetooth Classic" else "Local Wi-Fi") +
                "  ·  " + (if (sessionKey == null) "Set shared key first" else "Session key in memory"),
            12f, MUTED
        ))
        info.addView(button("Edit connection", false) { showPage(Page.CONNECT) })
        col.addView(info)
        col.addView(spacing(16))
        col.addView(text("Message language", 13f, MUTED, true))
        col.addView(darkSpinner(M2Language.entries.map { it.displayName + " · " + it.nativeName }) {
            language = M2Language.entries[it]
        }.apply { setSelection(M2Language.entries.indexOf(language)) })
        col.addView(spacing(12))
        col.addView(text("Transcript (editable)", 13f, MUTED, true))
        val edit = field("Speak or type your message", draft, multiline = true).apply {
            minHeight = dp(140)
            addTextChangedListener(onText { draft = it })
        }
        composer = edit
        col.addView(edit)
        col.addView(spacing(8))
        col.addView(button("Hold to speak • release to stop", true) {}.apply {
            contentDescription = "Push to talk. Hold while speaking, release to finish capture."
            setOnTouchListener { _, event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> { startSpeech(); true }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        runCatching { recognizer?.stopListening() }
                        true
                    }
                    else -> true
                }
            }
        })
        col.addView(button("Tap to start capture (accessibility alternative)", false) { startSpeech() })
        col.addView(spacing(6))
        col.addView(button("Stop speech capture", false) { runCatching { recognizer?.stopListening() } })
        col.addView(spacing(15))
        col.addView(text("Transmission priority", 13f, MUTED, true))
        val priorityRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        fun priorityChoice(value: M4Priority, title: String): TextView {
            val isActive = urgency == value
            return text(title, 14f, if (isActive) DARK else FG, true).apply {
                gravity = Gravity.CENTER
                minHeight = dp(46)
                background = shape(if (isActive) ACCENT else PANEL, 12)
                setOnClickListener { urgency = value; showPage(Page.COMPOSE) }
            }
        }
        priorityRow.addView(priorityChoice(M4Priority.ROUTINE, "Routine"),
            LinearLayout.LayoutParams(0, dp(46), 1f).apply { rightMargin = dp(8) })
        priorityRow.addView(priorityChoice(M4Priority.URGENT, "Urgent"),
            LinearLayout.LayoutParams(0, dp(46), 1f))
        col.addView(priorityRow)
        col.addView(text("Privileged distress routing is withheld until verified role authorization exists.",
            12f, MUTED))
        col.addView(spacing(20))
        col.addView(button("Encrypt and send", true) { submitMessage() })
        col.addView(spacing(8))
        col.addView(text(
            "Safety check: important numbers, negations and coordinates require explicit confirmation. " +
                "Heuristics are not calibrated speech-recognition confidence.", 12f, MUTED
        ))
    }

    private fun startSpeech() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 4101)
            updateNotice("Microphone permission required; retry speech capture after granting it.")
            return
        }
        runCatching { recognizer?.destroy() }
        updateNotice("Using device-provided on-device ASR (open-source model compliance is not established).")
        recognizer = M1Speech.startOfflineAsr(this, language.localeTag) { result ->
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                if (result.transcript.isNullOrBlank()) updateNotice("Recognition failed: " + result.error)
                else {
                    draft = result.transcript
                    composer?.setText(draft)
                    composer?.setSelection(draft.length)
                    updateNotice("Speech recognized locally by platform provider. Review the text before sending.")
                }
            }
        }
    }

    private fun submitMessage() {
        val key = sessionKey?.copyOf()
        if (key == null) { updateNotice("Go to Links and enter or generate a shared session key."); return }
        if (recipientId.isBlank()) { updateNotice("Set a destination ID in Links."); return }
        if (selfId == recipientId) { updateNotice("Sender and recipient device IDs must differ."); return }
        if (draft.isBlank()) { updateNotice("Message transcript is empty."); return }
        val target = if (useBluetooth) bluetoothAddress else wifiAddress
        if (target.isBlank()) { updateNotice("Select an adjacent Bluetooth peer or enter local Wi-Fi IPv4."); return }
        val assessment = try { CriticalInformationGuard.assess(draft.trim(), language.localeTag) }
            catch (error: IllegalArgumentException) {
                updateNotice("Cannot assess safety-critical fields: " + error.message)
                return
            }
        val sender = selfId
        val receiver = recipientId
        val selectedLanguage = language
        val selectedUrgency = urgency
        val viaBluetooth = useBluetooth
        val send = {
            sendAssessed(
                assessment.rawText, assessment.action, assessment.fields,
                key, target, sender, receiver, selectedLanguage, selectedUrgency, viaBluetooth
            )
        }
        if (assessment.action == SafetyAction.SEND) send()
        else AlertDialog.Builder(this)
            .setTitle("Confirm critical information")
            .setMessage(
                "Verify exactly what will be transmitted. Detector confidence does not prove correctness." +
                    "\n\nCritical spans: " + assessment.fields.joinToString { it.value } +
                    "\n\n" + assessment.rawText
            )
            .setPositiveButton("Transmit exact text") { _, _ -> send() }
            .setNegativeButton("Edit", null)
            .show()
    }

    private fun sendAssessed(
        value: String, safety: SafetyAction,
        fields: List<dev.syntax6.vani.m0probe.m1.CriticalField>,
        key: ByteArray, target: String, sender: String, receiver: String,
        selectedLanguage: M2Language, selectedUrgency: M4Priority,
        viaBluetooth: Boolean,
    ) {
        val m1 = M1Message(
            languageTag = selectedLanguage.localeTag, text = value,
            createdElapsedNanos = System.nanoTime(),
            source = sender, destination = receiver,
            priority = selectedUrgency.wire, expiresAfterMillis = 3_600_000L,
            safetyAction = safety, criticalFields = fields,
        )
        val bundle = M4ProtocolAdapter.fromM1(m1).copy(
            ackPolicy = M4AckPolicy.DEVICE_RECEIVED, hopLimit = 1, copyBudget = 1
        )
        updateNotice("Creating encrypted local message. Peer delivery not yet known.")
        executor.execute {
            try {
                val protector = AesGcmMessageProtector(SecretKeySpec(key, "AES"))
                val engine = M4DeliveryEngine(store, protector, sender)
                val bytes = engine.enqueue(bundle)
                val result = transmit(target, M4FrameCodec.fragment(bundle.messageId, bytes), viaBluetooth)
                if (result.accepted) store.transition(bundle.messageId, DeliveryState.TRANSFERRED)
                updateNotice(
                    if (result.accepted) "Adjacent peer accepted encrypted message. Human receipt unconfirmed."
                    else "Peer rejected transfer. Encrypted message remains queued."
                )
            } catch (error: Exception) {
                updateNotice("Transport or encoding unavailable; check queued ledger. " + error.message)
            } finally {
                key.fill(0)
            }
        }
    }

    private fun transmit(target: String, frames: List<ByteArray>, bluetooth: Boolean): TransferReceipt =
        if (bluetooth) M4BluetoothTransport(this).send(PeerId(target), frames)
        else M4LocalWifiTransport().send(PeerId(target), frames)

    private fun renderConnect(col: LinearLayout) {
        sectionTitle(col, "Local connections", "Provision two devices deliberately. Nothing is sent to a cloud service.")
        col.addView(text("My device identity", 13f, MUTED, true))
        col.addView(field("Local ID", selfId).apply {
            addTextChangedListener(onText { selfId = it.trim(); prefs.edit().putString("self", selfId).apply() })
        })
        col.addView(spacing(12))
        col.addView(text("Destination identity", 13f, MUTED, true))
        col.addView(field("Recipient ID", recipientId).apply {
            addTextChangedListener(onText { recipientId = it.trim(); prefs.edit().putString("recipient", recipientId).apply() })
        })
        col.addView(spacing(12))
        col.addView(text("Link transport", 13f, MUTED, true))
        col.addView(button("Use " + (if (useBluetooth) "local Wi-Fi" else "paired Bluetooth"), false) {
            useBluetooth = !useBluetooth
            prefs.edit().putBoolean("bluetooth_mode", useBluetooth).apply()
            showPage(Page.CONNECT)
        })
        col.addView(text("Selected: " + (if (useBluetooth) "Bluetooth Classic / RFCOMM" else "Private local Wi-Fi / TCP"),
            13f, ACCENT, true))
        col.addView(spacing(10))
        if (!useBluetooth) {
            col.addView(text("Local IPv4 candidates: " + localIpCandidates(), 12f, MUTED))
            col.addView(text("Receiver uses TCP port " + M4LocalWifiTransport.PORT + ". " +
                "Both phones need a directly reachable local Wi-Fi link.", 12f, MUTED))
        }
        if (useBluetooth) {
            col.addView(text("Paired peer: " + bluetoothAddress.ifBlank { "Not selected" }, 13f, FG))
            col.addView(button("Select paired Android Bluetooth device", false) { pickBluetoothPeer() })
            col.addView(button("Open system Bluetooth pairing", false) {
                startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
            })
            col.addView(text("This implementation uses Bluetooth Classic, not BLE Mesh. " +
                "Pair the two phones in Android settings first.", 12f, MUTED))
        } else {
            col.addView(text("Adjacent peer IPv4", 13f, MUTED, true))
            col.addView(field("e.g. 192.168.1.25", wifiAddress).apply {
                addTextChangedListener(onText { wifiAddress = it.trim(); prefs.edit().putString("wifi", wifiAddress).apply() })
            })
        }
        col.addView(spacing(14))
        val keyCard = surfaceCard()
        keyCard.addView(text("ENCRYPTION KEY", 12f, ACCENT, true))
        keyCard.addView(text(
            if (sessionKey == null) "Not configured for this app session"
            else "256-bit AES key loaded in memory. Never stored in preferences.",
            14f, FG
        ))
        val keyEntry = field("64 hex characters", "", multiline = false).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        keyCard.addView(keyEntry)
        keyCard.addView(button("Apply shared key", false) {
            val raw = keyEntry.text.toString().trim()
            if (!raw.matches(Regex("^[0-9a-fA-F]{64}$"))) {
                keyEntry.error = "Provide a 64-character hexadecimal key"; return@button
            }
            stopListeners() // Existing sockets are bound to the previous key/identity.
            sessionKey?.fill(0)
            sessionKey = ByteArray(32) { i -> raw.substring(i * 2, i * 2 + 2).toInt(16).toByte() }
            keyEntry.text.clear()
            updateNotice("Key loaded in memory. Both peers must independently use the same key.")
            showPage(Page.CONNECT)
        })
        keyCard.addView(button("Generate new session key", false) { generateKey() })
        keyCard.addView(text("Use a securely exchanged key. Manual key sharing is a research fallback, " +
            "not a production trust/pairing workflow.", 12f, MUTED))
        col.addView(keyCard)
        col.addView(spacing(14))
        col.addView(button(
            if (wifiListener?.isRunning() == true || bluetoothListener?.isRunning() == true)
                "Stop receiving" else "Start receiving on selected link", true
        ) {
            if (wifiListener?.isRunning() == true || bluetoothListener?.isRunning() == true) {
                stopListeners(); updateNotice("Receivers stopped."); showPage(Page.CONNECT)
            } else startReceiver()
        })
        col.addView(spacing(10))
        col.addView(button("Retry queued messages (keep IDs)", false) { retryQueued() })
        col.addView(text("A running receiver captures its identity and key at startup. Restart it after changing either. " +
            "Automatic opportunistic relaying and Bluetooth LE discovery " +
            "are not yet implemented.", 12f, MUTED))
        col.addView(spacing(16))
        val audio = surfaceCard()
        audio.addView(text("PLAYBACK PREFERENCE", 12f, ACCENT, true))
        audio.addView(text(
            if (autoSpeak) "Automatic speech playback is enabled on received messages"
            else "Automatic speech playback is disabled; messages remain readable",
            13f, FG
        ))
        audio.addView(button(if (autoSpeak) "Disable automatic playback" else "Enable automatic playback", false) {
            autoSpeak = !autoSpeak
            prefs.edit().putBoolean("auto_speak", autoSpeak).apply()
            showPage(Page.CONNECT)
        })
        audio.addView(text("Audio respects Android's normal user volume and focus policy.", 12f, MUTED))
        col.addView(audio)
        col.addView(spacing(10))
        val alerts = surfaceCard()
        alerts.addView(text("URGENT NOTIFICATIONS", 12f, GOLD, true))
        alerts.addView(text(
            if (urgentAlerts) "Urgent notification preference enabled"
            else "No system alerts for incoming urgent messages",
            13f, FG
        ))
        alerts.addView(button(if (urgentAlerts) "Disable urgent notifications" else "Enable urgent notifications",
            false) {
            if (urgentAlerts) {
                urgentAlerts = false
                prefs.edit().putBoolean("urgent_notify", false).apply()
                showPage(Page.CONNECT)
            } else {
                if (Build.VERSION.SDK_INT >= 33 &&
                    checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 4103)
                    updateNotice("Android notification permission is required for urgent alerts.")
                } else {
                    urgentAlerts = true
                    prefs.edit().putBoolean("urgent_notify", true).apply()
                    showPage(Page.CONNECT)
                }
            }
        })
        alerts.addView(text("Urgent alerts have a 30-second rate limit, never reveal transcript content, " +
            "and respect notification permissions and Do Not Disturb.", 12f, MUTED))
        col.addView(alerts)
    }

    private fun generateKey() {
        stopListeners()
        sessionKey?.fill(0)
        sessionKey = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val hex = sessionKey!!.joinToString("") { "%02x".format(it.toInt() and 255) }
        AlertDialog.Builder(this)
            .setTitle("New local session key")
            .setMessage("Share with your intended peer using a separate trusted method. " +
                "Copying a key to the clipboard can expose it to other software.\n\n" + hex)
            .setPositiveButton("Copy key") { _, _ ->
                val clip = ClipData.newPlainText("Vani session key", hex)
                getSystemService(ClipboardManager::class.java).setPrimaryClip(clip)
                updateNotice("Key copied at your request. Clear the clipboard when finished.")
            }
            .setNegativeButton("Close", null)
            .show()
        renderStatus()
    }

    private fun pickBluetoothPeer() {
        if (!hasBluetoothPermission()) {
            requestBluetoothPermission()
            return
        }
        val candidates = try { M4BluetoothTransport(this).bondedPeers() }
            catch (error: Exception) { updateNotice("Bluetooth unavailable: " + error.message); return }
        if (candidates.isEmpty()) {
            alert("No paired devices", "Pair a phone in Android Bluetooth settings before selecting it.")
            return
        }
        AlertDialog.Builder(this)
            .setTitle("Choose an already-paired device")
            .setItems(candidates.map { it.first + " • " + it.second }.toTypedArray()) { _, index ->
                bluetoothAddress = candidates[index].second
                prefs.edit().putString("bluetooth", bluetoothAddress).apply()
                showPage(Page.CONNECT)
            }.show()
    }

    private fun hasBluetoothPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED

    private fun requestBluetoothPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requestPermissions(arrayOf(Manifest.permission.BLUETOOTH_CONNECT), 4102)
            updateNotice("Nearby Bluetooth connection permission is required to use paired devices.")
        }
    }

    private fun startReceiver() {
        if (sessionKey == null) { updateNotice("Configure the shared session key first."); return }
        if (selfId.isBlank() || selfId.length > 128) { updateNotice("Set a valid local device ID."); return }
        if (useBluetooth && !hasBluetoothPermission()) { requestBluetoothPermission(); return }
        val key = sessionKey!!.copyOf()
        val identity = selfId
        val engine = M4DeliveryEngine(store, AesGcmMessageProtector(SecretKeySpec(key, "AES")), identity)
        key.fill(0)
        val handler: (ByteArray) -> Boolean = { encrypted ->
            try {
                val incoming = engine.acceptBundle(encrypted)
                runCatching { VaniNotification.maybePost(this, incoming) }
                updateNotice("Received and authenticated message from " + incoming.sourceId +
                    ". Open Inbox to view it.")
                if (autoSpeak) {
                    runOnUiThread {
                        M1Speech.speakOffline(this, incoming.languageTag, incoming.transcript) { result ->
                            if (result.success) runCatching { engine.markPlaybackStarted(incoming.messageId) }
                            updateNotice(if (result.success) "Receiver played the message locally."
                                else "Message saved; speech engine could not play: " + result.error)
                        }
                    }
                }
                true
            } catch (error: Exception) {
                updateNotice("Rejected duplicate, invalid, or foreign encrypted bundle.")
                false
            }
        }
        if (useBluetooth) {
            bluetoothListener = M4BluetoothTransport.Server(handler) { message -> updateNotice(message) }
                .also { it.start(executor) }
        } else {
            wifiListener = M4LocalWifiTransport.Server(handler).also { it.start(executor) }
        }
        updateNotice("Receiver starting. Keep Vāṇī open; no background service is claimed.")
        showPage(Page.CONNECT)
    }

    private fun stopListeners() {
        wifiListener?.stop(); wifiListener = null
        bluetoothListener?.stop(); bluetoothListener = null
    }

    private fun retryQueued() {
        if (useBluetooth && !hasBluetoothPermission()) { requestBluetoothPermission(); return }
        val viaBluetooth = useBluetooth
        val target = if (viaBluetooth) bluetoothAddress else wifiAddress
        val intendedRecipient = recipientId
        if (target.isBlank()) { updateNotice("Configure the adjacent peer address first."); return }
        if (intendedRecipient.isBlank()) {
            updateNotice("Select the recipient identity before retrying queued messages."); return
        }
        executor.execute {
            try {
                val records = store.pending(Direction.OUTBOX)
                    .filter { it.state == DeliveryState.QUEUED && it.destination == intendedRecipient }
                var accepted = 0
                var refused = 0
                for (record in records) {
                    if (!store.incrementRetry(record.messageId)) {
                        store.transition(record.messageId, DeliveryState.FAILED)
                        refused++
                        continue
                    }
                    try {
                        val result = transmit(target, M4FrameCodec.fragment(record.messageId, record.bundle), viaBluetooth)
                        if (result.accepted) {
                            store.transition(record.messageId, DeliveryState.TRANSFERRED)
                            accepted++
                        } else refused++
                    } catch (_: Exception) { refused++ }
                }
                updateNotice("Manual retry: " + accepted + " peer receipts, " + refused +
                    " not accepted. No human acknowledgement implied.")
            } catch (error: Exception) { updateNotice("Could not retry queue: " + error.message) }
        }
    }

    private fun renderTools(col: LinearLayout) {
        sectionTitle(col, "Field instruments", "Diagnostics and research tools remain available without appearing as finished features.")
        toolCard(col, "Language capabilities", "Probe each of the ten configured Android language paths. " +
            "A service being exposed does not prove offline model support.") {
            startActivity(Intent(this, M2Activity::class.java))
        }
        toolCard(col, "Delivery evidence", "View measured local queue state and export redacted metrics, never fabricated WER/latency results.") {
            startActivity(Intent(this, M5DiagnosticsActivity::class.java))
        }
        toolCard(col, "Legacy voice-loop probe", "Open the existing M1 direct transport and platform speech feasibility test.") {
            startActivity(Intent(this, dev.syntax6.vani.m0probe.m1.M1Activity::class.java))
        }
        toolCard(col, "Protocol diagnostics", "Access the separate M4 direct-link experiment, replay and fragmentation tooling.") {
            startActivity(Intent(this, M4DirectActivity::class.java))
        }
        toolCard(col, "Unicode link feasibility", "Open the M0 local-Wi-Fi UTF-8 link and evidence tool.") {
            startActivity(Intent(this, MainActivity::class.java))
        }
        val limitations = surfaceCard()
        limitations.addView(text("IMPLEMENTATION BOUNDARIES", 12f, GOLD, true))
        limitations.addView(text(
            "• Android platform speech providers are experimental; redistributable open-source model packs are still required." +
                "\n• Bluetooth Classic direct transport is not BLE Mesh or automatic multi-hop routing." +
                "\n• Manual shared keys do not constitute authenticated onboarding." +
                "\n• Physical two-/three-phone trials, battery and language benchmarks are intentionally deferred.",
            13f, MUTED
        ))
        col.addView(limitations)
        col.addView(spacing(12))
        col.addView(text("Build revision: " + dev.syntax6.vani.m0probe.BuildConfig.GIT_COMMIT, 11f, MUTED))
    }

    private fun toolCard(col: LinearLayout, title: String, summary: String, action: () -> Unit) {
        val card = surfaceCard()
        card.addView(text(title, 17f, FG, true))
        card.addView(text(summary, 13f, MUTED))
        card.addView(button("Open", false, action))
        col.addView(card)
        col.addView(spacing(10))
    }

    private fun sectionTitle(col: LinearLayout, title: String, subtitle: String) {
        col.addView(text(title, 25f, FG, true))
        col.addView(spacing(5))
        col.addView(text(subtitle, 13f, MUTED))
        col.addView(spacing(18))
    }

    private fun statCard(value: String, label: String): LinearLayout = surfaceCard().apply {
        addView(text(value, 24f, FG, true))
        addView(text(label, 12f, MUTED))
    }

    private fun surfaceCard() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(17), dp(15), dp(17), dp(15))
        background = shape(PANEL, 18, BORDER)
        minimumHeight = dp(60)
    }

    private fun text(value: String, size: Float, color: Int, bold: Boolean = false) =
        TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            if (bold) typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            setLineSpacing(dp(2).toFloat(), 1f)
        }

    private fun button(value: String, emphasized: Boolean, action: () -> Unit) = Button(this).apply {
        text = value
        textSize = 14f
        isAllCaps = false
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        minHeight = dp(48)
        setPadding(dp(10), dp(8), dp(10), dp(8))
        background = shape(if (emphasized) ACCENT else SURFACE, 14,
            if (emphasized) ACCENT else BORDER)
        setTextColor(if (emphasized) DARK else FG)
        setOnClickListener { action() }
    }

    private fun field(hint: String, value: String, multiline: Boolean = false) = EditText(this).apply {
        this.hint = hint
        setText(value)
        setTextColor(FG)
        setHintTextColor(MUTED)
        textSize = 15f
        minHeight = if (multiline) dp(90) else dp(50)
        setPadding(dp(14), dp(12), dp(14), dp(12))
        background = shape(SURFACE, 13, BORDER)
        if (multiline) {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or
                InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            gravity = Gravity.TOP or Gravity.START
        } else setSingleLine(true)
        contentDescription = hint
    }

    private fun darkSpinner(options: List<String>, onSelect: (Int) -> Unit): Spinner =
        Spinner(this).apply {
            val adapter = object : ArrayAdapter<String>(this@VaniFieldActivity,
                android.R.layout.simple_spinner_dropdown_item, options) {
                override fun getView(position: Int, convertView: View?, parent: android.view.ViewGroup): View =
                    optionRow(getItem(position).orEmpty(), false)
                override fun getDropDownView(position: Int, convertView: View?,
                    parent: android.view.ViewGroup): View =
                    optionRow(getItem(position).orEmpty(), true)
            }
            this.adapter = adapter
            onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?,
                    position: Int, id: Long) = onSelect(position)
                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) = Unit
            }
        }

    private fun optionRow(label: String, dropdown: Boolean) = TextView(this).apply {
        text = label
        setTextColor(FG)
        textSize = 15f
        setPadding(dp(14), dp(13), dp(14), dp(13))
        background = shape(if (dropdown) PANEL else SURFACE, 12)
    }

    private fun onText(onChanged: (String) -> Unit) = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            onChanged(s?.toString().orEmpty())
        }
        override fun afterTextChanged(s: Editable?) = Unit
    }

    private fun localIpCandidates(): String {
        return try {
            java.net.NetworkInterface.getNetworkInterfaces().toList()
                .filter { it.isUp && !it.isLoopback }
                .flatMap { it.inetAddresses.toList() }
                .filterIsInstance<java.net.Inet4Address>()
                .filter { !it.isLoopbackAddress && (it.isSiteLocalAddress || it.isLinkLocalAddress) }
                .map { it.hostAddress.orEmpty() }
                .distinct()
                .take(4)
                .joinToString(", ")
                .ifBlank { "none detected" }
        } catch (_: Exception) { "not available" }
    }

    private fun updateNotice(value: String) {
        runOnUiThread {
            if (!isFinishing && !isDestroyed) {
                notice = value
                renderStatus()
            }
        }
    }

    private fun alert(title: String, description: String) =
        AlertDialog.Builder(this).setTitle(title).setMessage(description)
            .setPositiveButton("Close", null).show()

    override fun onRequestPermissionsResult(requestCode: Int,
        permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 4103) {
            if (grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
                urgentAlerts = true
                prefs.edit().putBoolean("urgent_notify", true).apply()
                showPage(Page.CONNECT)
                updateNotice("Android urgent notifications are enabled by user permission.")
            } else updateNotice("Urgent notification permission denied; alerts stay off.")
            return
        }
        if (requestCode == 4101 || requestCode == 4102) {
            updateNotice(if (grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED)
                "Permission granted. Repeat the requested action."
            else "Permission was not granted. Feature remains unavailable.")
        }
    }

    private fun spacing(dpValue: Int) = View(this).apply {
        layoutParams = LinearLayout.LayoutParams(1, dp(dpValue))
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density + .5f).toInt()
    private fun shape(fill: Int, corner: Int, border: Int? = null) = GradientDrawable().apply {
        setColor(fill)
        cornerRadius = dp(corner).toFloat()
        if (border != null) setStroke(dp(1), border)
    }

    companion object {
        private val DARK = Color.rgb(12, 23, 29)
        private val SURFACE = Color.rgb(28, 45, 52)
        private val PANEL = Color.rgb(21, 36, 43)
        private val BORDER = Color.rgb(56, 77, 82)
        private val FG = Color.rgb(238, 247, 244)
        private val MUTED = Color.rgb(163, 184, 183)
        private val ACCENT = Color.rgb(126, 226, 188)
        private val GOLD = Color.rgb(245, 194, 111)
    }
}
