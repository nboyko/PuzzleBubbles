package com.example.puzzlebubbles.presentation

import android.app.Activity
import android.net.Uri
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.material3.*
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.res.stringResource
import com.example.puzzlebubbles.R
import androidx.activity.compose.BackHandler
import com.example.puzzlebubbles.presentation.games.whatsthat.*
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
import com.example.puzzlebubbles.presentation.games.whatsthat.WhatsThatGameScreen
import com.example.puzzlebubbles.presentation.games.whatsthat.WhatsThatViewModel
import com.example.puzzlebubbles.presentation.games.whatsthat.WhatsThatGameSelectionScreen
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
                            GameType.WHATS_THAT -> "whats-that"
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
                val selection: GameSelectionViewModel = viewModel(factory = GameSelectionViewModel.Factory(gameRepository))
                BackHandler(enabled = selection.deleteId != null || selection.deleting) { selection.cancelDelete() }
                WhatsThatGameSelectionScreen(
                    games = selection.games,
                    deletingGameId = selection.deleteId,
                    loadError = selection.loadError,
                    onRetry = selection::reload,
                    enabled = !selection.deleting && selection.deleteId == null,
                    onDelete = selection::requestDelete,
                    onAdd = {
                        if (!selection.deleting && selection.deleteId == null && navController.currentDestination?.route == "whats-that")
                            navController.navigate("create-game") { launchSingleTop = true }
                    },
                    onPlay = { gameId ->
                        if (!selection.deleting && selection.deleteId == null && navController.currentBackStackEntry?.destination?.route == "whats-that") {
                            navController.navigate("whats-that/play/${Uri.encode(gameId)}") { launchSingleTop = true }
                        }
                    },
                    onHome = { if (!selection.deleting) navController.popBackStack("home", inclusive = false) },
                    onLibrary = {
                        if (!selection.deleting && navController.currentBackStackEntry?.destination?.route == "whats-that") {
                            navController.navigate("library") {
                                popUpTo("home")
                                launchSingleTop = true
                            }
                        }
                    },
                )
                if (selection.deleteId != null) BlueGameTheme { AlertDialog(onDismissRequest = selection::cancelDelete,
                    title = { Text(stringResource(R.string.delete_game_title)) },
                    text = { Text(stringResource(if (selection.deleteError) R.string.game_delete_error else R.string.delete_game_message)) },
                    confirmButton = { TextButton(onClick = selection::delete, enabled = !selection.deleting) { Text(stringResource(R.string.delete_photo)) } },
                    dismissButton = { TextButton(onClick = selection::cancelDelete, enabled = !selection.deleting) { Text(stringResource(R.string.cancel)) } }) }
            }
            composable("create-game") {
                val app = LocalContext.current.applicationContext as PuzzleBubblesApplication
                val model: CreateGameViewModel = viewModel(factory = CreateGameViewModel.Factory(
                    gameRepository, app.photoRepository, app.soundRepository,
                    { com.example.puzzlebubbles.data.LocalAudioPlayer(app, app.soundFiles::assetFile) }))
                CreateGameScreen(model) {
                    if (navController.currentDestination?.route == "create-game") navController.popBackStack()
                }
            }
            composable("whats-that/play/{gameId}", arguments = listOf(navArgument("gameId") { type = NavType.StringType })) { entry ->
                val gameDefinition = games.single { it.type == GameType.WHATS_THAT }
                val gameId = requireNotNull(entry.arguments?.getString("gameId"))
                var attempt by remember { mutableIntStateOf(0) }
                var selectedGame by remember(gameId) { mutableStateOf<com.example.puzzlebubbles.domain.WhatsThatGame?>(null) }
                var failed by remember(gameId) { mutableStateOf(false) }
                LaunchedEffect(gameId, attempt) {
                    failed = false
                    try { selectedGame = gameRepository.loadWhatsThatGame(gameId) }
                    catch (e: kotlinx.coroutines.CancellationException) { throw e }
                    catch (_: Exception) { failed = true }
                }
                if (selectedGame == null) {
                    Column(screenModifier) {
                        TextButton(onClick = { navController.popBackStack() }) { Text(stringResource(R.string.back)) }
                        if (failed) {
                            Text(stringResource(R.string.game_load_error))
                            TextButton(onClick = { attempt++ }) { Text(stringResource(R.string.retry)) }
                        } else CircularProgressIndicator()
                    }
                    return@composable
                }
                val whatsThatViewModel: WhatsThatViewModel = viewModel(
                    factory = WhatsThatViewModel.Factory(checkNotNull(selectedGame)),
                )
                WhatsThatGameScreen(
                    title = if (gameId == "whats-that") gameDefinition.title else checkNotNull(selectedGame).title,
                    uiState = whatsThatViewModel.uiState,
                    audioPlayer = audioPlayer,
                    onAnswer = whatsThatViewModel::answer,
                    onReplay = whatsThatViewModel::replayAudio,
                    onFeedbackFinished = whatsThatViewModel::finishFeedback,
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
