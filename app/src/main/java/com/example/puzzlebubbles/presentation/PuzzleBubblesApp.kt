package com.example.puzzlebubbles.presentation

import android.app.Activity
import android.net.Uri
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.puzzlebubbles.data.AudioPlayer
import com.example.puzzlebubbles.domain.GameDefinition
import com.example.puzzlebubbles.domain.GameType
import com.example.puzzlebubbles.domain.GameRepository
import com.example.puzzlebubbles.presentation.games.chooseit.ChooseItGameScreen
import com.example.puzzlebubbles.presentation.games.chooseit.ChooseItViewModel
import com.example.puzzlebubbles.presentation.games.chooseit.WhatsThatGameSelectionScreen
import com.example.puzzlebubbles.presentation.games.matching.MatchingGameScreen
import com.example.puzzlebubbles.presentation.home.HomeScreen
import com.example.puzzlebubbles.presentation.library.LibraryScreen
import com.example.puzzlebubbles.presentation.library.PhotosScreen
import com.example.puzzlebubbles.presentation.library.SoundsScreen
import com.example.puzzlebubbles.presentation.library.SoundsViewModel
import com.example.puzzlebubbles.presentation.library.PhotosViewModel
import com.example.puzzlebubbles.PuzzleBubblesApplication
import androidx.compose.ui.platform.LocalContext

@Composable
fun PuzzleBubblesApp(
    games: List<GameDefinition>,
    gameRepository: GameRepository,
    audioPlayer: AudioPlayer,
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val view = LocalView.current
    val route = backStackEntry?.destination?.route ?: "home"
    val lightSystemBars = route in listOf("home", "whats-that", "library", "library/photos", "library/sounds") || !isSystemInDarkTheme()
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
                            GameType.CHOOSE_IT -> "whats-that"
                            GameType.MATCHING -> "matching"
                        }
                        if (navController.currentBackStackEntry?.destination?.route == "home") {
                            navController.navigate(route) { launchSingleTop = true }
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                    onLibrarySelected = { navController.navigate("library") { launchSingleTop = true } },
                )
            }
            composable("library") {
                LibraryScreen(
                    onBack = { navController.popBackStack() },
                    onPhotosSelected = { navController.navigate("library/photos") { launchSingleTop = true } },
                    onSoundsSelected = { navController.navigate("library/sounds") { launchSingleTop = true } },
                    modifier = Modifier.fillMaxSize(),
                )
            }
            composable("library/photos") {
                val app = LocalContext.current.applicationContext as PuzzleBubblesApplication
                val photosModel: PhotosViewModel = viewModel(factory = PhotosViewModel.Factory(
                    app.photoRepository, app.photoImages, app.photoProcessor, app.contentResolver,
                ))
                PhotosScreen(
                    model = photosModel,
                    onBack = { navController.popBackStack() },
                    onHome = { navController.popBackStack("home", inclusive = false) },
                    modifier = Modifier.fillMaxSize(),
                )
            }
            composable("library/sounds") {
                val app = LocalContext.current.applicationContext as PuzzleBubblesApplication
                val soundsModel: SoundsViewModel = viewModel(factory = SoundsViewModel.Factory(app))
                SoundsScreen(
                    model = soundsModel,
                    onBack = { navController.popBackStack() },
                    onHome = { navController.popBackStack("home", inclusive = false) },
                    modifier = Modifier.fillMaxSize(),
                )
            }
            composable("whats-that") {
                val availableGames = remember(gameRepository) { gameRepository.getChooseItGames() }
                WhatsThatGameSelectionScreen(
                    games = availableGames,
                    onPlay = { gameId ->
                        if (navController.currentBackStackEntry?.destination?.route == "whats-that") {
                            navController.navigate("choose-it/${Uri.encode(gameId)}") { launchSingleTop = true }
                        }
                    },
                    onHome = { navController.popBackStack("home", inclusive = false) },
                    onLibrary = {
                        if (navController.currentBackStackEntry?.destination?.route == "whats-that") {
                            navController.navigate("library") {
                                popUpTo("home")
                                launchSingleTop = true
                            }
                        }
                    },
                )
            }
            composable("choose-it/{gameId}", arguments = listOf(navArgument("gameId") { type = NavType.StringType })) { entry ->
                val gameDefinition = games.single { it.type == GameType.CHOOSE_IT }
                val gameId = requireNotNull(entry.arguments?.getString("gameId"))
                val selectedGame = remember(gameRepository, gameId) { gameRepository.getChooseItGame(gameId) }
                val chooseItViewModel: ChooseItViewModel = viewModel(
                    factory = ChooseItViewModel.Factory(selectedGame),
                )
                ChooseItGameScreen(
                    title = gameDefinition.title,
                    uiState = chooseItViewModel.uiState,
                    audioPlayer = audioPlayer,
                    onAnswer = chooseItViewModel::answer,
                    onReplay = chooseItViewModel::replayAudio,
                    onFeedbackFinished = chooseItViewModel::finishFeedback,
                    onBack = { navController.popBackStack() },
                    onGameComplete = { navController.popBackStack("home", inclusive = false) },
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
