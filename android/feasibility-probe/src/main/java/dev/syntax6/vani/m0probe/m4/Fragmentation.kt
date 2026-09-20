package dev.syntax6.vani.m0probe.m4

class FragmentReassembler(
    private val maxBytes: Int = M4Protocol.MAX_REASSEMBLY_BYTES,
    private val maxFragments: Int = M4Protocol.MAX_FRAGMENT_COUNT,
) {
    private data class Assembly(
        val count: Int,
        val totalBytes: Int,
        val parts: MutableMap<Int, ByteArray> = mutableMapOf(),
        val createdAtMillis: Long,
    )
    private val assemblies = mutableMapOf<String, Assembly>()

    @Synchronized
    fun accept(fragment: M4Protocol.Fragment, nowMillis: Long): Result {
        require(fragment.fragmentCount in 1..maxFragments)
        require(fragment.fragmentIndex in 0 until fragment.fragmentCount)
        require(fragment.totalBytes in 1..maxBytes)
        require(fragment.payload.isNotEmpty() || fragment.totalBytes == 0)
        val assembly = assemblies[fragment.messageId]
        if (assembly == null) {
            assemblies[fragment.messageId] = Assembly(
                fragment.fragmentCount, fragment.totalBytes, mutableMapOf(fragment.fragmentIndex to fragment.payload), nowMillis
            )
            return Result.Incomplete
        }
        require(assembly.count == fragment.fragmentCount) { "conflicting fragment count" }
        require(assembly.totalBytes == fragment.totalBytes) { "conflicting total size" }
        val existing = assembly.parts[fragment.fragmentIndex]
        if (existing != null) {
            require(existing.contentEquals(fragment.payload)) { "conflicting duplicate fragment" }
            return Result.Duplicate
        }
        require(assembly.parts.size < maxFragments)
        assembly.parts[fragment.fragmentIndex] = fragment.payload
        if (assembly.parts.size != assembly.count) return Result.Incomplete
        val output = ByteArray(assembly.parts.values.sumOf { it.size })
        require(output.size == assembly.totalBytes) { "reassembled length mismatch" }
        var offset = 0
        for (i in 0 until assembly.count) {
            val part = assembly.parts[i] ?: return Result.Incomplete
            part.copyInto(output, offset)
            offset += part.size
        }
        assemblies.remove(fragment.messageId)
        return Result.Complete(output)
    }

    @Synchronized
    fun expire(nowMillis: Long, lifetimeMillis: Long): Set<String> {
        val expired = assemblies.filterValues { nowMillis - it.createdAtMillis >= lifetimeMillis }.keys
        expired.forEach(assemblies::remove)
        return expired
    }

    sealed interface Result {
        data object Incomplete : Result
        data object Duplicate : Result
        data class Complete(val bytes: ByteArray) : Result
    }
}

object M4Fragmenter {
    fun split(messageId: String, bytes: ByteArray, maxFragmentPayload: Int): List<M4Protocol.Fragment> {
        require(messageId.isNotBlank())
        require(bytes.isNotEmpty() && bytes.size <= M4Protocol.MAX_REASSEMBLY_BYTES)
        require(maxFragmentPayload > 0)
        val count = (bytes.size + maxFragmentPayload - 1) / maxFragmentPayload
        require(count in 1..M4Protocol.MAX_FRAGMENT_COUNT) { "fragment count exceeds bound" }
        return (0 until count).map { index ->
            val start = index * maxFragmentPayload
            val end = minOf(bytes.size, start + maxFragmentPayload)
            M4Protocol.Fragment(messageId, index, count, bytes.size, bytes.copyOfRange(start, end))
        }
    }
}
