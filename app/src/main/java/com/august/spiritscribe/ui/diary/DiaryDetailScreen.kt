package com.august.spiritscribe.ui.diary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.august.spiritscribe.domain.model.AppLanguage
import com.august.spiritscribe.domain.model.DiaryEntry
import com.august.spiritscribe.domain.model.Translation
import com.august.spiritscribe.domain.model.TranslationStatus
import com.august.spiritscribe.utils.TtsState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiaryDetailScreen(
    onNavigateBack: () -> Unit,
    viewModel: DiaryDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showDeleteDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("일기 상세") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
                    }
                },
                actions = {
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Filled.Delete, contentDescription = "삭제")
                    }
                },
            )
        },
    ) { padding ->
        val entry = state.entry
        if (entry == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }
        } else {
            DetailContent(
                entry = entry,
                translations = state.translations,
                tts = state.tts,
                onSpeak = viewModel::speak,
                onStop = viewModel::stopSpeaking,
                onRetry = viewModel::retryTranslation,
                modifier = Modifier.padding(padding),
            )
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("삭제할까요?") },
            text = { Text("이 일기와 연결된 번역이 모두 삭제됩니다.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    viewModel.deleteEntry(onDeleted = onNavigateBack)
                }) { Text("삭제") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("취소") }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetailContent(
    entry: DiaryEntry,
    translations: List<Translation>,
    tts: TtsState,
    onSpeak: (String, AppLanguage) -> Unit,
    onStop: () -> Unit,
    onRetry: (AppLanguage) -> Unit,
    modifier: Modifier = Modifier,
) {
    val languages = remember(entry.sourceLanguage) {
        buildList {
            add(entry.sourceLanguage)
            AppLanguage.entries.filter { it != entry.sourceLanguage }.forEach { add(it) }
        }
    }
    var selectedIndex by remember { mutableStateOf(0) }
    val selectedLanguage = languages[selectedIndex]

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        if (entry.title.isNotBlank()) {
            Text(
                text = entry.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
        Text(
            text = "원문: ${entry.sourceLanguage.displayName}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(horizontal = 16.dp),
        )

        SecondaryScrollableTabRow(
            selectedTabIndex = selectedIndex,
            modifier = Modifier.padding(top = 12.dp),
        ) {
            languages.forEachIndexed { index, lang ->
                Tab(
                    selected = index == selectedIndex,
                    onClick = { selectedIndex = index },
                    text = {
                        Text(
                            if (lang == entry.sourceLanguage) "${lang.displayName} (원문)"
                            else lang.displayName,
                        )
                    },
                )
            }
        }

        HorizontalDivider()

        LanguagePanel(
            entry = entry,
            selectedLanguage = selectedLanguage,
            translation = translations.firstOrNull { it.targetLanguage == selectedLanguage },
            tts = tts,
            onSpeak = onSpeak,
            onStop = onStop,
            onRetry = onRetry,
        )
    }
}

@Composable
private fun LanguagePanel(
    entry: DiaryEntry,
    selectedLanguage: AppLanguage,
    translation: Translation?,
    tts: TtsState,
    onSpeak: (String, AppLanguage) -> Unit,
    onStop: () -> Unit,
    onRetry: (AppLanguage) -> Unit,
) {
    val isSource = selectedLanguage == entry.sourceLanguage
    val content = if (isSource) entry.content else translation?.translatedContent.orEmpty()
    val isPlayingThis = tts is TtsState.Playing && tts.language == selectedLanguage

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when {
                isSource -> {
                    Text(text = content, style = MaterialTheme.typography.bodyLarge)
                    PlayButton(
                        playing = isPlayingThis,
                        enabled = content.isNotBlank(),
                        onPlay = { onSpeak(content, selectedLanguage) },
                        onStop = onStop,
                    )
                }
                translation == null || translation.status == TranslationStatus.PENDING -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(4.dp),
                            strokeWidth = 2.dp,
                        )
                        Text(
                            text = "번역 중...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                translation.status == TranslationStatus.ERROR -> {
                    Text(
                        text = "번역 실패: ${translation.errorMessage ?: "알 수 없는 오류"}",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    FilledTonalButton(onClick = { onRetry(selectedLanguage) }) {
                        Icon(Icons.Filled.Refresh, contentDescription = null)
                        Text(text = "다시 시도", modifier = Modifier.padding(start = 8.dp))
                    }
                }
                else -> {
                    Text(text = content, style = MaterialTheme.typography.bodyLarge)
                    PlayButton(
                        playing = isPlayingThis,
                        enabled = content.isNotBlank(),
                        onPlay = { onSpeak(content, selectedLanguage) },
                        onStop = onStop,
                    )
                }
            }
        }
    }

    if (tts is TtsState.Error) {
        Text(
            text = tts.message,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun PlayButton(
    playing: Boolean,
    enabled: Boolean,
    onPlay: () -> Unit,
    onStop: () -> Unit,
) {
    FilledTonalButton(
        onClick = if (playing) onStop else onPlay,
        enabled = enabled,
    ) {
        Icon(
            imageVector = if (playing) Icons.Filled.Stop else Icons.Filled.PlayArrow,
            contentDescription = null,
        )
        Text(
            text = if (playing) "정지" else "듣기",
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}
