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
import com.august.spiritscribe.domain.repository.DiaryRepository
import com.august.spiritscribe.utils.TtsService
import com.august.spiritscribe.utils.TtsState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import androidx.navigation.toRoute
import java.util.UUID
import javax.inject.Inject

data class DiaryDetailUiState(
    val entry: DiaryEntry? = null,
    val translations: List<Translation> = emptyList(),
    val tts: TtsState = TtsState.Idle,
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
}
