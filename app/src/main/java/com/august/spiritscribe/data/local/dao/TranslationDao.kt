package com.august.spiritscribe.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.august.spiritscribe.data.local.entity.TranslationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TranslationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(translation: TranslationEntity)

    @Update
    suspend fun update(translation: TranslationEntity)

    @Query("DELETE FROM translations WHERE diaryEntryId = :diaryEntryId")
    suspend fun deleteByEntryId(diaryEntryId: String)

    @Query("SELECT * FROM translations WHERE diaryEntryId = :diaryEntryId")
    suspend fun getForEntry(diaryEntryId: String): List<TranslationEntity>

    @Query("SELECT * FROM translations WHERE diaryEntryId = :diaryEntryId")
    fun observeForEntry(diaryEntryId: String): Flow<List<TranslationEntity>>

    @Query(
        """
        SELECT * FROM translations
        WHERE diaryEntryId = :diaryEntryId AND targetLanguage = :targetLanguage
        LIMIT 1
        """
    )
    suspend fun getOne(diaryEntryId: String, targetLanguage: String): TranslationEntity?
}
