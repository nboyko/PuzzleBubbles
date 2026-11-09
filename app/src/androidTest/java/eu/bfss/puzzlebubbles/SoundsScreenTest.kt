package eu.bfss.puzzlebubbles

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.activity.compose.setContent
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.text.TextRange
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import eu.bfss.puzzlebubbles.domain.*
import eu.bfss.puzzlebubbles.presentation.library.*
import eu.bfss.puzzlebubbles.ui.theme.PuzzleBubblesTheme
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.IOException

class SoundsScreenTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private lateinit var model: SoundsViewModel
    private val repository = FakeRepository()
    private val recorder = FakeRecorder()
    private val player = FakePlayer()
    private var clock = 0L
    private var home = 0
    private var back = 0

    private class FakeRepository : SoundRepository {
        override val sounds = MutableStateFlow<List<LibrarySound>>(emptyList())
        var saves = 0; var deletes = 0; var failSave = false
        var saveGate: CompletableDeferred<Unit>? = null
        var deleteGate: CompletableDeferred<Unit>? = null
        override suspend fun refresh() = Unit
        override suspend fun save(draft: SoundDraft, name: String): LibrarySound = withContext(NonCancellable) {
            saves++; saveGate?.await()
            if (failSave) throw IOException("disk full")
            LibrarySound("saved-$saves", draft.durationMs, saves.toLong(), name).also { sounds.value = listOf(it) + sounds.value }
        }
        override suspend fun delete(id: String) { deletes++; deleteGate?.await(); sounds.value = sounds.value.filterNot { it.id == id } }
    }
    private class FakeRecorder : SoundRecorder {
        var starts = 0; var finishes = 0; var discards = 0; var failFinish = false
        var finishGate: CompletableDeferred<Unit>? = null
        var startGate: CompletableDeferred<Unit>? = null
        var limit: () -> Unit = {}; var error: () -> Unit = {}
        override suspend fun start(onLimit: () -> Unit, onError: () -> Unit) {
            starts++; limit = onLimit; error = onError; startGate?.await()
        }
        override suspend fun finish(): SoundDraft {
            finishes++; finishGate?.await()
            if (failFinish) throw IOException("No audio")
            return SoundDraft("sound-draft:example", 3000)
        }
        override suspend fun discard() { discards++ }
    }
    private class FakePlayer : SoundPlayback {
        override val playback = MutableStateFlow(SoundPlaybackState())
        var toggles = 0; var stops = 0
        override fun toggle(assetId: String) {
            toggles++
            playback.value = SoundPlaybackState(assetId, 1000, 3000,
                playing = playback.value.assetId != assetId || !playback.value.playing)
        }
        override fun stop() { stops++; playback.value = SoundPlaybackState() }
        override fun release() = stop()
    }
    private fun show(activity: MainActivity) {
        model = ViewModelProvider(activity, object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                modelClass.cast(SoundsViewModel(repository, recorder, player) { clock })!!
        })["sounds-test", SoundsViewModel::class.java]
        activity.setContent { PuzzleBubblesTheme { SoundsScreen(model, { back++ }, { home++ }, Modifier.fillMaxSize()) } }
    }
    @Before fun setup() {
        compose.activityRule.scenario.onActivity(::show)
        compose.waitUntil { !model.uiState.loading }
    }
    private fun open() { compose.onNodeWithContentDescription("Add Sound").performClick() }
    private fun start() {
        open()
        compose.runOnIdle { model.record(true) }
        compose.waitUntil { model.uiState.modal?.step == RecordingStep.RECORDING }
    }
    private fun preview() {
        start()
        compose.onNodeWithText("Stop").performClick()
        compose.waitUntil { model.uiState.modal?.step == RecordingStep.PREVIEW }
    }
    private fun saved() {
        preview(); compose.onNodeWithText("Save").performClick()
        compose.waitUntil { model.uiState.modal == null && !model.uiState.cleaning }
    }

    @Test fun emptyAddRecordAndHeaderPlusOpenTheSameIdleModalWithoutRecording() {
        compose.onNodeWithContentDescription("Add Sound").assertIsDisplayed().assertIsEnabled()
        compose.onNodeWithText("Add Record").assertIsDisplayed().assertIsEnabled().performClick()
        compose.onNodeWithTag("sound-modal").assertIsDisplayed()
        compose.onNodeWithText("Record").assertIsEnabled()
        assertEquals(RecordingStep.IDLE, model.uiState.modal!!.step)
        assertFalse(model.uiState.permissionPending)
        assertEquals(0, recorder.starts)
        compose.onNodeWithText("Add Record").assertDoesNotExist()
        compose.onNodeWithContentDescription("Add Sound").assertDoesNotExist()
        compose.onNodeWithContentDescription("Close").performClick()
        compose.waitUntil { !model.uiState.cleaning }
        compose.onNodeWithText("Add Record").assertIsDisplayed().assertIsEnabled()
        open()
        assertEquals(RecordingStep.IDLE, model.uiState.modal!!.step)
        assertFalse(model.uiState.permissionPending)
        assertEquals(0, recorder.starts)
    }
    @Test fun emptyAddRecordRemainsVisibleAndOpensTheModalInLandscape() {
        compose.activityRule.scenario.onActivity { it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE }
        try {
            compose.waitUntil(timeoutMillis = 5000) {
                compose.activity.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
            }
            compose.activityRule.scenario.onActivity(::show)
            compose.onNodeWithText("Add Record").assertIsDisplayed().assertIsEnabled().performClick()
            compose.onNodeWithTag("sound-modal").assertIsDisplayed()
            assertEquals(RecordingStep.IDLE, model.uiState.modal!!.step)
            assertEquals(0, recorder.starts)
        } finally {
            compose.activityRule.scenario.onActivity { it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED }
        }
    }
    @Test fun openingDoesNotRequestMicrophoneAndStalePermissionCannotStartRecording() {
        open()
        assertEquals(0, recorder.starts)
        assertFalse(model.uiState.permissionPending)
        compose.runOnIdle {
            assertTrue(model.record(false))
            assertFalse(model.record(false))
            model.close()
        }
        compose.waitUntil { !model.uiState.cleaning }
        open()
        compose.onNodeWithText("Record").assertIsNotEnabled()
        compose.runOnIdle { model.permissionResult(true, false) }
        assertEquals(0, recorder.starts)
        assertEquals(RecordingStep.IDLE, model.uiState.modal!!.step)
        compose.onNodeWithText("Record").assertIsEnabled()
    }
    @Test fun deniedPermissionStaysUsableAndOffersSettingsWithoutStarting() {
        open()
        compose.runOnIdle { model.record(false); model.permissionResult(false, true) }
        compose.onNodeWithText("Microphone access is needed to record a sound.").assertIsDisplayed()
        compose.onNodeWithText("Open Settings").assertIsDisplayed()
        assertEquals(0, recorder.starts)
        compose.onNodeWithContentDescription("Close").performClick()
        compose.onNodeWithTag("sound-modal").assertDoesNotExist()
    }
    @Test fun manualStopAndLimitCallbackFinalizeOnceAndNeverAutosave() {
        start()
        recorder.finishGate = CompletableDeferred()
        compose.runOnIdle { model.stopRecording(); recorder.limit(); model.stopRecording(); model.record(true) }
        compose.waitUntil { recorder.finishes == 1 }
        assertEquals(1, recorder.starts)
        compose.runOnIdle { recorder.finishGate!!.complete(Unit) }
        compose.waitUntil { model.uiState.modal?.step == RecordingStep.PREVIEW }
        assertEquals(0, repository.saves)
        compose.onNodeWithText("Save").assertIsEnabled()
    }
    @Test fun rotationKeepsDeadlineAndFiveSecondClockMovesToUnsavedPreview() {
        start()
        compose.runOnIdle { clock = 3000 }
        compose.waitUntil { model.uiState.modal?.elapsedMs == 3000L }
        compose.activityRule.scenario.recreate()
        compose.activityRule.scenario.onActivity(::show)
        assertEquals(1, recorder.starts)
        assertEquals(RecordingStep.RECORDING, model.uiState.modal!!.step)
        compose.runOnIdle { clock = 5000 }
        compose.waitUntil { model.uiState.modal?.step == RecordingStep.PREVIEW }
        assertEquals(1, recorder.finishes)
        assertEquals(0, repository.saves)
    }
    @Test fun saveIsExplicitGuardedAcrossRotationAndBackgroundAndKeepsOneItem() {
        preview()
        repository.saveGate = CompletableDeferred()
        compose.onNodeWithText("Save").performClick()
        compose.runOnIdle { model.save(); model.close(); model.background() }
        compose.onNodeWithText("Save").assertIsNotEnabled()
        compose.onNodeWithContentDescription("Close").assertIsNotEnabled()
        compose.activityRule.scenario.recreate()
        compose.activityRule.scenario.onActivity(::show)
        assertEquals(1, repository.saves)
        compose.runOnIdle { repository.saveGate!!.complete(Unit) }
        compose.waitUntil { model.uiState.modal == null && model.uiState.sounds.size == 1 }
        compose.onNodeWithTag("sounds-list").assertIsDisplayed()
        assertEquals(1, repository.saves)
    }
    @Test fun failedSaveAfterBackgroundDiscardsTheUnsavedDraftWithoutRetrying() {
        preview()
        repository.failSave = true
        repository.saveGate = CompletableDeferred()
        compose.onNodeWithText("Save").performClick()
        compose.waitUntil { repository.saves == 1 }
        compose.runOnIdle { model.background(); repository.saveGate!!.complete(Unit) }
        compose.waitUntil { model.uiState.modal == null && !model.uiState.cleaning }
        assertEquals(1, recorder.discards)
        assertTrue(repository.sounds.value.isEmpty())
        compose.runOnIdle { model.foreground() }
        assertNull(model.uiState.modal)
        assertEquals(1, repository.saves)
        assertEquals(SoundError.SAVE, model.uiState.error)
    }
    @Test fun backgroundDiscardsRecordingAndPreviewWithoutSavingOrRestarting() {
        for (inPreview in listOf(false, true)) {
            if (inPreview) preview() else start()
            compose.runOnIdle { model.background(); recorder.limit(); recorder.error() }
            compose.waitUntil { !model.uiState.cleaning }
            assertNull(model.uiState.modal)
            compose.runOnIdle { model.foreground() }
        }
        assertEquals(2, recorder.starts)
        assertEquals(0, repository.saves)
        assertTrue(repository.sounds.value.isEmpty())
    }
    @Test fun destroyedOwnerWaitsForCommittingSaveBeforeDiscardingItsSource() {
        preview()
        repository.saveGate = CompletableDeferred()
        compose.onNodeWithText("Save").performClick()
        compose.waitUntil { repository.saves == 1 }
        compose.runOnIdle { compose.activity.viewModelStore.clear() }
        assertEquals(0, recorder.discards)
        compose.runOnIdle { repository.saveGate!!.complete(Unit) }
        compose.waitUntil { recorder.discards == 1 }
        assertEquals(1, repository.sounds.value.size)
        assertEquals(1, repository.saves)
    }
    @Test fun selectedDefaultCanBeReplacedAndNameSurvivesRotationAndFailedSave() {
        preview()
        compose.onNodeWithTag("recording-name")
            .assertTextContains("Track 1")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.TextSelectionRange, TextRange(0, 7)))
            .performTextInput("  Bird song  ")
        assertEquals("  Bird song  ", model.uiState.modal!!.name)
        compose.activityRule.scenario.recreate()
        compose.activityRule.scenario.onActivity(::show)
        compose.onNodeWithTag("recording-name").assertTextContains("  Bird song  ")
        repository.failSave = true
        compose.onNodeWithText("Save").performClick()
        compose.waitUntil { model.uiState.modal?.error == SoundError.SAVE }
        compose.onNodeWithTag("recording-name").assertTextContains("  Bird song  ")
        repository.failSave = false
        compose.onNodeWithText("Save").performClick()
        compose.waitUntil { model.uiState.modal == null }
        assertEquals("Bird song", repository.sounds.value.single().name)
        compose.onNodeWithText("Bird song").assertIsDisplayed()
    }
    @Test fun defaultsUseSavedCountAndBlankFallsBackWhileDuplicatesAreAllowed() {
        saved()
        assertEquals("Track 1", repository.sounds.value.single().name)
        preview()
        compose.onNodeWithTag("recording-name").assertTextContains("Track 2").performTextReplacement("   ")
        compose.onNodeWithText("Save").performClick()
        compose.waitUntil { model.uiState.modal == null && !model.uiState.cleaning }
        assertEquals("Track 2", repository.sounds.value.first().name)
        preview()
        compose.onNodeWithTag("recording-name").assertTextContains("Track 3").performTextReplacement("Track 1")
        compose.onNodeWithText("Save").performClick()
        compose.waitUntil { model.uiState.modal == null && !model.uiState.cleaning }
        assertEquals(2, repository.sounds.value.count { it.name == "Track 1" })
        compose.runOnIdle { model.requestDelete(repository.sounds.value.first().id); model.delete() }
        compose.waitUntil { !model.uiState.deleting }
        preview()
        compose.onNodeWithTag("recording-name").assertTextContains("Track 3")
    }
    @Test fun invalidShortRecordingReturnsToIdleAndSaveFailureRequiresExplicitRetry() {
        start()
        recorder.failFinish = true
        compose.onNodeWithText("Stop").performClick()
        compose.waitUntil { model.uiState.modal?.error == SoundError.RECORD && !model.uiState.cleaning }
        compose.onNodeWithText("Save").assertDoesNotExist()
        compose.onNodeWithText("Record").assertIsEnabled()
        compose.runOnIdle { model.close() }
        compose.waitUntil { !model.uiState.cleaning }
        recorder.failFinish = false
        preview()
        repository.failSave = true
        compose.onNodeWithText("Save").performClick()
        compose.waitUntil { model.uiState.modal?.error == SoundError.SAVE }
        assertNotNull(model.uiState.modal!!.draft)
        assertTrue(model.uiState.sounds.isEmpty())
        repository.failSave = false
        compose.onNodeWithText("Save").performClick()
        compose.waitUntil { model.uiState.modal == null }
        assertEquals(2, repository.saves)
        assertEquals(1, model.uiState.sounds.size)
    }
    @Test fun cancelAndBackDiscardWhileModalBlocksUnderlyingNavigation() {
        preview()
        compose.onNodeWithContentDescription("Play recording 1").performClick()
        compose.onNodeWithContentDescription("Pause recording 1").assertIsDisplayed()
        compose.onNodeWithText("Cancel").performClick()
        compose.waitUntil { !model.uiState.cleaning }
        assertFalse(player.playback.value.playing)
        open()
        compose.onNodeWithTag("sound-modal").performTouchInput { click(androidx.compose.ui.geometry.Offset(4f, 4f)) }
        compose.onNodeWithTag("sound-modal").assertIsDisplayed()
        compose.onNodeWithTag("sound-backdrop").performTouchInput { click(androidx.compose.ui.geometry.Offset(1f, 1f)) }
        compose.onNodeWithTag("sound-modal").assertDoesNotExist()
        compose.waitUntil { !model.uiState.cleaning }
        open()
        compose.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        compose.onNodeWithTag("sound-modal").assertDoesNotExist()
        assertEquals(0, home); assertEquals(0, back); assertEquals(0, repository.saves)
    }
    @Test fun deleteConfirmationAndRapidDeleteRemoveOneItemAndRestoreEmptyState() {
        saved()
        compose.onNodeWithText("Add Record").assertDoesNotExist()
        compose.onNodeWithContentDescription("Play recording 1").performClick()
        compose.onNodeWithContentDescription("Delete recording 1").performClick()
        assertFalse(player.playback.value.playing)
        compose.onNodeWithText("Cancel").performClick()
        assertEquals(0, repository.deletes)
        compose.onNodeWithContentDescription("Delete recording 1").performClick()
        repository.deleteGate = CompletableDeferred()
        compose.onNodeWithText("Delete").performClick()
        compose.runOnIdle { model.delete(); model.cancelDelete(); model.open() }
        compose.onNodeWithContentDescription("Close").assertIsNotEnabled()
        compose.runOnIdle { repository.deleteGate!!.complete(Unit) }
        compose.waitUntil { model.uiState.deleteId == null }
        compose.onNodeWithText("Add Record").assertIsDisplayed().assertIsEnabled()
        assertEquals(1, repository.deletes)
    }
    @Test fun cancellingStartupIgnoresItsLateCompletionAndReopeningIsFresh() {
        recorder.startGate = CompletableDeferred()
        startWithoutWaiting()
        compose.runOnIdle { model.close(); recorder.startGate!!.complete(Unit) }
        compose.waitUntil { !model.uiState.cleaning }
        assertNull(model.uiState.modal)
        assertEquals(0, repository.saves)
        recorder.startGate = null
        start()
        assertEquals(2, recorder.starts)
        assertEquals(RecordingStep.RECORDING, model.uiState.modal!!.step)
    }
    private fun startWithoutWaiting() { open(); compose.runOnIdle { model.record(true) } }
}
