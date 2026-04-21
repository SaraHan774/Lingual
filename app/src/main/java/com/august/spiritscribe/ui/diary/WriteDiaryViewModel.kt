package com.august.spiritscribe.ui.diary

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.august.spiritscribe.WriteDiary
import com.august.spiritscribe.data.translation.TranslationEngine
import com.august.spiritscribe.di.ApplicationScope
import com.august.spiritscribe.domain.model.AppLanguage
import com.august.spiritscribe.domain.model.DiaryEntry
import com.august.spiritscribe.domain.model.Translation
import com.august.spiritscribe.domain.model.TranslationStatus
import com.august.spiritscribe.domain.repository.DiaryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
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
    // 편집 모드 여부. entryId 가 non-null 일 때 true. 화면 타이틀 분기 등에 사용.
    val isEditMode: Boolean = false,
    // 편집 모드에서 기존 엔트리 로드 중 여부. 로드 완료 전에는 UI 를 비활성 상태로 표시.
    val isLoading: Boolean = false,
    // 제목/본문/원문 언어 중 하나라도 초기 로드값과 다르면 true. AC-E08 의 취소 다이얼로그 표시 조건.
    val isDirty: Boolean = false,
) {
    val shouldShowCircularProgress: Boolean
        get() = isSaving || isLoading
}

@HiltViewModel
class WriteDiaryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: DiaryRepository,
    private val translationEngine: TranslationEngine,
    @ApplicationScope private val applicationScope: CoroutineScope,
) : ViewModel() {

    // 편집 모드면 non-null, 신규 작성 모드면 null.
    private val entryId: String? = savedStateHandle.toRoute<WriteDiary>().entryId

    // 변경 감지의 단일 소스. ViewModel 에 두어 화면 회전 등 Composition 재생성과 무관하게 유지된다.
    // 신규 작성 모드에서도 비교 자체는 성립하도록 기본값(빈 제목/본문/KOREAN)으로 초기화.
    private var initialTitle: String = ""
    private var initialContent: String = ""
    private var initialSourceLanguage: AppLanguage = AppLanguage.KOREAN

    private val _uiState = MutableStateFlow(
        WriteDiaryUiState(isEditMode = entryId != null, isLoading = entryId != null),
    )
    val uiState: StateFlow<WriteDiaryUiState> = _uiState.asStateFlow()

    // 편집 모드에서 엔트리 로드 실패 시 1회성 이벤트. 화면은 스낵바 표시 후 popBackStack.
    private val _loadErrorEvents = MutableSharedFlow<String>(replay = 0, extraBufferCapacity = 1)
    val loadErrorEvents: SharedFlow<String> = _loadErrorEvents.asSharedFlow()

    init {
        if (entryId != null) loadExistingEntry(entryId)
    }

    private fun loadExistingEntry(id: String) {
        viewModelScope.launch {
            val entry = runCatching { repository.getEntry(id) }.getOrNull()
            if (entry == null) {
                // AC-E10: 로드 실패 시 에러 이벤트 방출 → 화면이 스낵바 표시 후 popBackStack.
                _loadErrorEvents.tryEmit("일기를 불러올 수 없습니다.")
                return@launch
            }
            initialTitle = entry.title
            initialContent = entry.content
            initialSourceLanguage = entry.sourceLanguage
            _uiState.update {
                it.copy(
                    title = entry.title,
                    content = entry.content,
                    sourceLanguage = entry.sourceLanguage,
                    isLoading = false,
                    isDirty = false,
                )
            }
        }
    }

    fun updateTitle(value: String) {
        _uiState.update { it.copy(title = value).withDirtyRecomputed() }
    }

    fun updateContent(value: String) {
        _uiState.update { it.copy(content = value).withDirtyRecomputed() }
    }

    fun updateSourceLanguage(language: AppLanguage) {
        _uiState.update { it.copy(sourceLanguage = language).withDirtyRecomputed() }
    }

    private fun WriteDiaryUiState.withDirtyRecomputed(): WriteDiaryUiState =
        copy(
            isDirty = title != initialTitle ||
                content != initialContent ||
                sourceLanguage != initialSourceLanguage,
        )

    fun save() {
        val snapshot = _uiState.value
        if (snapshot.content.isBlank() || snapshot.isSaving || snapshot.isLoading) return

        if (entryId == null) {
            saveNew(snapshot)
        } else {
            saveEdit(entryId, snapshot)
        }
    }

    private fun saveNew(snapshot: WriteDiaryUiState) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            val now = System.currentTimeMillis()
            val newId = UUID.randomUUID().toString()
            val entry = DiaryEntry(
                id = newId,
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
                resetTranslationsToPending(entry)
            }.onSuccess {
                // PENDING 업서트 완료 직후 saved=true → 화면이 popBackStack.
                // 번역 본작업은 applicationScope 에서 이어진다. viewModelScope 로 띄우면 popBackStack
                // 시 VM 이 clear 되며 코루틴이 취소돼 후속 언어가 PENDING 에 멈춘다.
                _uiState.update { it.copy(isSaving = false, saved = true) }
                applicationScope.launch { translateAll(entry) }
            }.onFailure { e ->
                _uiState.update { it.copy(isSaving = false, error = e.message ?: "저장 실패") }
            }
        }
    }

    private fun saveEdit(id: String, snapshot: WriteDiaryUiState) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }

            // 최신 엔트리를 다시 로드해 createdAt / mood / tags 를 그대로 유지한다.
            val existing = runCatching { repository.getEntry(id) }.getOrNull()
            if (existing == null) {
                _uiState.update { it.copy(isSaving = false, error = "일기를 찾을 수 없습니다.") }
                _loadErrorEvents.tryEmit("일기를 찾을 수 없습니다.")
                return@launch
            }

            val contentChanged = snapshot.content != initialContent
            val sourceLanguageChanged = snapshot.sourceLanguage != initialSourceLanguage
            val needsRetranslate = contentChanged || sourceLanguageChanged

            val now = System.currentTimeMillis()
            val updated = existing.copy(
                title = snapshot.title,
                content = snapshot.content,
                sourceLanguage = snapshot.sourceLanguage,
                updatedAt = now,
                // createdAt 은 불변 (AC-E09).
            )

            runCatching {
                repository.updateEntry(updated)
                if (needsRetranslate) {
                    // AC-E05: 본문 또는 원문 언어 변경 → 전 언어 PENDING 리셋 → 즉시 재번역.
                    resetTranslationsToPending(updated)
                }
            }.onSuccess {
                // popBackStack 은 PENDING upsert 완료 직후(번역 본작업 전)에 실행되어야 한다.
                // 이렇게 해야 상세 화면에서 스피너 → 텍스트 전이가 관찰된다.
                _uiState.update { it.copy(isSaving = false, saved = true) }
                if (needsRetranslate) {
                    // 번역 본작업은 VM 수명에 묶이지 않도록 applicationScope 에서 실행한다.
                    applicationScope.launch { translateAll(updated) }
                }
                // 제목만 변경(AC-E06) 또는 변경 없음(AC-E05 예외): 번역 레코드 불변, updatedAt 만 갱신.
            }.onFailure { e ->
                _uiState.update { it.copy(isSaving = false, error = e.message ?: "저장 실패") }
            }
        }
    }

    /**
     * 원문 언어를 제외한 3개 언어 Translation 을 모두 PENDING 상태로 upsert.
     * 기존 레코드가 있으면 PK 를 재사용해 불필요한 REPLACE(=DELETE+INSERT) 를 피한다.
     * (REPLACE 로도 UNIQUE `(diaryEntryId, targetLanguage)` 덕에 정합성은 유지되지만 id 재사용이 더 명확하다.)
     */
    private suspend fun resetTranslationsToPending(entry: DiaryEntry) {
        val existingByLang = repository.observeTranslations(entry.id).first()
            .associateBy { it.targetLanguage }

        val targets = AppLanguage.entries.filter { it != entry.sourceLanguage }
        targets.forEach { target ->
            val priorId = existingByLang[target]?.id
            val placeholder = Translation(
                id = priorId ?: UUID.randomUUID().toString(),
                diaryEntryId = entry.id,
                targetLanguage = target,
                translatedContent = "",
                status = TranslationStatus.PENDING,
                errorMessage = null,
                translatedAt = System.currentTimeMillis(),
            )
            repository.upsertTranslation(placeholder, translationEngine.modelVersion)
        }
    }

    private suspend fun translateAll(entry: DiaryEntry) {
        val existingByLang = repository.observeTranslations(entry.id).first()
            .associateBy { it.targetLanguage }

        val targets = AppLanguage.entries.filter { it != entry.sourceLanguage }
        targets.forEach { target ->
            val priorId = existingByLang[target]?.id
            val result = translationEngine.translate(entry.content, entry.sourceLanguage, target)
            val updated = Translation(
                id = priorId ?: UUID.randomUUID().toString(),
                diaryEntryId = entry.id,
                targetLanguage = target,
                translatedContent = result.getOrElse { "" },
                status = if (result.isSuccess) TranslationStatus.SUCCESS else TranslationStatus.ERROR,
                errorMessage = result.exceptionOrNull()?.message,
                translatedAt = System.currentTimeMillis(),
            )
            repository.upsertTranslation(updated, translationEngine.modelVersion)
        }
    }
}
