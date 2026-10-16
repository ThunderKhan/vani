package dev.syntax6.vani.m0probe.ui

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import dev.syntax6.vani.m0probe.BuildConfig
import dev.syntax6.vani.m4.DeliveryState
import dev.syntax6.vani.m4.Direction
import dev.syntax6.vani.m4.M4DeliveryStore
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.util.concurrent.Executors

/**
 * Privacy-preserving local M5 evidence summary.
 * No fabricated ASR/STT timing, model performance or multi-hop metrics.
 */
class M5DiagnosticsActivity : Activity() {
    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var store: M4DeliveryStore
    private lateinit var root: LinearLayout
    private lateinit var status: TextView
    private var lastExport: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = M4DeliveryStore(this)
        val scroll = ScrollView(this).apply { setBackgroundColor(BACKGROUND) }
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(24), dp(20), dp(30))
        }
        scroll.addView(root)
        setContentView(scroll)
        showReport()
    }

    override fun onDestroy() {
        executor.shutdownNow()
        store.close()
        super.onDestroy()
    }

    private fun showReport() {
        root.removeAllViews()
        root.addView(label("Evidence monitor", 26f, FOREGROUND, true))
        root.addView(label("Local, measured app records only", 13f, MUTED))
        root.addView(space(16))
        val observations = store.history(200)
        val queued = observations.count { it.state == DeliveryState.QUEUED }
        val inbox = observations.count { it.direction == Direction.INBOX }
        val transferred = observations.count { it.state == DeliveryState.TRANSFERRED }
        val bytes = observations.sumOf { it.bundle.size.toLong() }
        val network = getSystemService(ConnectivityManager::class.java)
        val caps = network.activeNetwork?.let { network.getNetworkCapabilities(it) }
        val validated = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true

        root.addView(metricCard("Durable records", observations.size.toString(),
            "Latest 200 local metadata records"))
        root.addView(metricCard("Authenticated inbox records", inbox.toString(),
            "Stored after M4 destination decryption and validation"))
        root.addView(metricCard("Queued / awaiting transfer", queued.toString(),
            "Delivery may happen later; no success is inferred"))
        root.addView(metricCard("Peer transfer receipts", transferred.toString(),
            "Not signed end-recipient or human acknowledgements"))
        root.addView(metricCard("Stored ciphertext bytes", bytes.toString(),
            "Byte total from the selected 200 local records"))
        root.addView(metricCard("Validated Internet", validated.toString(),
            "This snapshot is not an airplane-mode test result"))
        root.addView(space(16))
        val missing = panel()
        missing.addView(label("NOT YET MEASURED", 12f, AMBER, true))
        missing.addView(label(
            "Offline model WER/CER • human TTS intelligibility • endpoint latency • " +
                "cold/warm inference • P50/P90/P95 speech-to-speech delay • " +
                "BLE packet loss • physical three-phone relay • energy and thermal load",
            14f, MUTED
        ))
        root.addView(missing)
        root.addView(space(16))
        root.addView(label("Build: " + BuildConfig.GIT_COMMIT, 11f, MUTED))
        root.addView(space(10))
        root.addView(action("Refresh local records") { showReport() })
        root.addView(space(8))
        root.addView(action("Export redacted JSON report") { exportReport() })
        status = label("No export performed.", 12f, MUTED).apply {
            setPadding(0, dp(8), 0, 0)
        }
        root.addView(status)
    }

    private fun exportReport() {
        val observations = try { store.history(200) }
            catch (error: Exception) {
                status.text = "Local store unavailable: " + error.message
                return
            }
        val totals = JSONObject()
        DeliveryState.entries.forEach { state ->
            totals.put(state.name, observations.count { it.state == state })
        }
        val byTransport = JSONObject()
        Direction.entries.forEach { direction ->
            byTransport.put(direction.name, observations.count { it.direction == direction })
        }
        val report = JSONObject()
            .put("schema_version", 1)
            .put("report_type", "redacted_local_delivery_metadata")
            .put("collected_at_utc", Instant.now().toString())
            .put("git_commit", BuildConfig.GIT_COMMIT)
            .put("records_in_sample", observations.size)
            .put("sample_limit", 200)
            .put("state_counts", totals)
            .put("direction_counts", byTransport)
            .put("ciphertext_bytes", observations.sumOf { it.bundle.size.toLong() })
            .put("speech_metrics", JSONObject.NULL)
            .put("physical_relay_metrics", JSONObject.NULL)
            .put("energy_metrics", JSONObject.NULL)
            .put("limitations", JSONArray(listOf(
                "No plaintext, keys, IDs or ciphertext included",
                "This export is a local store snapshot, not a benchmark",
                "Transport receipt does not certify end-recipient or human acknowledgment",
                "No unsupported speech, energy or mesh measurements are imputed"
            )))
        lastExport = report.toString(2) + "\n"
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/json"
            putExtra(Intent.EXTRA_TITLE, "vani-redacted-local-evidence.json")
        }
        startActivityForResult(intent, EXPORT_REQUEST)
    }

    @Deprecated("Uses the existing Activity API, no AndroidX dependency.")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != EXPORT_REQUEST || resultCode != RESULT_OK) return
        val uri = data?.data ?: return
        val payload = lastExport ?: return
        executor.execute {
            val success = try {
                contentResolver.openOutputStream(uri, "w")?.use { output ->
                    output.write(payload.toByteArray(Charsets.UTF_8))
                } != null
            } catch (_: Exception) { false }
            runOnUiThread {
                status.text = if (success) "Redacted local report written."
                else "Export failed; no benchmark was recorded."
            }
        }
    }

    private fun metricCard(title: String, value: String, note: String) = panel().apply {
        addView(label(title, 12f, MUTED))
        addView(label(value, 25f, FOREGROUND, true))
        addView(label(note, 12f, MUTED))
    }

    private fun panel() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(16), dp(14), dp(16), dp(14))
        background = GradientDrawable().apply {
            setColor(PANEL)
            cornerRadius = dp(16).toFloat()
            setStroke(dp(1), STROKE)
        }
        val params = LinearLayout.LayoutParams(-1, -2)
        params.bottomMargin = dp(9)
        layoutParams = params
    }

    private fun label(value: String, size: Float, color: Int, weight: Boolean = false) =
        TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            if (weight) typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            setLineSpacing(dp(2).toFloat(), 1f)
        }

    private fun action(value: String, callback: () -> Unit) = Button(this).apply {
        text = value
        isAllCaps = false
        setTextColor(BACKGROUND)
        minHeight = dp(48)
        background = GradientDrawable().apply {
            setColor(ACCENT)
            cornerRadius = dp(14).toFloat()
        }
        setOnClickListener { callback() }
    }

    private fun space(height: Int) = View(this).apply {
        layoutParams = LinearLayout.LayoutParams(1, dp(height))
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density + .5f).toInt()

    companion object {
        private const val EXPORT_REQUEST = 5301
        private val BACKGROUND = Color.rgb(12, 23, 29)
        private val PANEL = Color.rgb(21, 36, 43)
        private val STROKE = Color.rgb(56, 77, 82)
        private val FOREGROUND = Color.rgb(238, 247, 244)
        private val MUTED = Color.rgb(163, 184, 183)
        private val ACCENT = Color.rgb(126, 226, 188)
        private val AMBER = Color.rgb(245, 194, 111)
    }
}
