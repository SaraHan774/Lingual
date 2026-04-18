package com.august.spiritscribe.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.august.spiritscribe.data.local.entity.WordCardEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WordCardDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(card: WordCardEntity)

    @Update
    suspend fun update(card: WordCardEntity)

    @Query("DELETE FROM word_cards WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM word_cards WHERE id = :id")
    suspend fun getById(id: String): WordCardEntity?

    @Query("SELECT * FROM word_cards ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<WordCardEntity>>

    @Query("SELECT * FROM word_cards WHERE isFavorite = 1 ORDER BY createdAt DESC")
    fun observeFavorites(): Flow<List<WordCardEntity>>

    @Query(
        """
        SELECT * FROM word_cards
        WHERE nextReviewAt IS NOT NULL AND nextReviewAt <= :now
        ORDER BY nextReviewAt ASC
        """
    )
    fun observeDueForReview(now: Long): Flow<List<WordCardEntity>>

    @Query("SELECT * FROM word_cards WHERE sourceEntryId = :entryId")
    fun observeForEntry(entryId: String): Flow<List<WordCardEntity>>
}
