package eu.bfss.puzzlebubbles

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.espresso.Espresso.pressBack
import eu.bfss.puzzlebubbles.domain.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.*
import org.junit.Assert.*
import java.util.UUID
import java.io.File
import androidx.test.platform.app.InstrumentationRegistry

class CustomGameFlowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private lateinit var app: PuzzleBubblesApplication
    private val ownedPhotos = mutableListOf<LibraryPhoto>()
    private var sound: LibrarySound? = null
    private var gameId: String? = null
    private var name = "Family ${UUID.randomUUID().toString().take(6)}"
    private fun revealActions() {
        compose.onNodeWithContentDescription("Play $name").performScrollTo().performTouchInput { swipeLeft() }
        compose.waitForIdle()
    }
    private fun capture(name: String) {
        compose.mainClock.advanceTimeBy(500)
        compose.waitForIdle()
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        // Allow the emulator Surface to present the frame before a native screenshot.
        Thread.sleep(250)
        val screenshot = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        try {
            val directory = File(app.externalCacheDir, "custom-game-verification").apply { mkdirs() }
            File(directory, "$name.png").outputStream().use { screenshot.compress(Bitmap.CompressFormat.PNG, 100, it) }
        } finally { screenshot.recycle() }
    }
    @Before fun setup() = runBlocking {
        app = compose.activity.application as PuzzleBubblesApplication
        app.photoRepository.refresh(); app.soundRepository.refresh()
        repeat(2) { index ->
            val bitmap = Bitmap.createBitmap(1024, 1024, Bitmap.Config.ARGB_8888).apply { eraseColor(if (index == 0) Color.RED else Color.BLUE) }
            val draft = app.photoImages.putDraft(UUID.randomUUID().toString(), bitmap)
            try { ownedPhotos += app.photoRepository.save(draft) } finally { bitmap.recycle() }
        }
        val (id, file) = app.soundFiles.newDraft(); aacFixture(file, 2000)
        sound = app.soundRepository.save(SoundDraft("sound-draft:$id", app.soundFiles.duration(file)), "Fixture prompt")
    }
    @After fun cleanup() = runBlocking {
        // Only this test's generated records/files are owned by its cleanup.
        gameId?.let { app.gameRepository.deleteCustomGame(it) }
        ownedPhotos.forEach { app.photoRepository.delete(it.id) }
        sound?.let { app.soundRepository.delete(it.id) }
        Unit
    }
    @Test fun createsPlaysAndDeletesCustomGameWhileRetainingLibraryMedia() {
        compose.onNodeWithText("What's That?").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Add Game").performClick()
        compose.onNodeWithTag("bottom-navigation").assertDoesNotExist()
        compose.waitUntil(5000) {
            compose.onAllNodes(hasSetTextAction() and isEnabled()).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNode(hasSetTextAction()).performTextInput(name)
        closeSoftKeyboard()
        compose.onNodeWithText("Select photos").performScrollTo().performClick()
        ownedPhotos.forEach { compose.onNodeWithContentDescription(it.id).performScrollTo().performClick() }
        capture("photo-pool-modal")
        compose.onNodeWithText("Done").performClick()
        compose.onNodeWithText("Edit photos selection").performScrollTo().assertIsDisplayed()
        capture("game-step")
        compose.onNodeWithContentDescription("Back").assertDoesNotExist()
        compose.onNodeWithText("Continue").assertDoesNotExist()
        compose.onNodeWithText("Add Question").performScrollTo().performClick()
        compose.onNodeWithText("Choose correct image").performScrollTo().performClick()
        compose.onNodeWithTag("game-media-modal").assertIsDisplayed()
        compose.onNodeWithContentDescription(ownedPhotos[0].id).performScrollTo().performClick()
        capture("correct-image-modal")
        compose.onNodeWithText("Done").performClick()
        compose.onNodeWithText("Select sound").performScrollTo().performClick()
        compose.onNodeWithTag("game-media-modal").assertIsDisplayed()
        // Observe native playback frame-by-frame instead of auto-advancing past
        // the short clip while the new dialog is settling.
        compose.mainClock.autoAdvance = false
        try {
            compose.onNodeWithContentDescription("Preview Fixture prompt").performScrollTo().performClick()
            compose.waitUntil(5000) {
                compose.mainClock.advanceTimeByFrame()
                compose.onAllNodesWithContentDescription("Pause Fixture prompt").fetchSemanticsNodes().isNotEmpty()
            }
        } catch (failure: Throwable) {
            capture("sound-preview-failure")
            val modal = runCatching { compose.onNodeWithTag("game-media-modal").printToString() }.getOrDefault("Selector was dismissed")
            throw AssertionError("Preview state: $modal", failure)
        } finally {
            compose.mainClock.autoAdvance = true
        }
        compose.onNodeWithTag("select-sound-${sound!!.id}").performScrollTo().performClick()
        compose.onNodeWithTag("select-sound-${sound!!.id}").assertIsOn()
        capture("sound-modal")
        compose.onNodeWithText("Done").performClick()
        capture("questions-step")
        compose.onNodeWithText("Save").performClick()
        compose.waitUntil(5000) { compose.onAllNodesWithContentDescription("Play $name").fetchSemanticsNodes().isNotEmpty() }
        gameId = runBlocking { app.gameRepository.observeWhatsThatGames().first().single { it.name == name }.id }
        compose.onNodeWithContentDescription("Delete game Animals").assertDoesNotExist()
        capture("game-list")
        revealActions()
        compose.onNodeWithContentDescription("Edit game $name").performClick()
        compose.onNodeWithText("Edit Game").assertIsDisplayed()
        compose.waitUntil(5000) { compose.onAllNodes(hasSetTextAction() and isEnabled()).fetchSemanticsNodes().isNotEmpty() }
        capture("edit-game")
        val renamed = "$name edited"
        compose.onNode(hasSetTextAction()).performTextReplacement(renamed)
        closeSoftKeyboard()
        compose.onNodeWithText("Save").performClick()
        compose.waitUntil(5000) { compose.onAllNodesWithContentDescription("Play $renamed").fetchSemanticsNodes().isNotEmpty() }
        name = renamed
        assertEquals(gameId, runBlocking { app.gameRepository.observeWhatsThatGames().first().single { it.name == name }.id })
        compose.onNodeWithContentDescription("Play $name").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Round 1 of 1").assertIsDisplayed()
        capture("custom-gameplay")
        compose.onNodeWithContentDescription(ownedPhotos[0].id).performClick()
        compose.waitUntil(10000) { compose.onAllNodesWithTag("whats-that-selection").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithContentDescription("Delete game $name").assertDoesNotExist()
        revealActions()
        val editBounds = compose.onNodeWithContentDescription("Edit game $name").fetchSemanticsNode().boundsInRoot
        val deleteBounds = compose.onNodeWithContentDescription("Delete game $name").fetchSemanticsNode().boundsInRoot
        assertTrue(editBounds.right <= deleteBounds.left)
        compose.onNodeWithContentDescription("Delete game $name").performScrollTo().performClick()
        capture("delete-game-confirmation")
        compose.onNodeWithText("Cancel").performClick()
        compose.onNodeWithContentDescription("Play $name").assertExists()
        revealActions()
        compose.onNodeWithContentDescription("Delete game $name").performClick()
        compose.onNodeWithText("Delete").performClick()
        compose.waitUntil(5000) { compose.onAllNodesWithContentDescription("Play $name").fetchSemanticsNodes().isEmpty() }
        runBlocking {
            assertTrue(app.photoRepository.photos.value.containsAll(ownedPhotos))
            assertTrue(app.soundFiles.file(sound!!.id).exists())
            assertTrue(ownedPhotos.all { app.photoImages.file(it.id).exists() })
        }
        pressBack()
        compose.onNodeWithTag("home-content").assertIsDisplayed()
    }
}
