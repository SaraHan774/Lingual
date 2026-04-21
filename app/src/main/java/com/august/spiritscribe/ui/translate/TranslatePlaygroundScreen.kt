package com.august.spiritscribe.ui.translate

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.august.spiritscribe.R
import com.august.spiritscribe.domain.model.AppLanguage
import com.august.spiritscribe.utils.TtsState

/**
 * AC-T02-01 — 번역 탭 진입 시 열리는 Translate Playground.
 *
 * @param onNavigateToFlashCard 단어 카드 저장 성공 스낵바의 "FlashCard 보기" Action 에서 호출.
 *   AC-T02-21 — `Screen.FlashCard` 로 이동하되 현재 화면은 백스택에 유지.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranslatePlaygroundScreen(
    onNavigateToFlashCard: () -> Unit,
    viewModel: TranslatePlaygroundViewModel = hiltViewModel(),
) {
    val sourceLanguage by viewModel.sourceLanguage.collectAsStateWithLifecycle()
    val inputText by viewModel.inputText.collectAsStateWithLifecycle()
    val results by viewModel.results.collectAsStateWithLifecycle()
    val resultsContext by viewModel.resultsContext.collectAsStateWithLifecycle()
    val showFirstUseHint by viewModel.showFirstUseHint.collectAsStateWithLifecycle()
    val ttsState by viewModel.ttsState.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val saveSuccessMessage = stringResource(R.string.translate_playground_save_success)
    val saveSuccessAction = stringResource(R.string.translate_playground_save_success_action)
    val saveFailureMessage = stringResource(R.string.translate_playground_save_failure)
    val ttsUnavailableMessage = stringResource(R.string.translate_playground_tts_unavailable)

    // SharedFlow(replay=0) 이벤트 수집. Composition 재생성 시 과거 이벤트가 재방출되지 않는다.
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is PlaygroundEvent.WordCardSaved -> {
                    val result = snackbarHostState.showSnackbar(
                        message = saveSuccessMessage,
                        actionLabel = saveSuccessAction,
                        duration = SnackbarDuration.Short,
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        onNavigateToFlashCard()
                    }
                }
                is PlaygroundEvent.WordCardSaveFailed -> {
                    snackbarHostState.showSnackbar(
                        message = saveFailureMessage,
                        duration = SnackbarDuration.Short,
                    )
                }
                is PlaygroundEvent.TtsUnavailable -> {
                    snackbarHostState.showSnackbar(
                        message = ttsUnavailableMessage,
                        duration = SnackbarDuration.Short,
                    )
                }
            }
        }
    }

    val targetLanguages = remember(sourceLanguage) {
        AppLanguage.entries.filter { it != sourceLanguage }
    }
    val successCount by remember(results) {
        derivedStateOf {
            results.values.count { it is PlaygroundResult.Success }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.translate_playground_title)) })
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding(),
        ) {
            SourceLanguageRow(
                sourceLanguage = sourceLanguage,
                clearEnabled = inputText.isNotEmpty(),
                onLanguageChange = viewModel::updateSourceLanguage,
                onClear = viewModel::clear,
            )

            InputArea(
                text = inputText,
                onTextChange = viewModel::updateInput,
            )

            SaveButton(
                successCount = successCount,
                onClick = viewModel::saveWordCard,
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                val isInputBlank = inputText.trim().isEmpty()
                // AC-T02-12 — 현재 (source, text) 가 results 산출 시점의 (source, text) 와 다르면 outdated.
                val outdated = !isInputBlank &&
                    (resultsContext.first != sourceLanguage || resultsContext.second != inputText.trim())
                targetLanguages.forEach { lang ->
                    val state = results[lang] ?: PlaygroundResult.Idle
                    ResultCard(
                        language = lang,
                        state = state,
                        // Success/Error 카드만 흐리게 — Loading/Idle 은 그 자체 시각이 우선.
                        isOutdated = outdated &&
                            (state is PlaygroundResult.Success || state is PlaygroundResult.Error),
                        isPlaying = ttsState is TtsState.Playing &&
                            (ttsState as TtsState.Playing).language == lang,
                        onSpeakToggle = { viewModel.toggleSpeak(lang) },
                        onRetry = { viewModel.retry(lang) },
                    )
                }

                // AC-T02-28 — 첫 진입 시 Empty State 헬퍼. 한 번이라도 Loading/Success/Error 진입하면 영구 숨김.
                if (showFirstUseHint) {
                    Text(
                        text = stringResource(R.string.translate_playground_first_use_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }

                // 빈 입력 + 헬퍼 미표시 (한 번 사용 후 지움) 상태에서 placeholder 노출.
                val allIdle = results.values.all { it is PlaygroundResult.Idle }
                if (allIdle && isInputBlank && !showFirstUseHint) {
                    Text(
                        text = stringResource(R.string.translate_playground_placeholder),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun SourceLanguageRow(
    sourceLanguage: AppLanguage,
    clearEnabled: Boolean,
    onLanguageChange: (AppLanguage) -> Unit,
    onClear: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.translate_playground_source_language_label),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Box {
            var expanded by remember { mutableStateOf(false) }
            TextButton(onClick = { expanded = true }) {
                Text(text = sourceLanguage.displayName)
                Icon(
                    imageVector = Icons.Filled.ArrowDropDown,
                    contentDescription = null,
                )
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                AppLanguage.entries.forEach { lang ->
                    DropdownMenuItem(
                        text = { Text(lang.displayName) },
                        onClick = {
                            expanded = false
                            onLanguageChange(lang)
                        },
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // AC-T02-18 — 입력이 있을 때만 활성.
        IconButton(
            onClick = onClear,
            enabled = clearEnabled,
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = stringResource(R.string.translate_playground_clear_cd),
            )
        }
    }
}

@Composable
private fun InputArea(
    text: String,
    onTextChange: (String) -> Unit,
) {
    val len = text.length
    // AC-T02-04 — 180 자 이상이면 error 색, 미만이면 onSurfaceVariant.
    val counterColor = if (len >= TranslatePlaygroundViewModel.WARN_INPUT_LEN) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            modifier = Modifier.fillMaxWidth(),
            maxLines = 4,
            placeholder = {
                Text(stringResource(R.string.translate_playground_input_hint))
            },
        )
        Text(
            text = stringResource(
                R.string.translate_playground_char_counter,
                len,
                TranslatePlaygroundViewModel.MAX_INPUT_LEN,
            ),
            style = MaterialTheme.typography.labelSmall,
            color = counterColor,
            modifier = Modifier
                .align(Alignment.End)
                .padding(top = 4.dp),
        )
    }
}

@Composable
private fun SaveButton(
    successCount: Int,
    onClick: () -> Unit,
) {
    val enabled = successCount >= 1
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .alpha(if (enabled) 1f else 0.38f),
    ) {
        Text(
            text = stringResource(R.string.translate_playground_save_button_label, successCount),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ResultCard(
    language: AppLanguage,
    state: PlaygroundResult,
    isOutdated: Boolean,
    isPlaying: Boolean,
    onSpeakToggle: () -> Unit,
    onRetry: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (isOutdated) 0.5f else 1f),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // AC-T02-19 — 헤더에는 언어명 + [♪] TTS 만. 카드별 [+] 절대 없음.
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = language.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.weight(1f))
                IconButton(
                    onClick = onSpeakToggle,
                    enabled = state is PlaygroundResult.Success,
                ) {
                    Icon(
                        imageVector = if (isPlaying) {
                            Icons.Filled.Stop
                        } else {
                            Icons.AutoMirrored.Filled.VolumeUp
                        },
                        contentDescription = stringResource(R.string.translate_playground_tts_cd),
                        tint = if (state is PlaygroundResult.Success) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }

            when (state) {
                is PlaygroundResult.Idle -> {
                    Text(
                        text = stringResource(R.string.translate_playground_placeholder),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                is PlaygroundResult.Loading -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = stringResource(R.string.translate_playground_loading),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                is PlaygroundResult.Success -> {
                    Text(
                        text = state.text,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
                is PlaygroundResult.Error -> {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = stringResource(R.string.translate_playground_error_generic),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        FilledTonalButton(onClick = onRetry) {
                            Icon(Icons.Filled.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = stringResource(R.string.translate_playground_retry))
                        }
                    }
                }
            }
        }
    }
}

