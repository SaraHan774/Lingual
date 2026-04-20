package com.august.spiritscribe.ui.translate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.august.spiritscribe.domain.model.AppLanguage
import com.august.spiritscribe.domain.model.TranslationStatus
import com.august.spiritscribe.domain.repository.DiaryRepository
import com.august.spiritscribe.ui.diary.DiaryListItem
import com.august.spiritscribe.ui.diary.TranslationSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class TranslateBrowseViewModel @Inject constructor(
    repository: DiaryRepository,
) : ViewModel() {

    private val _selectedLanguage: MutableStateFlow<AppLanguage?> = MutableStateFlow(null)
    val selectedLanguage: StateFlow<AppLanguage?> = _selectedLanguage.asStateFlow()

    private val entries: StateFlow<List<DiaryListItem>> = repository.observeAllWithTranslations()
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
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val filteredEntries: StateFlow<List<DiaryListItem>> = combine(entries, _selectedLanguage) { list, lang ->
        if (lang == null) list else list.filter { it.entry.sourceLanguage == lang }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun selectLanguage(lang: AppLanguage?) {
        _selectedLanguage.value = lang
    }
}
