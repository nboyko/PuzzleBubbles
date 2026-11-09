package eu.bfss.puzzlebubbles

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import androidx.compose.runtime.CompositionLocalProvider
import eu.bfss.puzzlebubbles.presentation.components.LocalPhotoImages
import eu.bfss.puzzlebubbles.data.LocalGameRepository
import eu.bfss.puzzlebubbles.data.LocalAudioPlayer
import eu.bfss.puzzlebubbles.presentation.PuzzleBubblesApp
import eu.bfss.puzzlebubbles.presentation.home.HomeViewModel
import eu.bfss.puzzlebubbles.ui.theme.PuzzleBubblesTheme

class MainActivity : ComponentActivity() {
    private val gameRepository get() = (application as PuzzleBubblesApplication).gameRepository
    private lateinit var audioPlayer: LocalAudioPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        audioPlayer = LocalAudioPlayer(this) { (application as PuzzleBubblesApplication).soundFiles.assetFile(it) }
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
