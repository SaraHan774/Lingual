package com.august.spiritscribe.domain.model

import com.august.spiritscribe.data.local.entity.DiaryEntryEntity
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

data class DiaryEntry(
    val id: String,
    val title: String,
    val content: String,
    val sourceLanguage: AppLanguage,
    val mood: String?,
    val tags: List<String>,
    val createdAt: Long,
    val updatedAt: Long,
)

private val json = Json { ignoreUnknownKeys = true; isLenient = true }
private val stringListSerializer = ListSerializer(String.serializer())

fun DiaryEntryEntity.toDomain(): DiaryEntry = DiaryEntry(
    id = id,
    title = title,
    content = content,
    sourceLanguage = AppLanguage.fromCode(sourceLanguage) ?: AppLanguage.KOREAN,
    mood = mood,
    tags = runCatching { json.decodeFromString(stringListSerializer, tags) }.getOrDefault(emptyList()),
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun DiaryEntry.toEntity(): DiaryEntryEntity = DiaryEntryEntity(
    id = id,
    title = title,
    content = content,
    sourceLanguage = sourceLanguage.code,
    mood = mood,
    tags = json.encodeToString(stringListSerializer, tags),
    createdAt = createdAt,
    updatedAt = updatedAt,
)
