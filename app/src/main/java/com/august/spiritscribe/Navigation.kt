package com.august.spiritscribe

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.august.spiritscribe.ui.diary.DiaryDetailScreen
import com.august.spiritscribe.ui.diary.DiaryListScreen
import com.august.spiritscribe.ui.diary.WriteDiaryScreen
import com.august.spiritscribe.ui.flashcard.FlashCardScreen
import com.august.spiritscribe.ui.settings.SettingsScreen
import com.august.spiritscribe.ui.translate.TranslateBrowseScreen
import kotlinx.serialization.Serializable

@Serializable
data class WriteDiary(val entryId: String? = null)

@Serializable
data class DiaryDetail(val id: String)

@Serializable
data object FlashCardStudy

val hideBottomNavigationRoutes = listOf(
    WriteDiary::class,
    DiaryDetail::class,
    FlashCardStudy::class,
)

sealed class Screen(val route: String) {
    open val label: String = ""
    open val icon: ImageVector = Icons.Filled.AutoStories

    object Diary : Screen("diary") {
        override val icon: ImageVector = Icons.Filled.AutoStories
        override val label: String = "일기"
    }

    object Translate : Screen("translate") {
        override val icon: ImageVector = Icons.Filled.Translate
        override val label: String = "번역"
    }

    object FlashCard : Screen("flashcard") {
        override val icon: ImageVector = Icons.Filled.Style
        override val label: String = "단어장"
    }

    object Settings : Screen("settings") {
        override val icon: ImageVector = Icons.Filled.Settings
        override val label: String = "설정"
    }
}

@Composable
fun AppNavigation(
    modifier: Modifier = Modifier,
    navController: NavHostController,
) {
    NavHost(
        modifier = modifier,
        navController = navController,
        startDestination = Screen.Diary.route,
    ) {
        composable(Screen.Diary.route) {
            DiaryListScreen(
                onWriteClick = { navController.navigate(WriteDiary()) },
                onEntryClick = { id -> navController.navigate(DiaryDetail(id)) },
            )
        }
        composable(Screen.Translate.route) {
            TranslateBrowseScreen(
                onEntryClick = { id -> navController.navigate(DiaryDetail(id)) },
            )
        }
        composable(Screen.FlashCard.route) { FlashCardScreen() }
        composable(Screen.Settings.route) { SettingsScreen() }

        composable<WriteDiary> {
            WriteDiaryScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable<DiaryDetail> {
            DiaryDetailScreen(
                onNavigateBack = { navController.popBackStack() },
                onEditClick = { id -> navController.navigate(WriteDiary(entryId = id)) },
            )
        }
    }
}

@Composable
fun BottomNavigationBar(
    navController: NavController,
    currentDestination: NavDestination?,
    modifier: Modifier = Modifier,
) {
    NavigationBar(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        val items = listOf(
            Screen.Diary,
            Screen.Translate,
            Screen.FlashCard,
            Screen.Settings,
        )

        items.forEach { screen ->
            val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true

            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = screen.icon,
                        contentDescription = null,
                        tint = if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                label = {
                    Text(
                        text = screen.label,
                        color = if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                selected = selected,
                onClick = {
                    if (!selected) {
                        navController.navigate(screen.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
            )
        }
    }
}
