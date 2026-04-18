package com.august.spiritscribe.data.repository

import com.august.spiritscribe.data.local.dao.DiaryEntryDao
import com.august.spiritscribe.data.local.dao.TranslationDao
import com.august.spiritscribe.data.local.dao.WordCardDao
import com.august.spiritscribe.domain.model.DiaryEntry
import com.august.spiritscribe.domain.model.Translation
import com.august.spiritscribe.domain.model.WordCard
import com.august.spiritscribe.domain.model.toDomain
import com.august.spiritscribe.domain.model.toEntity
import com.august.spiritscribe.domain.repository.DiaryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DiaryRepositoryImpl @Inject constructor(
    private val diaryEntryDao: DiaryEntryDao,
    private val translationDao: TranslationDao,
    private val wordCardDao: WordCardDao,
) : DiaryRepository {

    override fun observeAll(): Flow<List<DiaryEntry>> =
        diaryEntryDao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeAllWithTranslations(): Flow<List<Pair<DiaryEntry, List<Translation>>>> =
        diaryEntryDao.observeAllWithDetails().map { list ->
            list.map { it.entry.toDomain() to it.translations.map { t -> t.toDomain() } }
        }

    override fun observeEntry(id: String): Flow<DiaryEntry?> =
        diaryEntryDao.observeWithDetails(id).map { it?.entry?.toDomain() }

    override fun observeTranslations(entryId: String): Flow<List<Translation>> =
        translationDao.observeForEntry(entryId).map { list -> list.map { it.toDomain() } }

    override suspend fun getEntry(id: String): DiaryEntry? =
        diaryEntryDao.getById(id)?.toDomain()

    override suspend fun createEntry(entry: DiaryEntry) {
        diaryEntryDao.insert(entry.toEntity())
    }

    override suspend fun updateEntry(entry: DiaryEntry) {
        diaryEntryDao.update(entry.toEntity())
    }

    override suspend fun deleteEntry(id: String) {
        diaryEntryDao.deleteById(id)
    }

    override suspend fun upsertTranslation(translation: Translation, modelVersion: String?) {
        translationDao.upsert(translation.toEntity(modelVersion))
    }

    override fun observeAllWordCards(): Flow<List<WordCard>> =
        wordCardDao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeWordCardsForEntry(entryId: String): Flow<List<WordCard>> =
        wordCardDao.observeForEntry(entryId).map { list -> list.map { it.toDomain() } }

    override suspend fun addWordCard(card: WordCard) {
        wordCardDao.insert(card.toEntity())
    }

    override suspend fun updateWordCard(card: WordCard) {
        wordCardDao.update(card.toEntity())
    }

    override suspend fun deleteWordCard(id: String) {
        wordCardDao.deleteById(id)
    }

    override fun observeDueWordCards(now: Long): Flow<List<WordCard>> =
        wordCardDao.observeDueForReview(now).map { list -> list.map { it.toDomain() } }
}
