package com.august.spiritscribe.ui.flashcard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.august.spiritscribe.domain.model.AppLanguage
import com.august.spiritscribe.domain.model.WordCard

private val masteryLabels = listOf("모름", "어려움", "보통", "완벽")

private fun languageBadgeColor(lang: AppLanguage): Color = when (lang) {
    AppLanguage.KOREAN -> Color(0xFF1565C0)
    AppLanguage.ENGLISH -> Color(0xFF2E7D32)
    AppLanguage.JAPANESE -> Color(0xFFC62828)
    AppLanguage.CHINESE -> Color(0xFFE65100)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashCardScreen(
    viewModel: FlashCardViewModel = hiltViewModel(),
) {
    val filteredCards by viewModel.filteredCards.collectAsStateWithLifecycle()
    val filter by viewModel.filterState.collectAsStateWithLifecycle()
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    var pendingDeleteCard by remember { mutableStateOf<WordCard?>(null) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("단어장") }) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            if (stats.total > 0) {
                StatsBanner(stats)
            }

            FilterChipRow(
                selected = filter,
                onSelect = viewModel::setFilter,
            )

            if (filteredCards.isEmpty()) {
                EmptyState(filter = filter, modifier = Modifier.fillMaxSize())
            } else {
                val revealedMap = remember { mutableStateMapOf<String, Boolean>() }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(filteredCards, key = { it.id }) { card ->
                        FlashCardItem(
                            card = card,
                            revealed = revealedMap[card.id] == true,
                            onToggleReveal = {
                                revealedMap[card.id] = !(revealedMap[card.id] ?: false)
                            },
                            onRate = { level -> viewModel.updateMastery(card, level) },
                            onToggleFavorite = { viewModel.toggleFavorite(card) },
                            onRequestDelete = { pendingDeleteCard = card },
                        )
                    }
                }
            }
        }
    }

    pendingDeleteCard?.let { target ->
        AlertDialog(
            onDismissRequest = { pendingDeleteCard = null },
            title = { Text("이 단어 카드를 삭제할까요?") },
            text = { Text("단어 \"${target.word}\" 카드를 삭제합니다.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteCard(target.id)
                    pendingDeleteCard = null
                }) { Text("삭제") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteCard = null }) { Text("취소") }
            },
        )
    }
}

@Composable
private fun StatsBanner(stats: FlashCardStats) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Text(
            text = "카드 ${stats.total}개 · 즐겨찾기 ${stats.favorites}개 · 오늘 복습 ${stats.dueToday}개",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )
    }
}

@Composable
private fun FilterChipRow(
    selected: FlashCardFilter,
    onSelect: (FlashCardFilter) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(
            selected = selected == FlashCardFilter.All,
            onClick = { onSelect(FlashCardFilter.All) },
            label = { Text("전체") },
        )
        FilterChip(
            selected = selected == FlashCardFilter.Favorites,
            onClick = { onSelect(FlashCardFilter.Favorites) },
            label = { Text("즐겨찾기") },
        )
        FilterChip(
            selected = selected == FlashCardFilter.DueForReview,
            onClick = { onSelect(FlashCardFilter.DueForReview) },
            label = { Text("복습 예정") },
        )
    }
}

@Composable
private fun EmptyState(
    filter: FlashCardFilter,
    modifier: Modifier = Modifier,
) {
    val (icon: ImageVector, message: String) = when (filter) {
        FlashCardFilter.All ->
            Icons.AutoMirrored.Outlined.MenuBook to "아직 단어 카드가 없습니다.\n일기에서 단어를 추가해 보세요."
        FlashCardFilter.Favorites ->
            Icons.Outlined.FavoriteBorder to "즐겨찾기한 카드가 없습니다."
        FlashCardFilter.DueForReview ->
            Icons.Outlined.CheckCircle to "복습 예정인 카드가 없습니다."
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(56.dp),
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun MasteryDots(level: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(3) { index ->
            val active = index < level
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(
                        color = if (active) MaterialTheme.colorScheme.primary
                        else Color.Transparent,
                        shape = CircleShape,
                    )
                    .then(
                        if (active) Modifier
                        else Modifier.background(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = CircleShape,
                        ),
                    ),
            )
        }
    }
}

@Composable
private fun LanguageBadge(lang: AppLanguage) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = languageBadgeColor(lang),
    ) {
        Text(
            text = lang.code.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

@Composable
private fun FlashCardItem(
    card: WordCard,
    revealed: Boolean,
    onToggleReveal: () -> Unit,
    onRate: (Int) -> Unit,
    onToggleFavorite: () -> Unit,
    onRequestDelete: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggleReveal),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        LanguageBadge(card.sourceLanguage)
                        Text(
                            text = card.word,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        MasteryDots(card.masteryLevel)
                        Text(
                            text = "복습 ${card.reviewCount}회",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                        )
                    }
                }
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        imageVector = if (card.isFavorite) Icons.Filled.Favorite
                        else Icons.Filled.FavoriteBorder,
                        contentDescription = "즐겨찾기",
                        tint = if (card.isFavorite) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (revealed) {
                AppLanguage.entries
                    .filter { it != card.sourceLanguage }
                    .forEach { lang ->
                        val value = card.translations[lang]
                        if (!value.isNullOrBlank()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                LanguageBadge(lang)
                                Text(
                                    text = value,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    masteryLabels.forEachIndexed { level, label ->
                        FilterChip(
                            selected = card.masteryLevel == level,
                            onClick = { onRate(level) },
                            label = { Text(label) },
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    IconButton(onClick = onRequestDelete) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = "삭제",
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            } else {
                Text(
                    text = "탭하여 뜻 보기",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
