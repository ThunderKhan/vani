package dev.syntax6.vani.m0probe

import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

object LinkProtocol {
    const val VERSION: Int = 1
    const val MAX_PAYLOAD_BYTES: Int = 64 * 1024
    const val MAX_DEVICE_INFO_BYTES: Int = 512

    private const val DATA_MAGIC: Int = 0x56414E49 // "VANI"
    private const val ACK_MAGIC: Int = 0x41434B31 // "ACK1"
    private const val HASH_BYTES: Int = 32

    data class DataFrame(
        val senderElapsedNanos: Long,
        val senderInfo: String,
        val payload: ByteArray,
        val sha256: ByteArray,
    )

    data class AckFrame(
        val accepted: Boolean,
        val receiverElapsedNanos: Long,
        val receiverInfo: String,
        val payloadLength: Int,
        val sha256: ByteArray,
    )

    @Throws(IOException::class)
    fun writeDataFrame(
        output: DataOutputStream,
        senderElapsedNanos: Long,
        senderInfo: String,
        payload: ByteArray,
    ) {
        require(payload.size in 1..MAX_PAYLOAD_BYTES) {
            "Payload must be 1..$MAX_PAYLOAD_BYTES bytes"
        }
        val hash = sha256(payload)
        output.writeInt(DATA_MAGIC)
        output.writeByte(VERSION)
        output.writeLong(senderElapsedNanos)
        writeUtf8(output, senderInfo, MAX_DEVICE_INFO_BYTES)
        output.writeInt(payload.size)
        output.write(hash)
        output.write(payload)
        output.flush()
    }

    @Throws(IOException::class)
    fun readDataFrame(input: DataInputStream): DataFrame {
        val magic = input.readInt()
        if (magic != DATA_MAGIC) throw ProtocolException("Unexpected data-frame magic")

        val version = input.readUnsignedByte()
        if (version != VERSION) throw ProtocolException("Unsupported data-frame version: $version")

        val senderElapsedNanos = input.readLong()
        val senderInfo = readUtf8(input, MAX_DEVICE_INFO_BYTES)
        val payloadLength = input.readInt()
        if (payloadLength !in 1..MAX_PAYLOAD_BYTES) {
            throw ProtocolException("Invalid payload length: $payloadLength")
        }

        val expectedHash = ByteArray(HASH_BYTES)
        input.readFully(expectedHash)
        val payload = ByteArray(payloadLength)
        input.readFully(payload)

        val actualHash = sha256(payload)
        if (!MessageDigest.isEqual(expectedHash, actualHash)) {
            throw ProtocolException("Payload SHA-256 mismatch")
        }

        return DataFrame(
            senderElapsedNanos = senderElapsedNanos,
            senderInfo = senderInfo,
            payload = payload,
            sha256 = actualHash,
        )
    }

    @Throws(IOException::class)
    fun writeAckFrame(
        output: DataOutputStream,
        accepted: Boolean,
        receiverElapsedNanos: Long,
        receiverInfo: String,
        payloadLength: Int,
        sha256: ByteArray,
    ) {
        require(payloadLength in 1..MAX_PAYLOAD_BYTES)
        require(sha256.size == HASH_BYTES)

        output.writeInt(ACK_MAGIC)
        output.writeByte(VERSION)
        output.writeBoolean(accepted)
        output.writeLong(receiverElapsedNanos)
        writeUtf8(output, receiverInfo, MAX_DEVICE_INFO_BYTES)
        output.writeInt(payloadLength)
        output.write(sha256)
        output.flush()
    }

    @Throws(IOException::class)
    fun readAckFrame(input: DataInputStream): AckFrame {
        val magic = input.readInt()
        if (magic != ACK_MAGIC) throw ProtocolException("Unexpected ACK magic")

        val version = input.readUnsignedByte()
        if (version != VERSION) throw ProtocolException("Unsupported ACK version: $version")

        val accepted = input.readBoolean()
        val receiverElapsedNanos = input.readLong()
        val receiverInfo = readUtf8(input, MAX_DEVICE_INFO_BYTES)
        val payloadLength = input.readInt()
        if (payloadLength !in 1..MAX_PAYLOAD_BYTES) {
            throw ProtocolException("Invalid ACK payload length: $payloadLength")
        }
        val hash = ByteArray(HASH_BYTES)
        input.readFully(hash)

        return AckFrame(
            accepted = accepted,
            receiverElapsedNanos = receiverElapsedNanos,
            receiverInfo = receiverInfo,
            payloadLength = payloadLength,
            sha256 = hash,
        )
    }

    fun sha256(payload: ByteArray): ByteArray =
        MessageDigest.getInstance("SHA-256").digest(payload)

    fun sha256Hex(payload: ByteArray): String =
        sha256(payload).joinToString(separator = "") { "%02x".format(it) }

    fun bytesToHex(bytes: ByteArray): String =
        bytes.joinToString(separator = "") { "%02x".format(it) }

    private fun writeUtf8(output: DataOutputStream, value: String, maxBytes: Int) {
        val bytes = value.toByteArray(StandardCharsets.UTF_8)
        require(bytes.size <= maxBytes) { "UTF-8 field exceeds $maxBytes bytes" }
        output.writeInt(bytes.size)
        output.write(bytes)
    }

    private fun readUtf8(input: DataInputStream, maxBytes: Int): String {
        val length = input.readInt()
        if (length !in 0..maxBytes) throw ProtocolException("Invalid UTF-8 field length: $length")
        val bytes = ByteArray(length)
        input.readFully(bytes)
        return try {
            StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes))
                .toString()
        } catch (error: java.nio.charset.CharacterCodingException) {
            throw ProtocolException("Invalid UTF-8 field")
        }
    }
}

class ProtocolException(message: String) : IOException(message)
