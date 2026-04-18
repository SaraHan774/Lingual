package com.august.spiritscribe.ui.diary

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.august.spiritscribe.domain.model.DiaryEntry
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiaryListScreen(
    onWriteClick: () -> Unit,
    onEntryClick: (String) -> Unit,
    viewModel: DiaryListViewModel = hiltViewModel(),
) {
    val entries by viewModel.entries.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text("내 일기") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onWriteClick) {
                Icon(Icons.Filled.Edit, contentDescription = "새 일기 작성")
            }
        },
    ) { padding ->
        if (entries.isEmpty()) {
            EmptyState(padding)
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(entries, key = { it.id }) { entry ->
                    DiaryItem(entry = entry, onClick = { onEntryClick(entry.id) })
                }
            }
        }
    }
}

@Composable
private fun EmptyState(padding: PaddingValues) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "아직 작성된 일기가 없습니다.\n우측 하단의 버튼으로 첫 일기를 써보세요.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun DiaryItem(
    entry: DiaryEntry,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = entry.title.ifBlank { "제목 없음" },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = entry.content.take(120),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
            )
            Text(
                text = formatDate(entry.createdAt),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }
    }
}

private fun formatDate(millis: Long): String =
    SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.getDefault()).format(Date(millis))
