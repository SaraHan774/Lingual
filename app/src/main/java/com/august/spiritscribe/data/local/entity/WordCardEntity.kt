package com.august.spiritscribe.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "word_cards",
    foreignKeys = [
        ForeignKey(
            entity = DiaryEntryEntity::class,
            parentColumns = ["id"],
            childColumns = ["sourceEntryId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index("sourceEntryId"),
        Index("sourceLanguage"),
    ],
)
data class WordCardEntity(
    @PrimaryKey
    val id: String,
    val sourceEntryId: String?,
    val word: String,
    val sourceLanguage: String,
    val translationsJson: String,
    val exampleSentenceJson: String?,
    val masteryLevel: Int,
    val nextReviewAt: Long?,
    val reviewCount: Int,
    val isFavorite: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    // 사용자가 번역을 한 번이라도 직접 수정했으면 true. ML Kit 자동 번역과 구분해 UI 인디케이터로 사용.
    // Room exportSchema=false + destructive rebuild 전제이므로 기본값만 두고 마이그레이션은 생략한다.
    val isTranslationEdited: Boolean = false,
)
