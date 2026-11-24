package com.hisaabwise.core.model

import java.util.Locale

/** The six languages the app ships. The raw tag is sent as `Accept-Language`. */
enum class AppLanguage(val tag: String, val endonym: String, val isRightToLeft: Boolean = false) {
    English("en", "English"),
    Arabic("ar", "العربية", isRightToLeft = true),
    Hindi("hi", "हिन्दी"),
    Filipino("fil", "Filipino"),
    Nepali("ne", "नेपाली"),
    Urdu("ur", "اردو", isRightToLeft = true);

    /** Latin digits everywhere, as on iOS (`numberingSystem = latn`). */
    val localeTag: String get() = "$tag-u-nu-latn"

    companion object {
        val shipped: List<AppLanguage> = entries

        fun fromTag(tag: String?): AppLanguage? = entries.firstOrNull { it.tag == tag }

        fun preferring(locales: List<Locale>): AppLanguage? =
            locales.firstNotNullOfOrNull { locale ->
                val lang = when (locale.language) { "tl" -> "fil"; else -> locale.language }
                fromTag(lang)
            }
    }
}
