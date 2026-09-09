package dev.syntax6.vani.m0probe

import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant

object ExperimentEvidence {
    data class RunContext(
        val startedAt: Instant,
        val startOffline: DeviceState.OfflineSnapshot,
        val startDevice: DeviceState.DeviceSnapshot,
    )

    fun begin(context: android.content.Context): RunContext = RunContext(
        startedAt = Instant.now(),
        startOffline = DeviceState.offlineSnapshot(context),
        startDevice = DeviceState.deviceSnapshot(context),
    )

    fun buildSenderRecord(
        run: RunContext,
        endOffline: DeviceState.OfflineSnapshot,
        endDevice: DeviceState.DeviceSnapshot,
        host: String,
        port: Int,
        sendResult: ProbeClient.SendResult?,
        error: Throwable?,
    ): String {
        val finishedAt = Instant.now()
        val commitValid = BuildConfig.GIT_COMMIT.matches(Regex("^[0-9a-f]{7,40}$")) &&
            BuildConfig.GIT_COMMIT != "0000000"
        val offlineProven = !run.startOffline.validatedInternetAvailable &&
            !endOffline.validatedInternetAvailable
        val linkPassed = error == null &&
            sendResult?.receiverAccepted == true &&
            sendResult.integrityMatched

        val result = when {
            error != null -> "fail"
            !linkPassed -> "fail"
            !offlineProven || !commitValid -> "blocked"
            else -> "pass"
        }

        val failureReason = when {
            error != null -> error.message ?: error::class.java.simpleName
            !linkPassed -> "Receiver ACK or SHA-256 integrity validation failed."
            !offlineProven -> "Validated Internet capability was present during the run; repeat with Internet unavailable."
            !commitValid -> "Git commit was not embedded in this build; rebuild from a Git checkout before recording M0 evidence."
            else -> null
        }

        val payloadBytes = sendResult?.payloadBytes
        val payloadHash = sendResult?.payloadSha256Hex

        val root = JSONObject()
            .put("schema_version", 1)
            .put("experiment_id", "M0-UNICODE-LINK-001-${run.startedAt.epochSecond}")
            .put("git_commit", BuildConfig.GIT_COMMIT)
            .put("build_variant", "android-feasibility-probe")
            .put("started_at_utc", run.startedAt.toString())
            .put("finished_at_utc", finishedAt.toString())
            .put(
                "offline_conditions",
                JSONObject()
                    .put("internet_blocked", offlineProven)
                    .put("airplane_mode", run.startOffline.airplaneMode && endOffline.airplaneMode)
                    .put("remote_api_calls_observed", false)
                    .put("local_radios_enabled", JSONArray(endOffline.localRadios))
                    .put(
                        "notes",
                        "NET_CAPABILITY_VALIDATED was ${run.startOffline.validatedInternetAvailable} at start and ${endOffline.validatedInternetAvailable} at finish. The probe contains no remote service endpoint; this is not a packet-capture proof.",
                    ),
            )
            .put("device_profile", deviceJson(run.startDevice, endDevice))
            .put("model", JSONObject.NULL)
            .put(
                "runtime_configuration",
                JSONObject()
                    .put("runtime", "java.net.Socket/ServerSocket")
                    .put("runtime_version", System.getProperty("java.version") ?: "")
                    .put("threads", 1),
            )
            .put("language", "en")
            .put("audio_fixture_or_dataset_reference", JSONObject.NULL)
            .put(
                "network_topology_and_seed",
                JSONObject()
                    .put("transport", "local-wifi-tcp")
                    .put("host", host)
                    .put("port", port)
                    .put("payload_bytes", payloadBytes ?: JSONObject.NULL)
                    .put("payload_sha256", payloadHash ?: JSONObject.NULL)
                    .put("receiver_device", sendResult?.receiverInfo ?: JSONObject.NULL),
            )
            .put("thermal_and_power_conditions", JSONObject.NULL)
            .put("result", result)
            .put(
                "raw_stage_metrics",
                JSONObject()
                    .put("send_start_monotonic_ns", sendResult?.startedAtNanos ?: JSONObject.NULL)
                    .put("send_finish_monotonic_ns", sendResult?.finishedAtNanos ?: JSONObject.NULL)
                    .put("receiver_ack_monotonic_ns_remote_clock", sendResult?.receiverElapsedNanos ?: JSONObject.NULL),
            )
            .put(
                "derived_metrics",
                JSONObject()
                    .put("round_trip_ms", sendResult?.roundTripMillis ?: JSONObject.NULL)
                    .put("payload_bytes", payloadBytes ?: JSONObject.NULL)
                    .put("integrity_matched", sendResult?.integrityMatched ?: false)
                    .put("receiver_accepted", sendResult?.receiverAccepted ?: false)
                    .put("validated_internet_start", run.startOffline.validatedInternetAvailable)
                    .put("validated_internet_finish", endOffline.validatedInternetAvailable),
            )
            .put("failure_reason", failureReason ?: JSONObject.NULL)
            .put(
                "notes",
                "M0 transport-only evidence. The payload may contain all ten required scripts; language=en is retained only because the current experiment schema is speech-oriented. A PASS proves bounded UTF-8 local transfer + ACK + SHA-256 integrity under the recorded offline state, not production security or delivery semantics.",
            )

        return root.toString(2)
    }

    fun buildReceiverRecord(
        context: android.content.Context,
        receiveResult: ProbeServer.ReceiveResult,
    ): String {
        val offline = DeviceState.offlineSnapshot(context)
        val device = DeviceState.deviceSnapshot(context)
        return JSONObject()
            .put("event", "m0_unicode_link_receive")
            .put("git_commit", BuildConfig.GIT_COMMIT)
            .put("received_at_utc", Instant.now().toString())
            .put("received_at_monotonic_ns", receiveResult.receivedAtNanos)
            .put("payload_bytes", receiveResult.payload.size)
            .put("payload_sha256", receiveResult.sha256Hex)
            .put("payload_utf8", receiveResult.payload.toString(Charsets.UTF_8))
            .put("sender_device", receiveResult.senderInfo)
            .put("sender_address", receiveResult.remoteAddress)
            .put("receiver_device", device.shortLabel())
            .put("airplane_mode", offline.airplaneMode)
            .put("validated_internet_available", offline.validatedInternetAvailable)
            .toString(2)
    }

    private fun deviceJson(
        start: DeviceState.DeviceSnapshot,
        end: DeviceState.DeviceSnapshot,
    ): JSONObject = JSONObject()
        .put("manufacturer", start.manufacturer)
        .put("model", start.model)
        .put("soc", start.soc)
        .put("android_version", start.androidVersion)
        .put("ram_mb", start.ramMb)
        .put("battery_percent_start", start.batteryPercent ?: JSONObject.NULL)
        .put("battery_percent_end", end.batteryPercent ?: JSONObject.NULL)
        .put("temperature_c_start", start.batteryTemperatureC ?: JSONObject.NULL)
        .put("temperature_c_end", end.batteryTemperatureC ?: JSONObject.NULL)
}
