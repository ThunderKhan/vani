package dev.syntax6.vani.m4

import java.util.UUID

data class DeliveryAcknowledgement(val messageId: UUID, val state: DeliveryState, val receiverId: String, val authenticated: Boolean)

object AckCorrelator {
    fun accept(expectedMessageId: UUID, ack: DeliveryAcknowledgement): Boolean {
        require(ack.authenticated) { "unauthenticated ACK" }
        require(ack.receiverId.isNotBlank()) { "invalid receiver identity" }
        return ack.messageId == expectedMessageId &&
            ack.state in setOf(DeliveryState.DELIVERED_DEVICE, DeliveryState.PLAYBACK_STARTED, DeliveryState.ACKNOWLEDGED_PERSON)
    }
}
