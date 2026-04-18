package com.august.spiritscribe.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.august.spiritscribe.data.local.entity.DiaryEntryEntity
import com.august.spiritscribe.data.local.entity.DiaryEntryWithDetails
import kotlinx.coroutines.flow.Flow

@Dao
interface DiaryEntryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: DiaryEntryEntity)

    @Update
    suspend fun update(entry: DiaryEntryEntity)

    @Query("DELETE FROM diary_entries WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM diary_entries WHERE id = :id")
    suspend fun getById(id: String): DiaryEntryEntity?

    @Transaction
    @Query("SELECT * FROM diary_entries WHERE id = :id")
    suspend fun getWithDetails(id: String): DiaryEntryWithDetails?

    @Transaction
    @Query("SELECT * FROM diary_entries WHERE id = :id")
    fun observeWithDetails(id: String): Flow<DiaryEntryWithDetails?>

    @Query("SELECT * FROM diary_entries ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<DiaryEntryEntity>>

    @Transaction
    @Query("SELECT * FROM diary_entries ORDER BY createdAt DESC")
    fun observeAllWithDetails(): Flow<List<DiaryEntryWithDetails>>

    @Query(
        """
        SELECT * FROM diary_entries
        WHERE content LIKE '%' || :query || '%'
           OR title LIKE '%' || :query || '%'
        ORDER BY createdAt DESC
        """
    )
    fun search(query: String): Flow<List<DiaryEntryEntity>>
}
