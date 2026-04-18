package com.august.spiritscribe.domain.model

import com.august.spiritscribe.data.local.entity.WordCardEntity
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

data class WordCard(
    val id: String,
    val sourceEntryId: String?,
    val word: String,
    val sourceLanguage: AppLanguage,
    val translations: Map<AppLanguage, String>,
    val exampleSentences: Map<AppLanguage, String>,
    val masteryLevel: Int,
    val nextReviewAt: Long?,
    val reviewCount: Int,
    val isFavorite: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
)

private val json = Json { ignoreUnknownKeys = true; isLenient = true }
private val stringMapSerializer = MapSerializer(String.serializer(), String.serializer())

private fun decodeMap(raw: String?): Map<AppLanguage, String> {
    if (raw.isNullOrBlank()) return emptyMap()
    val parsed = runCatching { json.decodeFromString(stringMapSerializer, raw) }.getOrNull().orEmpty()
    return parsed.mapNotNull { (key, value) ->
        AppLanguage.fromCode(key)?.let { it to value }
    }.toMap()
}

private fun encodeMap(map: Map<AppLanguage, String>): String =
    json.encodeToString(stringMapSerializer, map.mapKeys { it.key.code })

fun WordCardEntity.toDomain(): WordCard = WordCard(
    id = id,
    sourceEntryId = sourceEntryId,
    word = word,
    sourceLanguage = AppLanguage.fromCode(sourceLanguage) ?: AppLanguage.KOREAN,
    translations = decodeMap(translationsJson),
    exampleSentences = decodeMap(exampleSentenceJson),
    masteryLevel = masteryLevel,
    nextReviewAt = nextReviewAt,
    reviewCount = reviewCount,
    isFavorite = isFavorite,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun WordCard.toEntity(): WordCardEntity = WordCardEntity(
    id = id,
    sourceEntryId = sourceEntryId,
    word = word,
    sourceLanguage = sourceLanguage.code,
    translationsJson = encodeMap(translations),
    exampleSentenceJson = if (exampleSentences.isEmpty()) null else encodeMap(exampleSentences),
    masteryLevel = masteryLevel,
    nextReviewAt = nextReviewAt,
    reviewCount = reviewCount,
    isFavorite = isFavorite,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
