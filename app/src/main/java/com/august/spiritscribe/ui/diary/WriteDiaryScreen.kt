package com.august.spiritscribe.ui.diary

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.august.spiritscribe.R
import com.august.spiritscribe.domain.model.AppLanguage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WriteDiaryScreen(
    onNavigateBack: () -> Unit,
    viewModel: WriteDiaryViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showExitDialog by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(state.saved) {
        if (state.saved) onNavigateBack()
    }

    // AC-E10: 편집 모드에서 엔트리 로드 실패 시 스낵바 → popBackStack.
    // SharedFlow(replay=0) + LaunchedEffect(Unit) 패턴으로 회전 시 재방출 방지.
    LaunchedEffect(Unit) {
        viewModel.loadErrorEvents.collect { message ->
            snackbarHostState.showSnackbar(message)
            onNavigateBack()
        }
    }

    // 실제 이탈 동작: 변경분이 있으면 다이얼로그, 없으면 즉시 복귀.
    val requestExit: () -> Unit = {
        if (state.isDirty) showExitDialog = true else onNavigateBack()
    }

    // AC-E08: 시스템 Back 제스처도 동일하게 가로채야 한다. isDirty 일 때만 enabled → 변경 없으면
    // 기본 Back 처리를 막지 않아 부드럽게 빠진다.
    BackHandler(enabled = state.isDirty) {
        showExitDialog = true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (state.isEditMode) stringResource(R.string.write_diary_title_edit)
                        else stringResource(R.string.write_diary_title_new)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = requestExit) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(text = stringResource(R.string.write_diary_label_source_language), style = MaterialTheme.typography.labelLarge)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    AppLanguage.entries.forEach { lang ->
                        FilterChip(
                            selected = state.sourceLanguage == lang,
                            onClick = { viewModel.updateSourceLanguage(lang) },
                            label = { Text(lang.displayName) },
                            enabled = !state.isSaving,
                        )
                    }
                }

                OutlinedTextField(
                    value = state.title,
                    onValueChange = viewModel::updateTitle,
                    label = { Text(stringResource(R.string.write_diary_label_title)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !state.isSaving,
                )

                OutlinedTextField(
                    value = state.content,
                    onValueChange = viewModel::updateContent,
                    label = { Text(stringResource(R.string.write_diary_placeholder_content)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    minLines = 8,
                    enabled = !state.isSaving,
                )

                state.error?.let { msg ->
                    Text(text = msg, color = MaterialTheme.colorScheme.error)
                }

                Button(
                    onClick = viewModel::save,
                    enabled = state.content.isNotBlank() && !state.isSaving,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    // AC-E04: 저장 중 CircularProgressIndicator + 레이블 전환. 신규/편집 공통.
                    if (state.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                        Text(
                            text = stringResource(R.string.write_diary_btn_saving),
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    } else {
                        Text(
                            text = if (state.isEditMode) stringResource(R.string.common_save)
                            else stringResource(R.string.write_diary_btn_save_new),
                        )
                    }
                }
            }
        }
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text(stringResource(R.string.write_diary_dialog_exit_title)) },
            text = { Text(stringResource(R.string.write_diary_dialog_exit_body)) },
            confirmButton = {
                TextButton(onClick = {
                    showExitDialog = false
                    onNavigateBack()
                }) { Text(stringResource(R.string.write_diary_dialog_exit_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) { Text(stringResource(R.string.common_cancel)) }
            },
        )
    }
}
