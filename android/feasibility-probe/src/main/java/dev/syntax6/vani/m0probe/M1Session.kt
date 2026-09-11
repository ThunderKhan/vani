package dev.syntax6.vani.m0probe

import java.util.concurrent.atomic.AtomicReference

class M1Session {
    private val state = AtomicReference(DeliveryState.State.FAILED)
    private var bundle: SemanticBundle? = null

    fun queue(transcript: String, language: String, sourceId: String, destination: String): SemanticBundle {
        val created = SemanticBundle(
            sourceId = sourceId,
            destination = destination,
            languageCode = language,
            transcript = transcript,
            expiryEpochMs = System.currentTimeMillis() + 5 * 60_000L,
        )
        created.validate()
        bundle = created
        state.set(DeliveryState.State.QUEUED)
        DeliveryState.transition(created.messageId, DeliveryState.State.QUEUED)
        return created
    }

    fun markTransferred() = update(DeliveryState.State.TRANSFERRED)
    fun markDelivered() = update(DeliveryState.State.DELIVERED)
    fun markAcknowledged() = update(DeliveryState.State.ACKNOWLEDGED)
    fun markFailed() = update(DeliveryState.State.FAILED)
    fun currentState(): DeliveryState.State = state.get()
    fun currentBundle(): SemanticBundle? = bundle
    fun encodeForTransport(): ByteArray = bundle?.encodeUtf8() ?: error("No queued message")
    fun decodeReceived(bytes: ByteArray): SemanticBundle = SemanticBundle.decodeUtf8(bytes)

    private fun update(next: DeliveryState.State) {
        val id = bundle?.messageId ?: return
        state.set(next)
        DeliveryState.transition(id, next)
    }
}
