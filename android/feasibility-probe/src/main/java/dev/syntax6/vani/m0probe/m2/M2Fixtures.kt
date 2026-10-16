package dev.syntax6.vani.m0probe.m2

/** Small deterministic fixtures for script, numeric, and punctuation coverage. */
data class M2Fixture(
    val language: M2Language,
    val text: String,
    val category: String,
)

object M2Fixtures {
    val all: List<M2Fixture> = listOf(
        M2Fixture(M2Language.HINDI, "सेक्टर 13 में 18:30 बजे प्रवेश न करें।", "number-time-negation"),
        M2Fixture(M2Language.GUJARATI, "સેક્ટર 13 માં 18:30 વાગ્યે પ્રવેશ ન કરો.", "number-time-negation"),
        M2Fixture(M2Language.MARATHI, "सेक्टर 13 मध्ये 18:30 वाजता प्रवेश करू नका.", "number-time-negation"),
        M2Fixture(M2Language.KANNADA, "ಸೆಕ್ಟರ್ 13ಕ್ಕೆ 18:30ಕ್ಕೆ ಪ್ರವೇಶಿಸಬೇಡಿ.", "number-time-negation"),
        M2Fixture(M2Language.MALAYALAM, "സെക്ടർ 13-ൽ 18:30-ന് പ്രവേശിക്കരുത്.", "number-time-negation"),
        M2Fixture(M2Language.TAMIL, "செக்டர் 13-க்கு 18:30 மணிக்கு நுழைய வேண்டாம்.", "number-time-negation"),
        M2Fixture(M2Language.TELUGU, "సెక్టార్ 13లో 18:30 గంటలకు ప్రవేశించవద్దు.", "number-time-negation"),
        M2Fixture(M2Language.ODIA, "ସେକ୍ଟର 13ରେ 18:30ରେ ପ୍ରବେଶ କରନ୍ତୁ ନାହିଁ।", "number-time-negation"),
        M2Fixture(M2Language.BENGALI, "সেক্টর ১৩-তে ১৮:৩০-এ প্রবেশ করবেন না।", "number-time-negation"),
        M2Fixture(M2Language.ENGLISH, "Do not enter Sector 13 before 18:30.", "number-time-negation"),
    )

    fun forLanguage(language: M2Language): List<M2Fixture> = all.filter { it.language == language }
}
