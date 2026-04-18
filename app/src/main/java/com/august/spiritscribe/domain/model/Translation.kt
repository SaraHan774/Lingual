package com.august.spiritscribe.domain.model

import com.august.spiritscribe.data.local.entity.TranslationEntity

enum class TranslationStatus { PENDING, SUCCESS, ERROR }

data class Translation(
    val id: String,
    val diaryEntryId: String,
    val targetLanguage: AppLanguage,
    val translatedContent: String,
    val status: TranslationStatus,
    val errorMessage: String?,
    val translatedAt: Long,
)

fun TranslationEntity.toDomain(): Translation = Translation(
    id = id,
    diaryEntryId = diaryEntryId,
    targetLanguage = AppLanguage.fromCode(targetLanguage) ?: AppLanguage.ENGLISH,
    translatedContent = translatedContent,
    status = when (translationStatus) {
        "success" -> TranslationStatus.SUCCESS
        "error" -> TranslationStatus.ERROR
        else -> TranslationStatus.PENDING
    },
    errorMessage = errorMessage,
    translatedAt = translatedAt,
)

fun Translation.toEntity(modelVersion: String? = null): TranslationEntity = TranslationEntity(
    id = id,
    diaryEntryId = diaryEntryId,
    targetLanguage = targetLanguage.code,
    translatedContent = translatedContent,
    translationStatus = when (status) {
        TranslationStatus.SUCCESS -> "success"
        TranslationStatus.ERROR -> "error"
        TranslationStatus.PENDING -> "pending"
    },
    errorMessage = errorMessage,
    translatedAt = translatedAt,
    modelVersion = modelVersion,
)
