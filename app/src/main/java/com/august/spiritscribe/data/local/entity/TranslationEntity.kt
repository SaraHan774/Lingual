package com.august.spiritscribe.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "translations",
    foreignKeys = [
        ForeignKey(
            entity = DiaryEntryEntity::class,
            parentColumns = ["id"],
            childColumns = ["diaryEntryId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["diaryEntryId", "targetLanguage"], unique = true),
    ],
)
data class TranslationEntity(
    @PrimaryKey
    val id: String,
    val diaryEntryId: String,
    val targetLanguage: String,
    val translatedContent: String,
    val translationStatus: String,
    val errorMessage: String?,
    val translatedAt: Long,
    val modelVersion: String?,
)
