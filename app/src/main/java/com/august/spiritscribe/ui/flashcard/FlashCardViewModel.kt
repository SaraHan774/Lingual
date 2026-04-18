package com.august.spiritscribe.ui.flashcard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.august.spiritscribe.domain.model.WordCard
import com.august.spiritscribe.domain.repository.DiaryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FlashCardViewModel @Inject constructor(
    private val repository: DiaryRepository,
) : ViewModel() {

    val cards: StateFlow<List<WordCard>> = repository.observeAllWordCards()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun updateMastery(card: WordCard, newLevel: Int) {
        viewModelScope.launch {
            repository.updateWordCard(
                card.copy(
                    masteryLevel = newLevel.coerceIn(0, 3),
                    reviewCount = card.reviewCount + 1,
                    nextReviewAt = computeNextReview(newLevel),
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

    private fun computeNextReview(level: Int): Long {
        val now = System.currentTimeMillis()
        val dayMs = 24L * 60 * 60 * 1000
        return when (level) {
            0 -> now + dayMs
            1 -> now + 3 * dayMs
            2 -> now + 7 * dayMs
            else -> now + 14 * dayMs
        }
    }
}
