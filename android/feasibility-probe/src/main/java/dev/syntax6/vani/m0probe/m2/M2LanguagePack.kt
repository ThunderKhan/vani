package dev.syntax6.vani.m0probe.m2

import android.content.Context
import java.io.File
import java.security.MessageDigest

/**
 * Describes a locally installed speech asset. The M2 feasibility build does
 * not download assets; installation is intentionally an explicit future step.
 */
data class M2LanguagePack(
    val language: M2Language,
    val component: Component,
    val version: String,
    val relativeAssetPath: String,
    val expectedSha256: String,
    val licenseId: String,
) {
    enum class Component { ASR, TTS }

    init {
        require(version.isNotBlank()) { "pack version must not be blank" }
        require(relativeAssetPath.isNotBlank() && !relativeAssetPath.startsWith('/')) {
            "asset path must be relative"
        }
        require(expectedSha256.matches(SHA256)) { "expected SHA-256 must be 64 hexadecimal characters" }
        require(licenseId.isNotBlank()) { "license identifier must not be blank" }
    }
}

class M2LanguagePackStore(context: Context) {
    private val root = File(context.filesDir, "m2-language-packs").canonicalFile

    fun locate(pack: M2LanguagePack): File {
        val file = File(root, pack.relativeAssetPath).canonicalFile
        require(file.path == root.path || file.path.startsWith(root.path + File.separator)) {
            "language-pack path escapes the local pack directory"
        }
        return file
    }

    fun status(pack: M2LanguagePack): Status {
        val file = locate(pack)
        if (!file.isFile) return Status.MISSING
        if (file.length() <= 0L) return Status.INVALID
        val actual = sha256(file)
        return if (actual.equals(pack.expectedSha256, ignoreCase = true)) Status.VALID else Status.CORRUPT
    }

    enum class Status { MISSING, INVALID, CORRUPT, VALID }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    companion object {
        private val SHA256 = Regex("^[0-9a-fA-F]{64}$")
    }
}
