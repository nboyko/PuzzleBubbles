package com.example.puzzlebubbles

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.example.puzzlebubbles.data.LocalGameRepository
import com.example.puzzlebubbles.presentation.PuzzleBubblesApp
import com.example.puzzlebubbles.presentation.home.HomeViewModel
import com.example.puzzlebubbles.ui.theme.PuzzleBubblesTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val homeViewModel = ViewModelProvider(
            this,
            HomeViewModel.Factory(LocalGameRepository()),
        )[HomeViewModel::class.java]
        setContent {
            PuzzleBubblesTheme {
                PuzzleBubblesApp(games = homeViewModel.games)
            }
        }
    }
}
