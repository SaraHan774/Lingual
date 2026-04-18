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
    version = 1,
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
