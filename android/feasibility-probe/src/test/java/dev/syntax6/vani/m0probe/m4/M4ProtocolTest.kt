package dev.syntax6.vani.m0probe.m4

import dev.syntax6.vani.m0probe.m1.SafetyAction
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.charset.StandardCharsets

class M4ProtocolTest {
    private fun bundle(
        text: String = "नमस्ते",
        extensions: List<M4Protocol.Extension> = emptyList(),
        created: Long = 1_700_000_000_000L,
    ) = M4Protocol.SemanticBundle(
        messageId = "00000000-0000-0000-0000-000000000001",
        conversationId = "incident-1",
        sourceId = "alpha",
        destinationId = "bravo",
        languageTag = "hi-IN",
        priority = M4Protocol.Priority.URGENT,
        messageType = M4Protocol.MessageType.TEXT,
        createdAtEpochMillis = created,
        expiresAfterMillis = 30_000,
        hopLimit = 8,
        copyBudget = 4,
        ackPolicy = M4Protocol.AckPolicy.PLAYBACK,
        safetyAction = SafetyAction.CONFIRM,
        transcript = text,
        extensions = extensions,
    )

    @Test fun canonicalVectorIsStable() {
        val expected = "56414e34010001002430303030303030302d303030302d303030302d303030302d3030303030303030303030310a696e636964656e742d3105616c70686105627261766f0568692d494e0000018bcfe568000000000000007530080402010012e0a4a8e0a4aee0a4b8e0a58de0a4a4e0a5870000"
        assertEquals(expected, CanonicalBinarySemanticCodec.encode(bundle()).joinToString("") { "%02x".format(it) })
    }

    @Test fun unicodeRoundTripIsExact() {
        val original = bundle("தமிழ் • VĀṆI")
        assertEquals(original, CanonicalBinarySemanticCodec.decode(CanonicalBinarySemanticCodec.encode(original)))
    }

    @Test fun unknownNonCriticalExtensionIsPreserved() {
        val original = bundle(extensions = listOf(M4Protocol.Extension(42, false, byteArrayOf(1, 2, 3))))
        val decoded = CanonicalBinarySemanticCodec.decode(CanonicalBinarySemanticCodec.encode(original))
        assertEquals(1, decoded.extensions.size)
        assertArrayEquals(byteArrayOf(1, 2, 3), decoded.extensions.single().payload)
    }

    @Test(expected = IllegalStateException::class)
    fun unknownCriticalExtensionIsRejected() {
        CanonicalBinarySemanticCodec.decode(
            CanonicalBinarySemanticCodec.encode(bundle(extensions = listOf(M4Protocol.Extension(99, true, byteArrayOf(7))))
                .also { })
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun oversizedBundleIsRejected() {
        CanonicalBinarySemanticCodec.encode(bundle("x".repeat(9000)))
    }

    @Test(expected = IllegalArgumentException::class)
    fun malformedUtf8IsRejected() {
        val bytes = CanonicalBinarySemanticCodec.encode(bundle()).copyOf()
        bytes[bytes.lastIndex] = 0xff.toByte()
        CanonicalBinarySemanticCodec.decode(bytes)
    }

    @Test(expected = IllegalArgumentException::class)
    fun malformedVersionIsRejected() {
        val bytes = CanonicalBinarySemanticCodec.encode(bundle()).copyOf()
        bytes[4] = 99
        CanonicalBinarySemanticCodec.decode(bytes)
    }

    @Test fun fragmentationReassemblesOutOfOrderAndSuppressesDuplicates() {
        val payload = "abcdefghijklmnopqrstuvwxyz".toByteArray(StandardCharsets.UTF_8)
        val fragments = M4Fragmenter.split("m", payload, 5)
        val r = FragmentReassembler()
        var result: FragmentReassembler.Result = FragmentReassembler.Result.Incomplete
        listOf(fragments[2], fragments[0], fragments[2], fragments[4], fragments[1], fragments[3]).forEach {
            result = r.accept(it, 10)
        }
        assertTrue(result is FragmentReassembler.Result.Complete)
        assertArrayEquals(payload, (result as FragmentReassembler.Result.Complete).bytes)
    }

    @Test fun missingFragmentNeverCompletes() {
        val fragments = M4Fragmenter.split("m", ByteArray(20) { it.toByte() }, 5)
        val r = FragmentReassembler()
        fragments.filter { it.fragmentIndex != 2 }.forEach { r.accept(it, 10) }
        assertTrue(r.accept(fragments[0], 10) is FragmentReassembler.Result.Duplicate)
    }
}
