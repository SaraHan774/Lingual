package com.august.spiritscribe.ui.diary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.august.spiritscribe.data.translation.TranslationEngine
import com.august.spiritscribe.domain.model.AppLanguage
import com.august.spiritscribe.domain.model.DiaryEntry
import com.august.spiritscribe.domain.model.Translation
import com.august.spiritscribe.domain.model.TranslationStatus
import com.august.spiritscribe.domain.repository.DiaryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class WriteDiaryUiState(
    val title: String = "",
    val content: String = "",
    val sourceLanguage: AppLanguage = AppLanguage.KOREAN,
    val isSaving: Boolean = false,
    val saved: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class WriteDiaryViewModel @Inject constructor(
    private val repository: DiaryRepository,
    private val translationEngine: TranslationEngine,
) : ViewModel() {

    private val _uiState = MutableStateFlow(WriteDiaryUiState())
    val uiState: StateFlow<WriteDiaryUiState> = _uiState.asStateFlow()

    fun updateTitle(value: String) {
        _uiState.update { it.copy(title = value) }
    }

    fun updateContent(value: String) {
        _uiState.update { it.copy(content = value) }
    }

    fun updateSourceLanguage(language: AppLanguage) {
        _uiState.update { it.copy(sourceLanguage = language) }
    }

    fun save() {
        val snapshot = _uiState.value
        if (snapshot.content.isBlank() || snapshot.isSaving) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            val now = System.currentTimeMillis()
            val entryId = UUID.randomUUID().toString()
            val entry = DiaryEntry(
                id = entryId,
                title = snapshot.title,
                content = snapshot.content,
                sourceLanguage = snapshot.sourceLanguage,
                mood = null,
                tags = emptyList(),
                createdAt = now,
                updatedAt = now,
            )

            runCatching {
                repository.createEntry(entry)
                translateAll(entry)
            }.onSuccess {
                _uiState.update { it.copy(isSaving = false, saved = true) }
            }.onFailure { e ->
                _uiState.update { it.copy(isSaving = false, error = e.message ?: "저장 실패") }
            }
        }
    }

    private suspend fun translateAll(entry: DiaryEntry) {
        val targets = AppLanguage.entries.filter { it != entry.sourceLanguage }
        targets.forEach { target ->
            val placeholder = Translation(
                id = UUID.randomUUID().toString(),
                diaryEntryId = entry.id,
                targetLanguage = target,
                translatedContent = "",
                status = TranslationStatus.PENDING,
                errorMessage = null,
                translatedAt = System.currentTimeMillis(),
            )
            repository.upsertTranslation(placeholder, translationEngine.modelVersion)

            val result = translationEngine.translate(entry.content, entry.sourceLanguage, target)
            val updated = placeholder.copy(
                translatedContent = result.getOrElse { "" },
                status = if (result.isSuccess) TranslationStatus.SUCCESS else TranslationStatus.ERROR,
                errorMessage = result.exceptionOrNull()?.message,
                translatedAt = System.currentTimeMillis(),
            )
            repository.upsertTranslation(updated, translationEngine.modelVersion)
        }
    }
}
