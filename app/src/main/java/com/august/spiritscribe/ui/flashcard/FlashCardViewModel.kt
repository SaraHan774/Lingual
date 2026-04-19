package com.august.spiritscribe.ui.flashcard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.august.spiritscribe.domain.model.WordCard
import com.august.spiritscribe.domain.repository.DiaryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class FlashCardFilter { All, Favorites, DueForReview }

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
        val now = System.currentTimeMillis()
        when (filter) {
            FlashCardFilter.All -> list
            FlashCardFilter.Favorites -> list.filter { it.isFavorite }
            FlashCardFilter.DueForReview -> list.filter { card ->
                val due = card.nextReviewAt
                due != null && due <= now
            }
        }
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

    fun setFilter(filter: FlashCardFilter) {
        _filterState.value = filter
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
