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
)
