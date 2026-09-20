package dev.syntax6.vani.m4

import java.util.UUID

class M4DeliveryEngine(
    private val store: M4DeliveryStore,
    private val codec: SemanticBundleCodec = CanonicalBinaryBundleCodec(),
    private val protector: MessageProtector? = null,
) {
    fun enqueue(message: SemanticBundle, now: Long = System.currentTimeMillis()): ByteArray {
        message.validate(now)
        val logical = codec.encode(message)
        val protected = protector?.protect(logical) ?: logical
        require(protected.size <= M4_MAX_BUNDLE_BYTES) { "protected bundle exceeds bound" }
        store.enqueue(protected, message, Direction.OUTBOX, now)
        return protected
    }

    fun acceptBundle(bytes: ByteArray, now: Long = System.currentTimeMillis()): SemanticBundle {
        require(bytes.size <= M4_MAX_BUNDLE_BYTES) { "bundle exceeds bound" }
        val plaintext = protector?.unprotect(bytes) ?: bytes
        val message = codec.decode(plaintext)
        message.validate(now)
        check(store.markSeenIfNew(message.messageId, message.expiresAtEpochMillis, now)) { "duplicate or replayed message" }
        store.persistReceived(bytes, message, now)
        return message
    }

    fun markDelivered(id: UUID, now: Long = System.currentTimeMillis()) =
        store.transition(id, DeliveryState.DELIVERED_DEVICE, now)

    fun markPlaybackStarted(id: UUID, now: Long = System.currentTimeMillis()) =
        store.transition(id, DeliveryState.PLAYBACK_STARTED, now)

    fun markPersonAcknowledged(id: UUID, now: Long = System.currentTimeMillis()) =
        store.transition(id, DeliveryState.ACKNOWLEDGED_PERSON, now)
}
