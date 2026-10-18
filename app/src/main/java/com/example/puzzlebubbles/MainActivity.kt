package com.example.puzzlebubbles

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import androidx.compose.runtime.CompositionLocalProvider
import com.example.puzzlebubbles.presentation.components.LocalPhotoImages
import com.example.puzzlebubbles.data.LocalGameRepository
import com.example.puzzlebubbles.data.LocalAudioPlayer
import com.example.puzzlebubbles.presentation.PuzzleBubblesApp
import com.example.puzzlebubbles.presentation.home.HomeViewModel
import com.example.puzzlebubbles.ui.theme.PuzzleBubblesTheme

class MainActivity : ComponentActivity() {
    private val gameRepository = LocalGameRepository()
    private lateinit var audioPlayer: LocalAudioPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        audioPlayer = LocalAudioPlayer(this)
        val homeViewModel = ViewModelProvider(
            this,
            HomeViewModel.Factory(gameRepository),
        )[HomeViewModel::class.java]
        setContent {
            PuzzleBubblesTheme {
                CompositionLocalProvider(LocalPhotoImages provides (application as PuzzleBubblesApplication).photoImages) {
                    PuzzleBubblesApp(
                        games = homeViewModel.games,
                        gameRepository = gameRepository,
                        audioPlayer = audioPlayer,
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        audioPlayer.release()
        super.onDestroy()
    }
}
