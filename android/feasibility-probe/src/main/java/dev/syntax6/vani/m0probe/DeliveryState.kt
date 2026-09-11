package dev.syntax6.vani.m0probe

import java.util.concurrent.ConcurrentHashMap

object DeliveryState {
    enum class State { QUEUED, TRANSFERRED, DELIVERED, ACKNOWLEDGED, FAILED }

    data class Record(
        val messageId: String,
        val state: State,
        val updatedAtMs: Long = System.currentTimeMillis(),
        val duplicate: Boolean = false,
    )

    private val states = ConcurrentHashMap<String, Record>()

    fun transition(messageId: String, state: State, duplicate: Boolean = false): Record =
        Record(messageId, state, System.currentTimeMillis(), duplicate).also { states[messageId] = it }

    fun get(messageId: String): Record? = states[messageId]
}
