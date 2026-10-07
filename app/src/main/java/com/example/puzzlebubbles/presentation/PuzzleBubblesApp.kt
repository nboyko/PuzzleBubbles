package com.example.puzzlebubbles.presentation

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.puzzlebubbles.domain.GameDefinition
import com.example.puzzlebubbles.domain.GameType
import com.example.puzzlebubbles.presentation.game.ChooseItGameScreen
import com.example.puzzlebubbles.presentation.game.MatchingGameScreen
import com.example.puzzlebubbles.presentation.home.HomeScreen

@Composable
fun PuzzleBubblesApp(games: List<GameDefinition>, modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val view = LocalView.current
    val lightSystemBars = (backStackEntry?.destination?.route ?: "home") == "home" || !isSystemInDarkTheme()
    SideEffect {
        (view.context as? Activity)?.window?.let { window ->
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = lightSystemBars
                isAppearanceLightNavigationBars = lightSystemBars
            }
        }
    }

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        val screenModifier = Modifier.fillMaxSize().safeDrawingPadding()
        NavHost(navController = navController, startDestination = "home") {
            composable("home") {
                HomeScreen(
                    games = games,
                    onGameSelected = { game ->
                        val route = when (game.type) {
                            GameType.CHOOSE_IT -> "choose-it"
                            GameType.MATCHING -> "matching"
                        }
                        navController.navigate(route) { launchSingleTop = true }
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }
            composable("choose-it") {
                ChooseItGameScreen(
                    game = games.single { it.type == GameType.CHOOSE_IT },
                    onBack = { navController.popBackStack() },
                    modifier = screenModifier,
                )
            }
            composable("matching") {
                MatchingGameScreen(
                    game = games.single { it.type == GameType.MATCHING },
                    onBack = { navController.popBackStack() },
                    modifier = screenModifier,
                )
            }
        }
    }
}
