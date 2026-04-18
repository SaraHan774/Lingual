package com.august.spiritscribe.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

data class DiaryEntryWithDetails(
    @Embedded val entry: DiaryEntryEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "diaryEntryId",
    )
    val translations: List<TranslationEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "sourceEntryId",
    )
    val wordCards: List<WordCardEntity>,
)
