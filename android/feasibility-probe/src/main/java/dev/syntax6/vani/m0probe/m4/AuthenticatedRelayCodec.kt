package dev.syntax6.vani.m4

import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.SecretKey

/**
 * Opt-in routing-domain authenticity for the relay metadata and opaque payload.
 *
 * Every forwarding relay needs a provisioned domain key and MUST re-authenticate
 * the changed hop/copy counters before forwarding. This is not end-to-end
 * confidentiality; it is hop/domain authenticity. A compromised member of the
 * relay domain can still forge routing privileges.
 */
class AuthenticatedRelayEnvelopeCodec(private val domainKey: SecretKey) {
    companion object {
        const val TAG_BYTES = 32
    }

    fun encode(envelope: RelayEnvelope): ByteArray {
        val unsigned = RelayEnvelopeCodec.encode(envelope)
        return unsigned + authenticate(unsigned)
    }

    fun decode(wire: ByteArray, now: Long = System.currentTimeMillis()): RelayEnvelope {
        require(wire.size > TAG_BYTES && wire.size <= M4_MAX_BUNDLE_BYTES + 128) {
            "invalid authenticated relay envelope size"
        }
        val body = wire.copyOfRange(0, wire.size - TAG_BYTES)
        val provided = wire.copyOfRange(wire.size - TAG_BYTES, wire.size)
        require(MessageDigest.isEqual(authenticate(body), provided)) { "relay authentication failed" }
        return RelayEnvelopeCodec.decode(body, now)
    }

    private fun authenticate(bytes: ByteArray): ByteArray =
        Mac.getInstance("HmacSHA256").apply { init(domainKey) }.doFinal(bytes)
}
