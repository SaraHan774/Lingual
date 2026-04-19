package com.august.spiritscribe.ui.diary

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.BookmarkAdd
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
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
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
    onEditClick: (String) -> Unit,
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
                    // AC-E01: 편집 아이콘. 엔트리 로드 전에는 비활성(id 가 없으므로 라우팅 불가).
                    val currentEntryId = state.entry?.id
                    IconButton(
                        onClick = { currentEntryId?.let(onEditClick) },
                        enabled = currentEntryId != null,
                    ) {
                        Icon(Icons.Filled.Edit, contentDescription = "편집")
                    }
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
                onOpenAddWordCard = viewModel::openAddWordCard,
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
            onWordChange = viewModel::updateWordInput,
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
    onOpenAddWordCard: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val languages = remember(entry.sourceLanguage) {
        buildList {
            add(entry.sourceLanguage)
            AppLanguage.entries.filter { it != entry.sourceLanguage }.forEach { add(it) }
        }
    }
    val translationsByLang = remember(translations) {
        translations.associateBy { it.targetLanguage }
    }
    var selectedIndex by rememberSaveable { mutableStateOf(0) }
    val selectedLanguage = languages[selectedIndex]

    val isSourceSelected = selectedLanguage == entry.sourceLanguage

    // 탭 행과 BookmarkAdd 아이콘은 스크롤 밖 고정 영역에 둔다(AC-1).
    // 본문(LanguagePanel) 만 verticalScroll 로 스크롤된다.
    Column(modifier = modifier.fillMaxSize()) {
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

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SecondaryScrollableTabRow(
                selectedTabIndex = selectedIndex,
                modifier = Modifier.weight(1f),
            ) {
                languages.forEachIndexed { index, lang ->
                    val isSource = lang == entry.sourceLanguage
                    val translationStatus = if (isSource) {
                        null
                    } else {
                        translationsByLang[lang]?.status
                    }
                    Tab(
                        selected = index == selectedIndex,
                        onClick = { selectedIndex = index },
                        text = {
                            TabLabel(
                                label = if (isSource) "${lang.displayName} (원문)" else lang.displayName,
                                isSource = isSource,
                                status = translationStatus,
                            )
                        },
                    )
                }
            }
            // 원문 탭 활성 시에만 BookmarkAdd 버튼 노출(AC-1). 탭 행과 같은 고정 Row 안에
            // 있으므로 본문 길이와 무관하게 항상 보인다.
            AnimatedVisibility(
                visible = isSourceSelected,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                IconButton(onClick = onOpenAddWordCard) {
                    Icon(
                        imageVector = Icons.Outlined.BookmarkAdd,
                        contentDescription = "단어 카드 추가",
                    )
                }
            }
        }

        HorizontalDivider()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            LanguagePanel(
                entry = entry,
                selectedLanguage = selectedLanguage,
                translation = translationsByLang[selectedLanguage],
                tts = tts,
                onSpeak = onSpeak,
                onStop = onStop,
                onRetry = onRetry,
            )
        }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddWordCardBottomSheet(
    state: AddWordCardState,
    sourceLanguage: AppLanguage?,
    onWordChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val focusRequester = remember { FocusRequester() }
    // 시트 콘텐츠가 합성된 직후 TextField 에 포커스를 주어 키보드를 자동 오픈한다(AC-2).
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    val trimmedWord = state.word.trim()
    val canSave = trimmedWord.isNotEmpty() && !state.isTranslating && !state.isSaving
    val busy = state.isTranslating || state.isSaving

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

            OutlinedTextField(
                value = state.word,
                onValueChange = onWordChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                singleLine = true,
                enabled = !busy,
                placeholder = { Text("단어 또는 표현 입력") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                isError = state.error != null,
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
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "번역 실패: ${state.error}",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        FilledTonalButton(
                            onClick = onSave,
                            enabled = canSave,
                        ) {
                            Icon(Icons.Filled.Refresh, contentDescription = null)
                            Text(text = "다시 시도", modifier = Modifier.padding(start = 8.dp))
                        }
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
                    enabled = canSave,
                ) {
                    Text(
                        when {
                            state.isSaving -> "저장 중..."
                            state.isTranslating -> "번역 중..."
                            else -> "저장"
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun TabLabel(
    label: String,
    isSource: Boolean,
    status: TranslationStatus?,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(text = label)
        // 원문/번역 탭 모두 상태와 무관하게 10dp 영역을 항상 예약한다. 번역 탭의 PENDING →
        // SUCCESS 전이뿐 아니라 원문/번역 탭 사이의 너비 비대칭도 함께 방지한다. 원문 탭은
        // status 가 null 이므로 Box 내부가 비어 있다.
        Box(
            modifier = Modifier.size(10.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (!isSource) {
                when (status) {
                    TranslationStatus.PENDING -> CircularProgressIndicator(
                        modifier = Modifier.size(8.dp),
                        strokeWidth = 1.5.dp,
                    )
                    TranslationStatus.ERROR -> Icon(
                        imageVector = Icons.Filled.Error,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(10.dp),
                    )
                    TranslationStatus.SUCCESS, null -> Unit
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
