package com.example.puzzlebubbles

import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import com.example.puzzlebubbles.data.LocalGameRepository
import com.example.puzzlebubbles.domain.*
import com.example.puzzlebubbles.presentation.components.LocalPhotoImages
import com.example.puzzlebubbles.presentation.games.whatsthat.*
import com.example.puzzlebubbles.ui.theme.PuzzleBubblesTheme
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.*
import org.junit.Assert.*
import java.util.UUID

class CreateGameTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private lateinit var model: CreateGameViewModel
    private val ids = List(3) { UUID.randomUUID().toString() }
    private val sound = LibrarySound(UUID.randomUUID().toString(), 2000, 1, "My sound")
    private val secondSound = LibrarySound(UUID.randomUUID().toString(), 1800, 2, "Other sound")
    private val photos = object : PhotoRepository {
        override val photos = MutableStateFlow(ids.map { LibraryPhoto(it, 1) })
        override suspend fun refresh() = Unit
        override suspend fun save(draftAssetId: String, crop: PhotoCrop?) = error("unused")
        override suspend fun replace(id: String, crop: PhotoCrop) = Unit
        override suspend fun delete(id: String) = Unit
    }
    private val sounds = object : SoundRepository {
        override val sounds = MutableStateFlow(listOf(sound, secondSound))
        override suspend fun refresh() = Unit
        override suspend fun save(draft: SoundDraft, name: String) = error("unused")
        override suspend fun delete(id: String) = Unit
    }
    private val player = object : SoundPlayback {
        override val playback = MutableStateFlow(SoundPlaybackState())
        var stops = 0
        override fun toggle(assetId: String) { playback.value = SoundPlaybackState(assetId, playing = true) }
        override fun stop() { stops++; playback.value = SoundPlaybackState() }
        override fun release() = stop()
    }
    private var fail = false
    private var gate: CompletableDeferred<Unit>? = null
    private val saves = mutableListOf<CustomGame>()
    private var exits = 0
    private var editId: String? = null
    private var stored: CustomGame? = null
    private val updates = mutableListOf<CustomGame>()
    private val enlarged = mutableStateOf(false)
    private val games = object : GameRepository by LocalGameRepository() {
        override suspend fun loadCustomGame(id: String) = checkNotNull(stored).also { check(it.id == id) }
        override suspend fun updateCustomGame(game: CustomGame) {
            updates += game; gate?.await()
            if (fail) error("disk full")
            stored = game
        }
        override suspend fun saveCustomGame(game: CustomGame) {
            saves += game; gate?.await()
            if (fail) error("disk full")
        }
    }
    private fun show(activity: MainActivity) {
        model = ViewModelProvider(activity, CreateGameViewModel.Factory(games, photos, sounds, editId) { player })[
            editId ?: "create", CreateGameViewModel::class.java]
        activity.setContent { PuzzleBubblesTheme {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalPhotoImages provides (activity.application as PuzzleBubblesApplication).photoImages,
                LocalDensity provides Density(density.density, if (enlarged.value) 1.5f else density.fontScale)) {
                CreateGameScreen(model) { exits++ }
            }
        } }
    }
    @Before fun setup() { compose.activityRule.scenario.onActivity(::show); compose.waitForIdle() }
    private fun act(action: WizardAction) { compose.runOnIdle { model.act(action) }; compose.waitForIdle() }
    private fun complete() {
        act(WizardAction.Name("  Animals  ")); act(WizardAction.Photos)
        ids.forEach { act(WizardAction.Photo(it)) }; act(WizardAction.Done); act(WizardAction.AddQuestion)
        val q = model.state.questions.single().id
        act(WizardAction.EditImage(q)); act(WizardAction.Photo(ids[0])); act(WizardAction.Done)
        act(WizardAction.EditSound(q)); act(WizardAction.Sound(sound.id)); act(WizardAction.Done)
    }
    private fun openEdit(): CustomGame {
        val game = CustomGame("whats-that-existing", "Original", ids,
            listOf(CustomQuestion(ids[0], sound.id), CustomQuestion(ids[0], secondSound.id)))
        stored = game; editId = game.id
        compose.activityRule.scenario.onActivity(::show); compose.waitForIdle()
        return game
    }
    @Test fun editLoadsExistingValuesAndRevertedChangesExitWithoutConfirmation() {
        val original = openEdit()
        compose.onNodeWithText("Edit Game").assertIsDisplayed()
        assertEquals(original.imageIds, model.state.imageIds)
        assertEquals(original.questions.map { it.soundId }, model.state.questions.map { it.soundId })
        assertFalse(model.state.changed)
        act(WizardAction.Name("Changed")); act(WizardAction.Name(original.name))
        act(WizardAction.EditImage(model.state.questions[0].id)); act(WizardAction.Photo(ids[0])); act(WizardAction.Done)
        act(WizardAction.Photos); act(WizardAction.Photo(ids[2])); act(WizardAction.Done)
        assertTrue(model.state.changed)
        act(WizardAction.Photos); act(WizardAction.Photo(ids[2])); act(WizardAction.Done)
        assertFalse(model.state.changed)
        act(WizardAction.Cancel)
        assertEquals(1, exits); assertTrue(updates.isEmpty()); assertEquals(original, stored)
    }
    @Test fun failedEditSaveStaysOpenAndGuardedRetryUpdatesTheSameGame() {
        val original = openEdit()
        act(WizardAction.Name("Renamed")); fail = true
        act(WizardAction.Save)
        compose.onNodeWithText("Couldn't save this game. Check available storage and selected media, then try Save again.").assertIsDisplayed()
        assertEquals(original, stored); assertEquals(0, exits)
        assertEquals("Renamed", model.state.name)
        fail = false; gate = CompletableDeferred()
        act(WizardAction.Save); act(WizardAction.Save); act(WizardAction.Back)
        assertEquals(2, updates.size); assertTrue(model.state.saving)
        compose.runOnIdle { gate!!.complete(Unit) }; compose.waitForIdle()
        assertEquals(original.copy(name = "Renamed"), stored)
        assertTrue(saves.isEmpty()); assertEquals(1, exits)
    }
    @Test fun editRotationPreservesChangesAndDiscardDoesNotWrite() {
        val original = openEdit()
        act(WizardAction.Name("Changed"))
        compose.activityRule.scenario.recreate(); compose.activityRule.scenario.onActivity(::show); compose.waitForIdle()
        assertEquals("Changed", model.state.name)
        act(WizardAction.Back)
        compose.onNodeWithText("Discard changes?").assertIsDisplayed()
        compose.onNodeWithText("Discard").performClick(); compose.waitForIdle()
        assertEquals(original, stored); assertTrue(updates.isEmpty()); assertEquals(1, exits)
    }
    @Test fun emptyLibrariesAllowEntryAndExplainMissingContent() {
        compose.runOnIdle { photos.photos.value = emptyList(); sounds.sounds.value = emptyList() }
        compose.onNodeWithText("Create Game").assertIsDisplayed()
        compose.onNodeWithContentDescription("Back").assertDoesNotExist()
        compose.onNodeWithText("Continue").assertDoesNotExist()
        compose.onNodeWithText("Add Question").assertDoesNotExist()
        compose.onNodeWithText("Save").assertIsNotEnabled()
        compose.onNodeWithTag("bottom-navigation").assertDoesNotExist()
        compose.onNodeWithText("Select photos").performClick()
        compose.onNodeWithTag("game-media-modal").assertIsDisplayed()
        compose.onNodeWithText("No photos yet. Add photos to your Library before choosing images for this game.").assertIsDisplayed()
        act(WizardAction.Back)
        compose.runOnIdle { photos.photos.value = ids.map { LibraryPhoto(it, 1) } }
        act(WizardAction.Name("Family")); act(WizardAction.Photos)
        ids.take(2).forEach { act(WizardAction.Photo(it)) }; act(WizardAction.Done)
        assertTrue(model.state.questions.isEmpty())
        compose.onNodeWithText("Add Question").performScrollTo().assertIsDisplayed().performClick()
        act(WizardAction.EditSound(model.state.questions.single().id))
        compose.onNodeWithText("No sounds yet. Add sounds in Library to choose a sound for this question.").assertIsDisplayed()
    }
    @Test fun selectorCancellationUsedPhotosAndQuestionLimitAreEnforced() {
        complete()
        act(WizardAction.Photos); act(WizardAction.Photo(ids[0]))
        assertEquals(WizardError.USED_PHOTO, model.state.error)
        act(WizardAction.Photo(ids[2])); act(WizardAction.Back)
        assertEquals(3, model.state.imageIds.size)
        repeat(12) { act(WizardAction.AddQuestion) }
        assertEquals(10, model.state.questions.size)
        assertFalse(model.state.valid)
        repeat(9) { act(WizardAction.RemoveQuestion(model.state.questions.last().id)) }
        assertEquals(1, model.state.questions.size)
        assertTrue(model.state.valid)
        compose.onNodeWithContentDescription("Delete question 1").performScrollTo().performClick()
        assertTrue(model.state.questions.isEmpty())
        assertFalse(model.state.valid)
    }
    @Test fun rotationAndDiscardKeepDraftAndStopPlayback() {
        complete(); act(WizardAction.Preview(sound.assetId))
        val before = model.state
        compose.activityRule.scenario.recreate(); compose.activityRule.scenario.onActivity(::show)
        compose.waitForIdle()
        assertEquals(before, model.state)
        assertFalse(player.playback.value.playing)
        act(WizardAction.Back)
        compose.onNodeWithText("Discard this game?").assertIsDisplayed()
        compose.onNodeWithText("Keep Editing").performClick()
        assertEquals(before.questions, model.state.questions)
        act(WizardAction.Cancel)
        compose.onNodeWithText("Discard").performClick()
        compose.waitForIdle(); assertEquals(1, exits)
    }
    @Test fun saveFailureRetainsDraftAndRetryIsGuardedAndUsesStableIdentity() {
        complete(); fail = true
        act(WizardAction.Save)
        assertEquals(WizardError.SAVE, model.state.error); assertTrue(model.state.valid); assertEquals(0, exits)
        fail = false; gate = CompletableDeferred()
        act(WizardAction.Save); act(WizardAction.Save); act(WizardAction.Back)
        assertTrue(model.state.saving); assertEquals(2, saves.size)
        assertEquals(saves[0].id, saves[1].id); assertEquals("Animals", saves[1].name)
        compose.runOnIdle { gate!!.complete(Unit) }
        compose.waitForIdle(); assertEquals(1, exits)
    }
    @Test fun untouchedExitNeedsNoConfirmation() {
        act(WizardAction.Back); assertFalse(model.state.discard); assertEquals(1, exits)
    }
    @Test fun photoMinimumAppliesAfterReachingTwoAndCancelDiscardsPendingSelection() {
        act(WizardAction.AddQuestion)
        assertTrue(model.state.questions.isEmpty())
        act(WizardAction.Photos)
        compose.onNodeWithContentDescription(ids[0]).performClick().assertIsSelected()
        compose.onNodeWithContentDescription(ids[0]).performClick().assertIsNotSelected()
        assertTrue(model.state.pendingImages.isEmpty())
        ids.take(2).forEach { act(WizardAction.Photo(it)) }
        act(WizardAction.Photo(ids[0]))
        assertEquals(WizardError.MIN_PHOTOS, model.state.error)
        assertEquals(2, model.state.pendingImages.size)
        compose.onNodeWithContentDescription("Close").performClick()
        assertTrue(model.state.imageIds.isEmpty())
        assertFalse(model.state.changed)
        act(WizardAction.Photos)
        ids.take(2).forEach { act(WizardAction.Photo(it)) }; act(WizardAction.Done)
        assertEquals("", model.state.name)
        compose.onNodeWithText("Add Question").performScrollTo().assertIsEnabled()
        assertTrue(model.state.questions.isEmpty())
        act(WizardAction.Photos); act(WizardAction.Photo(ids[1]))
        assertEquals(WizardError.MIN_PHOTOS, model.state.error)
        act(WizardAction.Back)
        assertFalse(model.state.discard)
        assertEquals(2, model.state.imageIds.size)
    }
    @Test fun replacementAndQuestionDeletionReleaseOnlyUnsharedPhotoReferences() {
        complete()
        val first = model.state.questions.single().id
        act(WizardAction.AddQuestion)
        val second = model.state.questions.last().id
        act(WizardAction.EditImage(second)); act(WizardAction.Photo(ids[0])); act(WizardAction.Done)
        act(WizardAction.EditSound(second)); act(WizardAction.Sound(sound.id)); act(WizardAction.Done)
        act(WizardAction.EditImage(first)); act(WizardAction.Photo(ids[1]))
        assertEquals(ids[0], model.state.questions.first().imageId)
        act(WizardAction.Cancel)
        assertEquals(ids[0], model.state.questions.first().imageId)
        act(WizardAction.EditImage(first)); act(WizardAction.Photo(ids[1])); act(WizardAction.Done)
        act(WizardAction.Photos); act(WizardAction.Photo(ids[0]))
        assertEquals(WizardError.USED_PHOTO, model.state.error)
        act(WizardAction.Cancel); act(WizardAction.RemoveQuestion(second))
        act(WizardAction.Photos); act(WizardAction.Photo(ids[0])); act(WizardAction.Done)
        assertEquals(setOf(ids[1], ids[2]), model.state.imageIds.toSet())
        assertTrue(model.state.valid)
    }
    @Test fun compactSoundCheckboxSelectionIsPendingAndPreviewDoesNotSelect() {
        complete()
        val question = model.state.questions.single().id
        compose.onNodeWithText("Edit photos selection").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Replace sound").assertDoesNotExist()
        compose.onNodeWithContentDescription("Replace sound").performScrollTo().performClick()
        compose.onNodeWithTag("game-media-modal").assertIsDisplayed()
        val checkbox = compose.onNodeWithTag("select-sound-${secondSound.id}")
        val preview = compose.onNodeWithContentDescription("Preview ${secondSound.name}")
        assertTrue(checkbox.fetchSemanticsNode().boundsInRoot.left > preview.fetchSemanticsNode().boundsInRoot.right)
        preview.performClick()
        assertEquals(sound.id, model.state.pendingChoice)
        checkbox.performClick().assertIsOn()
        checkbox.performClick().assertIsOn()
        assertEquals(sound.id, model.state.questions.single().soundId)
        act(WizardAction.Name("Must not change behind modal"))
        assertEquals("  Animals  ", model.state.name)
        compose.onNode(hasText("Cancel") and isEnabled()).performClick()
        assertEquals(sound.id, model.state.questions.single().soundId)
        assertFalse(player.playback.value.playing)
        act(WizardAction.EditSound(question)); act(WizardAction.Sound(secondSound.id)); act(WizardAction.Done)
        assertEquals(secondSound.id, model.state.questions.single().soundId)
        act(WizardAction.EditSound(question)); act(WizardAction.Sound(sound.id))
        androidx.test.espresso.Espresso.pressBack()
        compose.waitForIdle()
        assertNull(model.state.selector)
        assertEquals(secondSound.id, model.state.questions.single().soundId)
        assertFalse(model.state.discard)
    }
    @Test fun modalPendingStateSurvivesRotationAndBackdropCancelsOnlyTheModal() {
        act(WizardAction.Photos)
        ids.take(2).forEach { act(WizardAction.Photo(it)) }
        val before = model.state
        compose.activityRule.scenario.recreate(); compose.activityRule.scenario.onActivity(::show)
        compose.waitForIdle()
        assertEquals(before, model.state)
        compose.onNodeWithTag("game-media-modal").assertIsDisplayed()
        compose.onNodeWithTag("game-media-backdrop").performTouchInput { click(androidx.compose.ui.geometry.Offset(1f, center.y)) }
        compose.onNodeWithTag("game-media-modal").assertDoesNotExist()
        assertNull(model.state.selector)
        assertTrue(model.state.imageIds.isEmpty())
        assertFalse(model.state.discard)
    }
    @Test fun nameRemainsEditableWithEnlargedTextAndLandscapeKeyboard() {
        compose.runOnIdle { enlarged.value = true }
        compose.activityRule.scenario.onActivity { it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE }
        try {
            compose.waitUntil(5000) { compose.activity.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE }
            compose.activityRule.scenario.onActivity(::show)
            compose.onNode(hasSetTextAction()).performClick()
            compose.waitUntil(5000) {
                ViewCompat.getRootWindowInsets(compose.activity.window.decorView)?.isVisible(WindowInsetsCompat.Type.ime()) == true
            }
            compose.onNode(hasSetTextAction()).assertIsDisplayed().performTextInput("Family")
            assertEquals("Family", model.state.name)
            compose.onNode(hasSetTextAction()).performImeAction()
            compose.onNodeWithText("Save").assertIsDisplayed()
        } finally {
            androidx.test.espresso.Espresso.closeSoftKeyboard()
            compose.activityRule.scenario.onActivity { it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED }
        }
    }
}
