package com.august.spiritscribe.ui.flashcard

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.august.spiritscribe.BuildConfig
import com.august.spiritscribe.domain.model.AppLanguage
import com.august.spiritscribe.domain.model.WordCard
import com.august.spiritscribe.domain.repository.DiaryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

// AC-9/AC-24: "최근 추가 (7일 내)" 필터를 위해 RecentlyAdded 케이스 추가. 순서는 UI 노출 순서
// (전체 → 즐겨찾기 → 복습 예정 → 최근 추가) 와 일치시켜 유지한다.
enum class FlashCardFilter { All, Favorites, DueForReview, RecentlyAdded }

// AC-24: 롤링 168시간 윈도우. 현지 자정 경계가 아닌 UTC epoch millis 기준.
private const val SEVEN_DAYS_MS = 7L * 24 * 60 * 60 * 1000L

data class FlashCardStats(
    val total: Int,
    val favorites: Int,
    val dueToday: Int,
)

@HiltViewModel
class FlashCardViewModel @Inject constructor(
    private val repository: DiaryRepository,
) : ViewModel() {

    val cards: StateFlow<List<WordCard>> = repository.observeAllWordCards()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _filterState = MutableStateFlow(FlashCardFilter.All)
    val filterState: StateFlow<FlashCardFilter> = _filterState.asStateFlow()

    val filteredCards: StateFlow<List<WordCard>> = combine(cards, _filterState) { list, filter ->
        // AC-24: now 는 Flow 재방출 시점(combine 람다 실행 시점)의 System.currentTimeMillis().
        // 즐겨찾기 토글/숙련도 선택 등으로 Flow 가 재방출될 때마다 경계가 재평가된다.
        val now = System.currentTimeMillis()
        val result = when (filter) {
            FlashCardFilter.All -> list
            FlashCardFilter.Favorites -> list.filter { it.isFavorite }
            FlashCardFilter.DueForReview -> list.filter { card ->
                val due = card.nextReviewAt
                due != null && due <= now
            }
            FlashCardFilter.RecentlyAdded -> {
                val sevenDaysAgo = now - SEVEN_DAYS_MS
                list.filter { it.createdAt >= sevenDaysAgo }
            }
        }
        if (BuildConfig.DEBUG && filter == FlashCardFilter.RecentlyAdded) {
            Log.d("QA", "filter:RecentlyAdded results=${result.size}")
        }
        result
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val stats: StateFlow<FlashCardStats> = cards.map { list ->
        val now = System.currentTimeMillis()
        FlashCardStats(
            total = list.size,
            favorites = list.count { it.isFavorite },
            dueToday = list.count { card ->
                val due = card.nextReviewAt
                due != null && due <= now
            },
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        FlashCardStats(0, 0, 0),
    )

    // 번역 수정 성공/실패 one-shot 이벤트. SharedFlow(replay=0) 로 Composition 재생성 시
    // 과거 이벤트가 재방출되지 않도록 한다 (CLAUDE.md one-shot 이벤트 규칙).
    private val _translationEditSucceeded = MutableSharedFlow<Unit>(replay = 0, extraBufferCapacity = 1)
    val translationEditSucceeded: SharedFlow<Unit> = _translationEditSucceeded.asSharedFlow()

    private val _translationEditFailed = MutableSharedFlow<Unit>(replay = 0, extraBufferCapacity = 1)
    val translationEditFailed: SharedFlow<Unit> = _translationEditFailed.asSharedFlow()

    fun setFilter(filter: FlashCardFilter) {
        _filterState.value = filter
        if (BuildConfig.DEBUG && filter == FlashCardFilter.RecentlyAdded) {
            Log.d("QA", "filter:RecentlyAdded selected")
        }
    }

    /**
     * 사용자가 BottomSheet 에서 저장한 번역을 영속화한다.
     *
     * 성공 시 [translationEditSucceeded] 방출 → UI 가 스낵바 + 시트 닫기. 실패 시 [translationEditFailed]
     * 방출 → UI 가 에러 스낵바 + 시트 유지 (AC-19).
     */
    fun updateTranslation(
        wordCardId: String,
        language: AppLanguage,
        newTranslation: String,
    ) {
        viewModelScope.launch {
            val result = runCatching {
                repository.updateTranslation(wordCardId, language, newTranslation)
            }
            val updated = result.getOrNull()
            if (result.isSuccess && updated != null) {
                _translationEditSucceeded.tryEmit(Unit)
            } else {
                _translationEditFailed.tryEmit(Unit)
            }
        }
    }

    fun updateMastery(card: WordCard, newLevel: Int) {
        viewModelScope.launch {
            val coerced = newLevel.coerceIn(0, 3)
            repository.updateWordCard(
                card.copy(
                    masteryLevel = coerced,
                    reviewCount = card.reviewCount + 1,
                    nextReviewAt = computeNextReview(coerced),
                    updatedAt = System.currentTimeMillis(),
                ),
            )
        }
    }

    fun toggleFavorite(card: WordCard) {
        viewModelScope.launch {
            repository.updateWordCard(
                card.copy(
                    isFavorite = !card.isFavorite,
                    updatedAt = System.currentTimeMillis(),
                ),
            )
        }
    }

    fun deleteCard(id: String) {
        viewModelScope.launch {
            repository.deleteWordCard(id)
        }
    }

    // AC-16: masteryLevel별 고정 간격으로 nextReviewAt 설정. Phase 2에서 SM-2 lite로 대체 예정.
    private fun computeNextReview(level: Int): Long {
        val now = System.currentTimeMillis()
        val dayMs = 24L * 60 * 60 * 1000
        val intervalDays = when (level) {
            0 -> 1L
            1 -> 3L
            2 -> 7L
            else -> 30L
        }
        return now + intervalDays * dayMs
    }
}
