package com.august.spiritscribe.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.august.spiritscribe.data.local.dao.DiaryEntryDao
import com.august.spiritscribe.data.local.dao.TranslationDao
import com.august.spiritscribe.data.local.dao.WordCardDao
import com.august.spiritscribe.data.local.entity.DiaryEntryEntity
import com.august.spiritscribe.data.local.entity.TranslationEntity
import com.august.spiritscribe.data.local.entity.WordCardEntity

@Database(
    entities = [
        DiaryEntryEntity::class,
        TranslationEntity::class,
        WordCardEntity::class,
    ],
    // v2 (2026-04-19): WordCardEntity.isTranslationEdited 컬럼 추가.
    // fallbackToDestructiveMigration() 이 활성화돼 있어 기존 DB 는 새 스키마로 재생성된다.
    version = 2,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun diaryEntryDao(): DiaryEntryDao
    abstract fun translationDao(): TranslationDao
    abstract fun wordCardDao(): WordCardDao

    companion object {
        const val DATABASE_NAME = "lingual.db"
    }
}
