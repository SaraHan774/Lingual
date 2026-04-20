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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.august.spiritscribe.R
import com.august.spiritscribe.domain.model.AppLanguage
import com.august.spiritscribe.domain.model.WordCard
import com.august.spiritscribe.ui.theme.languageBadgeColor

@Composable
private fun masteryLabels(): List<String> = listOf(
    stringResource(R.string.flashcard_mastery_0),
    stringResource(R.string.flashcard_mastery_1),
    stringResource(R.string.flashcard_mastery_2),
    stringResource(R.string.flashcard_mastery_3),
)

@Composable
private fun AppLanguage.localizedName(): String = stringResource(
    when (this) {
        AppLanguage.KOREAN -> R.string.language_name_ko
        AppLanguage.ENGLISH -> R.string.language_name_en
        AppLanguage.JAPANESE -> R.string.language_name_ja
        AppLanguage.CHINESE -> R.string.language_name_zh
    }
)

/**
 * 편집 시트가 열렸을 때 수정 중인 대상 — `(카드 ID, 언어)`. 시트는 이 값의 null 여부로 존재감이 결정된다.
 */
private data class EditTranslationTarget(
    val card: WordCard,
    val language: AppLanguage,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashCardScreen(
    viewModel: FlashCardViewModel = hiltViewModel(),
) {
    val filteredCards by viewModel.filteredCards.collectAsStateWithLifecycle()
    val filter by viewModel.filterState.collectAsStateWithLifecycle()
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    var pendingDeleteCard by remember { mutableStateOf<WordCard?>(null) }
    var editTarget by remember { mutableStateOf<EditTranslationTarget?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    val successMessage = stringResource(R.string.flashcard_edit_translation_success)
    val failureMessage = stringResource(R.string.flashcard_edit_translation_failure)

    // 성공 시 시트를 닫고 스낵바 노출. 실패 시 시트 유지 (AC-19).
    LaunchedEffect(Unit) {
        viewModel.translationEditSucceeded.collect {
            editTarget = null
            snackbarHostState.showSnackbar(successMessage)
        }
    }
    LaunchedEffect(Unit) {
        viewModel.translationEditFailed.collect {
            snackbarHostState.showSnackbar(failureMessage)
        }
    }

    // 시트가 열려 있는 동안에도 카드 번역 값이 갱신되면 동일 카드/언어의 최신 WordCard 로 교체.
    LaunchedEffect(filteredCards, editTarget?.card?.id) {
        val current = editTarget ?: return@LaunchedEffect
        val latest = filteredCards.firstOrNull { it.id == current.card.id } ?: return@LaunchedEffect
        if (latest != current.card) {
            editTarget = current.copy(card = latest)
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.flashcard_title)) }) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                            onEditTranslation = { lang ->
                                editTarget = EditTranslationTarget(card = card, language = lang)
                            },
                        )
                    }
                }
            }
        }
    }

    pendingDeleteCard?.let { target ->
        AlertDialog(
            onDismissRequest = { pendingDeleteCard = null },
            title = { Text(stringResource(R.string.flashcard_delete_dialog_title)) },
            text = { Text(stringResource(R.string.flashcard_delete_dialog_body, target.word)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteCard(target.id)
                    pendingDeleteCard = null
                }) { Text(stringResource(R.string.flashcard_delete_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteCard = null }) { Text(stringResource(R.string.common_cancel)) }
            },
        )
    }

    editTarget?.let { target ->
        EditTranslationBottomSheet(
            language = target.language,
            currentTranslation = target.card.translations[target.language],
            onSave = { newValue ->
                viewModel.updateTranslation(
                    wordCardId = target.card.id,
                    language = target.language,
                    newTranslation = newValue,
                )
            },
            onDismiss = { editTarget = null },
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
            text = stringResource(R.string.flashcard_stats_banner, stats.total, stats.favorites, stats.dueToday),
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
    // AC-9/AC-25: 네 번째 칩 "최근 추가" 추가. 좁은 화면(≤360dp)에서 일본어 전각 레이블로 overflow 되지
    // 않도록 Row 대신 LazyRow 로 수평 스크롤 허용. 기본 화면(≥400dp)에서는 4개 모두 노출된다.
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            FilterChip(
                selected = selected == FlashCardFilter.All,
                onClick = { onSelect(FlashCardFilter.All) },
                label = { Text(stringResource(R.string.common_all)) },
            )
        }
        item {
            FilterChip(
                selected = selected == FlashCardFilter.Favorites,
                onClick = { onSelect(FlashCardFilter.Favorites) },
                label = { Text(stringResource(R.string.flashcard_filter_favorites)) },
            )
        }
        item {
            FilterChip(
                selected = selected == FlashCardFilter.DueForReview,
                onClick = { onSelect(FlashCardFilter.DueForReview) },
                label = { Text(stringResource(R.string.flashcard_filter_due_for_review)) },
            )
        }
        item {
            FilterChip(
                selected = selected == FlashCardFilter.RecentlyAdded,
                onClick = { onSelect(FlashCardFilter.RecentlyAdded) },
                label = { Text(stringResource(R.string.flashcard_filter_recently_added)) },
            )
        }
    }
}

@Composable
private fun EmptyState(
    filter: FlashCardFilter,
    modifier: Modifier = Modifier,
) {
    val icon: ImageVector
    val message: String
    when (filter) {
        FlashCardFilter.All -> {
            icon = Icons.AutoMirrored.Outlined.MenuBook
            message = stringResource(R.string.flashcard_empty_all)
        }
        FlashCardFilter.Favorites -> {
            icon = Icons.Outlined.FavoriteBorder
            message = stringResource(R.string.flashcard_empty_favorites)
        }
        FlashCardFilter.DueForReview -> {
            icon = Icons.Outlined.CheckCircle
            message = stringResource(R.string.flashcard_empty_due_for_review)
        }
        // AC-14: "최근 추가" Empty State 는 4개 언어 리소스에 "다음 행동 제시" 문구 포함.
        FlashCardFilter.RecentlyAdded -> {
            icon = Icons.Outlined.Schedule
            message = stringResource(R.string.flashcard_empty_recently_added)
        }
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
private fun EditedBadge() {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Text(
            text = stringResource(R.string.flashcard_translation_edited_badge),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
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
    onEditTranslation: (AppLanguage) -> Unit,
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
                            text = stringResource(R.string.flashcard_review_count, card.reviewCount),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                        )
                    }
                }
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        imageVector = if (card.isFavorite) Icons.Filled.Favorite
                        else Icons.Filled.FavoriteBorder,
                        contentDescription = stringResource(R.string.flashcard_favorite_cd),
                        tint = if (card.isFavorite) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (revealed) {
                // AC-20: 수정됨 배지는 공개 상태에서 번역 목록 영역에 단 하나만 표시.
                if (card.isTranslationEdited) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        EditedBadge()
                    }
                }
                AppLanguage.entries
                    .filter { it != card.sourceLanguage }
                    .forEach { lang ->
                        TranslationRow(
                            language = lang,
                            translation = card.translations[lang],
                            onEdit = { onEditTranslation(lang) },
                        )
                    }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    masteryLabels().forEachIndexed { level, label ->
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
                            contentDescription = stringResource(R.string.flashcard_delete_cd),
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            } else {
                Text(
                    text = stringResource(R.string.flashcard_tap_to_reveal),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * 공개(revealed) 상태의 번역 목록 행. 번역 값이 없으면 플레이스홀더 노출 (AC-22/AC-23).
 *
 * 레이아웃: `[배지] [번역/플레이스홀더 weight=1f] [편집 아이콘]` — 긴 번역이 아이콘 영역을 침범하지 않도록
 * 텍스트에 weight 를 부여하고 아이콘은 고정폭으로 둔다.
 */
@Composable
private fun TranslationRow(
    language: AppLanguage,
    translation: String?,
    onEdit: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        LanguageBadge(language)
        val missing = translation.isNullOrBlank()
        Text(
            text = if (missing) stringResource(R.string.flashcard_translation_missing) else translation!!,
            style = MaterialTheme.typography.bodyMedium,
            color = if (missing) MaterialTheme.colorScheme.onSurfaceVariant
            else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onEdit) {
            Icon(
                imageVector = Icons.Filled.Edit,
                contentDescription = stringResource(R.string.flashcard_edit_translation_cd),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * 번역 수정 BottomSheet.
 *
 * - 현재 번역값이 있으면 전체 선택 상태로 포커스되고 (AC-18), 없으면 빈 TextField + hint 표시.
 * - [저장] 버튼 비활성화 조건:
 *   - 기존 번역이 있는 경우: 빈 값 또는 기존 번역과 동일.
 *   - 번역 실패/null 인 경우: 빈 값. (동일값 비교는 원본이 없으므로 미적용 — AC-22)
 * - 파괴적 덮어쓰기 경고는 항상 표시 (AC-18).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditTranslationBottomSheet(
    language: AppLanguage,
    currentTranslation: String?,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val focusRequester = remember { FocusRequester() }

    val initialValue = currentTranslation.orEmpty()
    // TextFieldValue 로 시작 시 전체 선택 상태를 표현. key 로 language 를 주어 다른 언어로 시트가 재사용돼도 초기 선택이 재적용되도록 함.
    var fieldValue by remember(language, currentTranslation) {
        mutableStateOf(
            TextFieldValue(
                text = initialValue,
                selection = if (initialValue.isEmpty()) TextRange.Zero
                else TextRange(0, initialValue.length),
            )
        )
    }

    LaunchedEffect(language) { focusRequester.requestFocus() }

    val trimmedNew = fieldValue.text.trim()
    val hasOriginal = !currentTranslation.isNullOrBlank()
    // AC-18 비활성화 조건: 기존 번역이 있으면 (동일 or 빈 값), 없으면 빈 값만.
    // 공백만 입력된 경우도 빈 값과 동일하게 취급해 저장을 막는다.
    val canSave = when {
        trimmedNew.isEmpty() -> false
        hasOriginal && trimmedNew == currentTranslation -> false
        else -> true
    }

    val languageName = language.localizedName()
    val title = stringResource(R.string.flashcard_edit_translation_title, languageName)

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
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )

            OutlinedTextField(
                value = fieldValue,
                onValueChange = { fieldValue = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                placeholder = { Text(stringResource(R.string.flashcard_edit_translation_hint)) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                singleLine = false,
                maxLines = 4,
            )

            Text(
                text = stringResource(R.string.flashcard_edit_translation_warning),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            ) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.flashcard_edit_translation_cancel))
                }
                TextButton(
                    onClick = { onSave(trimmedNew) },
                    enabled = canSave,
                ) {
                    Text(stringResource(R.string.flashcard_edit_translation_save))
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}
