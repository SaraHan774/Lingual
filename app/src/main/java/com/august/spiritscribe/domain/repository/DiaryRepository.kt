package com.august.spiritscribe.domain.repository

import com.august.spiritscribe.domain.model.AppLanguage
import com.august.spiritscribe.domain.model.DiaryEntry
import com.august.spiritscribe.domain.model.Translation
import com.august.spiritscribe.domain.model.WordCard
import kotlinx.coroutines.flow.Flow

interface DiaryRepository {

    fun observeAll(): Flow<List<DiaryEntry>>

    fun observeAllWithTranslations(): Flow<List<Pair<DiaryEntry, List<Translation>>>>

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

    /**
     * 특정 단어 카드의 한 언어 번역값을 사용자 수정본으로 덮어쓴다.
     *
     * 구현은 현재 카드의 `translations` Map 에서 [language] 항목만 교체하고 `isTranslationEdited = true`
     * 로 세팅해 persist 한다. 존재하지 않는 카드 ID 면 no-op.
     *
     * @return 성공 시 수정된 [WordCard], 실패(카드 없음 포함) 시 null.
     */
    suspend fun updateTranslation(
        wordCardId: String,
        language: AppLanguage,
        newTranslation: String,
    ): WordCard?

    suspend fun deleteWordCard(id: String)

    fun observeDueWordCards(now: Long): Flow<List<WordCard>>

    fun targetLanguagesFor(source: AppLanguage): List<AppLanguage> =
        AppLanguage.entries.filter { it != source }
}
