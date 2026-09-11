package dev.syntax6.vani.m0probe.m1

import android.content.Context
import java.util.UUID

/** Explicit sender states required by the M1 delivery contract. */
enum class M1DeliveryState { QUEUED, TRANSFERRED, DELIVERED, ACKNOWLEDGED, FAILED }

data class M1Message(
    val id: String = UUID.randomUUID().toString(),
    val languageTag: String,
    val text: String,
    val createdElapsedNanos: Long,
)

class M1DeliveryStore(context: Context) {
    private val prefs = context.getSharedPreferences("m1_delivery", Context.MODE_PRIVATE)

    fun state(id: String): M1DeliveryState? = prefs.getString("state:$id", null)?.let { runCatching { M1DeliveryState.valueOf(it) }.getOrNull() }

    fun setState(id: String, state: M1DeliveryState) {
        prefs.edit().putString("state:$id", state.name).apply()
    }

    fun wasPlayed(id: String): Boolean = prefs.getBoolean("played:$id", false)

    fun markPlayed(id: String) {
        prefs.edit().putBoolean("played:$id", true).apply()
    }
}
