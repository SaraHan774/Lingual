package com.august.spiritscribe.ui.diary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.august.spiritscribe.domain.model.DiaryEntry
import com.august.spiritscribe.domain.repository.DiaryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class DiaryListViewModel @Inject constructor(
    private val repository: DiaryRepository,
) : ViewModel() {

    val entries: StateFlow<List<DiaryEntry>> = repository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
