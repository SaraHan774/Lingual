package com.august.spiritscribe.domain.repository

import com.august.spiritscribe.domain.model.AppLanguage
import com.august.spiritscribe.domain.model.DiaryEntry
import com.august.spiritscribe.domain.model.Translation
import com.august.spiritscribe.domain.model.WordCard
import kotlinx.coroutines.flow.Flow

interface DiaryRepository {

    fun observeAll(): Flow<List<DiaryEntry>>

    fun observeEntry(id: String): Flow<DiaryEntry?>

    fun observeTranslations(entryId: String): Flow<List<Translation>>

    suspend fun getEntry(id: String): DiaryEntry?

    suspend fun createEntry(entry: DiaryEntry)

    suspend fun updateEntry(entry: DiaryEntry)

    suspend fun deleteEntry(id: String)

    suspend fun upsertTranslation(translation: Translation, modelVersion: String?)

    fun observeAllWordCards(): Flow<List<WordCard>>

    fun observeWordCardsForEntry(entryId: String): Flow<List<WordCard>>

    suspend fun addWordCard(card: WordCard)

    suspend fun updateWordCard(card: WordCard)

    suspend fun deleteWordCard(id: String)

    fun observeDueWordCards(now: Long): Flow<List<WordCard>>

    fun targetLanguagesFor(source: AppLanguage): List<AppLanguage> =
        AppLanguage.entries.filter { it != source }
}
