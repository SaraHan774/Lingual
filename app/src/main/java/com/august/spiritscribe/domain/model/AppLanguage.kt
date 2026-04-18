package com.august.spiritscribe.domain.model

import java.util.Locale

enum class AppLanguage(
    val code: String,
    val displayName: String,
) {
    KOREAN("ko", "한국어"),
    ENGLISH("en", "English"),
    JAPANESE("ja", "日本語"),
    CHINESE("zh", "中文");

    fun toLocale(): Locale = when (this) {
        KOREAN -> Locale.KOREAN
        ENGLISH -> Locale.US
        JAPANESE -> Locale.JAPANESE
        CHINESE -> Locale.SIMPLIFIED_CHINESE
    }

    companion object {
        fun fromCode(code: String): AppLanguage? = entries.firstOrNull { it.code == code }

        val targetsFor: (AppLanguage) -> List<AppLanguage> = { source ->
            entries.filter { it != source }
        }
    }
}
