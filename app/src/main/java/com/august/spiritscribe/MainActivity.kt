package com.august.spiritscribe

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.august.spiritscribe.ui.theme.SpiritScribeTheme

@dagger.hilt.android.AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SpiritScribeTheme {
                MainScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val shouldHideBottomNav = currentDestination?.let { destination ->
        hideBottomNavigationRoutes.any { routeClass ->
            when (routeClass) {
                WriteDiary::class -> destination.hasRoute<WriteDiary>()
                DiaryDetail::class -> destination.hasRoute<DiaryDetail>()
                FlashCardStudy::class -> destination.hasRoute<FlashCardStudy>()
                else -> false
            }
        }
    } ?: false

    Scaffold(
        bottomBar = {
            AnimatedVisibility(
                visible = !shouldHideBottomNav,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it }),
            ) {
                BottomNavigationBar(
                    navController = navController,
                    currentDestination = currentDestination,
                )
            }
        },
    ) { paddingValues ->
        AppNavigation(
            navController = navController,
            modifier = if (shouldHideBottomNav) Modifier else Modifier.padding(paddingValues),
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, name = "DefaultPreviewDark")
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, name = "DefaultPreviewLight")
@Composable
fun AppPreview() {
    SpiritScribeTheme {
        MainScreen()
    }
}
