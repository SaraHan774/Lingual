package com.august.spiritscribe.ui.diary

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.august.spiritscribe.DiaryDetail
import com.august.spiritscribe.data.translation.TranslationEngine
import com.august.spiritscribe.domain.model.AppLanguage
import com.august.spiritscribe.domain.model.DiaryEntry
import com.august.spiritscribe.domain.model.Translation
import com.august.spiritscribe.domain.model.TranslationStatus
import com.august.spiritscribe.domain.model.WordCard
import com.august.spiritscribe.domain.repository.DiaryRepository
import com.august.spiritscribe.utils.TtsService
import com.august.spiritscribe.utils.TtsState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import androidx.navigation.toRoute
import java.util.UUID
import javax.inject.Inject

data class DiaryDetailUiState(
    val entry: DiaryEntry? = null,
    val translations: List<Translation> = emptyList(),
    val tts: TtsState = TtsState.Idle,
)

data class AddWordCardState(
    val visible: Boolean = false,
    val word: String = "",
    val isTranslating: Boolean = false,
    val translations: Map<AppLanguage, String> = emptyMap(),
    val error: String? = null,
    val isSaving: Boolean = false,
)

@HiltViewModel
class DiaryDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: DiaryRepository,
    private val translationEngine: TranslationEngine,
    private val ttsService: TtsService,
) : ViewModel() {

    private val entryId: String = savedStateHandle.toRoute<DiaryDetail>().id

    val uiState: StateFlow<DiaryDetailUiState> = combine(
        repository.observeEntry(entryId),
        repository.observeTranslations(entryId),
        ttsService.state,
    ) { entry, translations, tts ->
        DiaryDetailUiState(entry = entry, translations = translations, tts = tts)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DiaryDetailUiState())

    private val _addWordCardState = MutableStateFlow(AddWordCardState())
    val addWordCardState: StateFlow<AddWordCardState> = _addWordCardState.asStateFlow()

    // Fire-and-forget 이벤트. SharedFlow(replay=0) 을 써서 화면 회전 등 Composition 재생성 시
    // 마지막 이벤트가 재방출되지 않도록 한다.
    private val _wordCardSavedEvents = MutableSharedFlow<Unit>(replay = 0, extraBufferCapacity = 1)
    val wordCardSavedEvents: SharedFlow<Unit> = _wordCardSavedEvents.asSharedFlow()

    fun speak(text: String, language: AppLanguage) {
        if (text.isBlank()) return
        ttsService.speak(text, language)
    }

    fun stopSpeaking() {
        ttsService.stop()
    }

    fun retryTranslation(targetLanguage: AppLanguage) {
        val snapshot = uiState.value
        val entry = snapshot.entry ?: return
        viewModelScope.launch {
            val existing = snapshot.translations.firstOrNull { it.targetLanguage == targetLanguage }
            val placeholder = (existing ?: Translation(
                id = UUID.randomUUID().toString(),
                diaryEntryId = entry.id,
                targetLanguage = targetLanguage,
                translatedContent = "",
                status = TranslationStatus.PENDING,
                errorMessage = null,
                translatedAt = System.currentTimeMillis(),
            )).copy(status = TranslationStatus.PENDING, errorMessage = null)
            repository.upsertTranslation(placeholder, translationEngine.modelVersion)

            val result = translationEngine.translate(entry.content, entry.sourceLanguage, targetLanguage)
            val updated = placeholder.copy(
                translatedContent = result.getOrElse { "" },
                status = if (result.isSuccess) TranslationStatus.SUCCESS else TranslationStatus.ERROR,
                errorMessage = result.exceptionOrNull()?.message,
                translatedAt = System.currentTimeMillis(),
            )
            repository.upsertTranslation(updated, translationEngine.modelVersion)
        }
    }

    fun deleteEntry(onDeleted: () -> Unit) {
        viewModelScope.launch {
            repository.deleteEntry(entryId)
            onDeleted()
        }
    }

    fun requestAddWordCard(rawWord: String) {
        val word = rawWord.trim()
        if (word.isBlank()) return
        // 구두점/기호만 있고 문자(Letter)가 전혀 없으면 단어 카드로 취급하지 않는다.
        if (word.none { it.isLetter() }) return
        val entry = uiState.value.entry ?: return

        _addWordCardState.value = AddWordCardState(
            visible = true,
            word = word,
            isTranslating = true,
            translations = emptyMap(),
            error = null,
        )

        viewModelScope.launch {
            runCatching { translateWordToAllTargets(word, entry.sourceLanguage) }
                .onSuccess { translations ->
                    _addWordCardState.update {
                        it.copy(isTranslating = false, translations = translations, error = null)
                    }
                }
                .onFailure { e ->
                    _addWordCardState.update {
                        it.copy(
                            isTranslating = false,
                            translations = emptyMap(),
                            error = e.message ?: "번역 실패",
                        )
                    }
                }
        }
    }

    fun dismissAddWordCard() {
        _addWordCardState.value = AddWordCardState()
    }

    fun saveWordCard() {
        val snapshot = _addWordCardState.value
        val entry = uiState.value.entry ?: return
        if (!snapshot.visible || snapshot.isTranslating || snapshot.isSaving) return
        if (snapshot.translations.isEmpty()) return

        viewModelScope.launch {
            _addWordCardState.update { it.copy(isSaving = true) }
            val now = System.currentTimeMillis()
            val card = WordCard(
                id = UUID.randomUUID().toString(),
                sourceEntryId = entry.id,
                word = snapshot.word,
                sourceLanguage = entry.sourceLanguage,
                translations = snapshot.translations,
                exampleSentences = emptyMap(),
                masteryLevel = 0,
                nextReviewAt = null,
                reviewCount = 0,
                isFavorite = false,
                createdAt = now,
                updatedAt = now,
            )
            runCatching { repository.addWordCard(card) }
                .onSuccess {
                    _addWordCardState.value = AddWordCardState()
                    _wordCardSavedEvents.tryEmit(Unit)
                }
                .onFailure { e ->
                    _addWordCardState.update {
                        it.copy(isSaving = false, error = e.message ?: "저장 실패")
                    }
                }
        }
    }

    private suspend fun translateWordToAllTargets(
        word: String,
        source: AppLanguage,
    ): Map<AppLanguage, String> = coroutineScope {
        val targets = AppLanguage.entries.filter { it != source }
        val deferred = targets.map { target ->
            async {
                target to translationEngine.translate(word, source, target)
                    .getOrThrow()
            }
        }
        deferred.awaitAll().toMap()
    }
}
