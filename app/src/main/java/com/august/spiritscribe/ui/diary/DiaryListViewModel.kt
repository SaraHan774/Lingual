package com.august.spiritscribe.ui.diary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.august.spiritscribe.domain.model.DiaryEntry
import com.august.spiritscribe.domain.model.TranslationStatus
import com.august.spiritscribe.domain.repository.DiaryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

sealed class TranslationSummary {
    data object Empty : TranslationSummary()
    data class InProgress(val completed: Int, val total: Int) : TranslationSummary()
    data object HasError : TranslationSummary()
    data object AllDone : TranslationSummary()
}

data class DiaryListItem(
    val entry: DiaryEntry,
    val translationSummary: TranslationSummary,
)

@HiltViewModel
class DiaryListViewModel @Inject constructor(
    private val repository: DiaryRepository,
) : ViewModel() {

    val entries: StateFlow<List<DiaryListItem>> = repository.observeAllWithTranslations()
        .map { list ->
            list.map { (entry, translations) ->
                val summary = when {
                    translations.isEmpty() -> TranslationSummary.Empty
                    translations.any { it.status == TranslationStatus.ERROR } -> TranslationSummary.HasError
                    translations.any { it.status == TranslationStatus.PENDING } -> {
                        val done = translations.count { it.status == TranslationStatus.SUCCESS }
                        TranslationSummary.InProgress(done, translations.size)
                    }
                    else -> TranslationSummary.AllDone
                }
                DiaryListItem(entry, summary)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
