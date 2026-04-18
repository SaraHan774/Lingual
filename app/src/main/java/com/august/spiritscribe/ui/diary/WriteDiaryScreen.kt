package com.august.spiritscribe.ui.diary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.august.spiritscribe.domain.model.AppLanguage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WriteDiaryScreen(
    onNavigateBack: () -> Unit,
    viewModel: WriteDiaryViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.saved) {
        if (state.saved) onNavigateBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("새 일기") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(text = "작성 언어", style = MaterialTheme.typography.labelLarge)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AppLanguage.entries.forEach { lang ->
                    FilterChip(
                        selected = state.sourceLanguage == lang,
                        onClick = { viewModel.updateSourceLanguage(lang) },
                        label = { Text(lang.displayName) },
                    )
                }
            }

            OutlinedTextField(
                value = state.title,
                onValueChange = viewModel::updateTitle,
                label = { Text("제목") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            OutlinedTextField(
                value = state.content,
                onValueChange = viewModel::updateContent,
                label = { Text("오늘 있었던 일을 적어보세요") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                minLines = 8,
            )

            state.error?.let { msg ->
                Text(text = msg, color = MaterialTheme.colorScheme.error)
            }

            Button(
                onClick = viewModel::save,
                enabled = state.content.isNotBlank() && !state.isSaving,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (state.isSaving) "저장 중..." else "저장하고 번역하기")
            }
        }
    }
}
