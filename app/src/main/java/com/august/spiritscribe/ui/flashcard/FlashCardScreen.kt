package com.august.spiritscribe.ui.flashcard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.august.spiritscribe.domain.model.AppLanguage
import com.august.spiritscribe.domain.model.WordCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashCardScreen(
    viewModel: FlashCardViewModel = hiltViewModel(),
) {
    val cards by viewModel.cards.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text("단어장") }) },
    ) { padding ->
        if (cards.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "아직 단어 카드가 없습니다.\n일기에서 단어를 추가해 보세요.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            val revealedMap = remember { mutableStateMapOf<String, Boolean>() }
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(cards, key = { it.id }) { card ->
                    FlashCardItem(
                        card = card,
                        revealed = revealedMap[card.id] == true,
                        onToggleReveal = {
                            revealedMap[card.id] = !(revealedMap[card.id] ?: false)
                        },
                        onRate = { level -> viewModel.updateMastery(card, level) },
                        onToggleFavorite = { viewModel.toggleFavorite(card) },
                    )
                }
            }
        }
    }
}

@Composable
private fun FlashCardItem(
    card: WordCard,
    revealed: Boolean,
    onToggleReveal: () -> Unit,
    onRate: (Int) -> Unit,
    onToggleFavorite: () -> Unit,
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
                Column {
                    Text(
                        text = card.word,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "원문: ${card.sourceLanguage.displayName} · 숙련도 ${card.masteryLevel}/3 · 복습 ${card.reviewCount}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
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
                            Text(
                                text = "${lang.displayName}: $value",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 4.dp),
                ) {
                    (0..3).forEach { level ->
                        AssistChip(
                            onClick = { onRate(level) },
                            label = { Text("Lv $level") },
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
