package dev.syntax6.vani.m0probe.m2

/**
 * The ten languages named by SIH26173. Locale tags are the canonical identifiers
 * used by the Android speech APIs; they are kept explicit so support is never
 * inferred from a generic "multilingual" label.
 */
enum class M2Language(
    val languageCode: String,
    val localeTag: String,
    val displayName: String,
    val nativeName: String,
) {
    HINDI("hi", "hi-IN", "Hindi", "हिन्दी"),
    GUJARATI("gu", "gu-IN", "Gujarati", "ગુજરાતી"),
    MARATHI("mr", "mr-IN", "Marathi", "मराठी"),
    KANNADA("kn", "kn-IN", "Kannada", "ಕನ್ನಡ"),
    MALAYALAM("ml", "ml-IN", "Malayalam", "മലയാളം"),
    TAMIL("ta", "ta-IN", "Tamil", "தமிழ்"),
    TELUGU("te", "te-IN", "Telugu", "తెలుగు"),
    ODIA("or", "or-IN", "Odia", "ଓଡ଼ିଆ"),
    BENGALI("bn", "bn-IN", "Bengali", "বাংলা"),
    ENGLISH("en", "en-IN", "English", "English"),
    ;

    companion object {
        fun fromLocaleTag(tag: String): M2Language? =
            entries.firstOrNull { it.localeTag.equals(tag.trim(), ignoreCase = true) }

        fun fromLanguageCode(code: String): M2Language? =
            entries.firstOrNull { it.languageCode.equals(code.trim(), ignoreCase = true) }
    }
}

/** A language path is deliberately explicit about what has and has not been verified. */
data class M2LanguagePath(
    val language: M2Language,
    val asr: M2ComponentStatus,
    val tts: M2ComponentStatus,
)

enum class M2ComponentStatus {
    NOT_CHECKED,
    AVAILABLE,
    UNAVAILABLE,
    REQUIRES_DEVICE_DATA,
    VERIFICATION_FAILED,
}
