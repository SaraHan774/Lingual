package com.august.spiritscribe.data.translation

import com.august.spiritscribe.domain.model.AppLanguage

interface TranslationEngine {

    val modelVersion: String

    suspend fun translate(text: String, from: AppLanguage, to: AppLanguage): Result<String>

    suspend fun prepareLanguage(language: AppLanguage): Result<Unit>

    suspend fun prepareLanguages(languages: Collection<AppLanguage>): Result<Unit> {
        languages.forEach { lang ->
            val result = prepareLanguage(lang)
            if (result.isFailure) return result
        }
        return Result.success(Unit)
    }
}
