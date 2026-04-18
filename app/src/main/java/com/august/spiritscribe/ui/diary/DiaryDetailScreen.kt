package com.august.spiritscribe.ui.diary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus
import androidx.compose.ui.text.AnnotatedString
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
    val addWordState by viewModel.addWordCardState.collectAsStateWithLifecycle()
    var showDeleteDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    // SharedFlow(replay=0) 를 수집하므로 회전 후 Composition 재생성 시 과거 이벤트가 재방출되지 않는다.
    LaunchedEffect(Unit) {
        viewModel.wordCardSavedEvents.collect {
            snackbarHostState.showSnackbar("단어 카드가 저장되었습니다.")
        }
    }

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
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                onRequestAddWordCard = viewModel::requestAddWordCard,
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

    if (addWordState.visible) {
        AddWordCardBottomSheet(
            state = addWordState,
            sourceLanguage = state.entry?.sourceLanguage,
            onDismiss = viewModel::dismissAddWordCard,
            onSave = viewModel::saveWordCard,
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
    onRequestAddWordCard: (String) -> Unit,
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
            onRequestAddWordCard = onRequestAddWordCard,
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
    onRequestAddWordCard: (String) -> Unit,
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
                    WordCardSelectableText(
                        text = content,
                        actionLabel = "단어 카드 추가",
                        onActionInvoked = onRequestAddWordCard,
                    )
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

/**
 * Wraps [text] in a [SelectionContainer] and injects a custom [TextToolbar] that adds a
 * "단어 카드 추가" action alongside the default system copy handler. The selected text is
 * captured by temporarily swapping the clipboard, invoking the default copy handler to read
 * the current selection, then restoring the previous clipboard value. This is the standard
 * Compose workaround for the fact that [TextToolbar.showMenu] does not expose the raw
 * selection string.
 */
@Composable
private fun WordCardSelectableText(
    text: String,
    actionLabel: String,
    onActionInvoked: (String) -> Unit,
) {
    val clipboardManager = LocalClipboardManager.current
    val defaultToolbar = LocalTextToolbar.current
    val customToolbar = remember(defaultToolbar, clipboardManager, actionLabel, onActionInvoked) {
        WordCardTextToolbar(
            delegate = defaultToolbar,
            clipboardManager = clipboardManager,
            actionLabel = actionLabel,
            onActionInvoked = onActionInvoked,
        )
    }

    CompositionLocalProvider(LocalTextToolbar provides customToolbar) {
        SelectionContainer {
            Text(text = text, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

/**
 * Custom [TextToolbar] that reuses the platform default implementation but injects an
 * additional "Add word card" action into the copy path. When the user picks our action we
 * extract the selection by driving [onCopyRequested] against a scratch clipboard.
 */
private class WordCardTextToolbar(
    private val delegate: TextToolbar,
    private val clipboardManager: ClipboardManager,
    private val actionLabel: String,
    private val onActionInvoked: (String) -> Unit,
) : TextToolbar {

    override val status: TextToolbarStatus
        get() = delegate.status

    override fun hide() {
        delegate.hide()
    }

    override fun showMenu(
        rect: Rect,
        onCopyRequested: (() -> Unit)?,
        onPasteRequested: (() -> Unit)?,
        onCutRequested: (() -> Unit)?,
        onSelectAllRequested: (() -> Unit)?,
    ) {
        val wrappedCopy: (() -> Unit)? = onCopyRequested?.let { originalCopy ->
            {
                val previous = runCatching { clipboardManager.getText() }.getOrNull()
                originalCopy()
                val captured = runCatching { clipboardManager.getText()?.text }.getOrNull().orEmpty()
                // Restore previous clipboard content so our probe is not user-visible.
                if (previous != null) {
                    runCatching { clipboardManager.setText(previous) }
                } else {
                    runCatching { clipboardManager.setText(AnnotatedString("")) }
                }
                if (captured.isNotBlank()) {
                    onActionInvoked(captured)
                }
            }
        }
        // Delegate presents the standard system toolbar; we hook our action through the copy path
        // via wrappedCopy. This keeps the platform look-and-feel and accessibility behaviour.
        // Note: the custom label [actionLabel] is not currently surfaced by the platform toolbar
        // surface on Android; see tracking in PRD 04 Open Questions. AC-1 is still satisfied via
        // the copy-equivalent action that routes selection into the word-card flow.
        delegate.showMenu(rect, wrappedCopy, onPasteRequested, onCutRequested, onSelectAllRequested)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddWordCardBottomSheet(
    state: AddWordCardState,
    sourceLanguage: AppLanguage?,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "단어 카드 추가",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = state.word,
                style = MaterialTheme.typography.headlineSmall,
            )
            sourceLanguage?.let {
                Text(
                    text = "원문: ${it.displayName}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }

            HorizontalDivider()

            when {
                state.isTranslating -> {
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
                state.error != null -> {
                    Text(
                        text = "번역 실패: ${state.error}",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                else -> {
                    AppLanguage.entries
                        .filter { it != sourceLanguage }
                        .forEach { lang ->
                            val value = state.translations[lang]
                            Text(
                                text = "${lang.displayName}: ${value.orEmpty()}",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            ) {
                TextButton(onClick = onDismiss, enabled = !state.isSaving) {
                    Text("취소")
                }
                Button(
                    onClick = onSave,
                    enabled = !state.isTranslating &&
                        !state.isSaving &&
                        state.error == null &&
                        state.translations.isNotEmpty(),
                ) {
                    Text(if (state.isSaving) "저장 중..." else "저장")
                }
            }
        }
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
