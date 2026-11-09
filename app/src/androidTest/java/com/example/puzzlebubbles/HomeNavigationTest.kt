package com.example.puzzlebubbles

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.test.espresso.Espresso.pressBack
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlinx.coroutines.runBlocking
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class HomeNavigationTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun homeContainsBothClickableGames() {
        assertHome()
    }

    @Test
    fun eachCardOpensItsDestinationAndVisibleBackReturnsHome() {
        openGame("What's That?")
        compose.onNodeWithContentDescription("Replay sound").assertIsDisplayed()
        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithTag("whats-that-selection").assertIsDisplayed()
        compose.onNodeWithText("Home").performClick()
        assertHome()

        openGame("Match Up")
        compose.onNodeWithText("Game coming soon").assertIsDisplayed()
        compose.onNodeWithText("Back").performClick()
        assertHome()
    }

    @Test
    fun systemBackReturnsHomeFromEitherGame() {
        for (title in listOf("What's That?", "Match Up")) {
            openGame(title)
            pressBack()
            if (title == "What's That?") {
                compose.onNodeWithTag("whats-that-selection").assertIsDisplayed()
                pressBack()
            }
            assertHome()
        }
    }

    @Test
    fun selectedDestinationSurvivesActivityRecreation() {
        for (title in listOf("What's That?", "Match Up")) {
            openGame(title)
            compose.activityRule.scenario.recreate()
            compose.onNodeWithText(title).assertIsDisplayed()
            if (title == "What's That?") {
                compose.onNodeWithContentDescription("Replay sound").assertIsDisplayed()
                compose.onNodeWithText("Game coming soon").assertDoesNotExist()
            } else {
                compose.onNodeWithText("Game coming soon").assertIsDisplayed()
            }
            pressBack()
            if (title == "What's That?") {
                compose.onNodeWithTag("whats-that-selection").assertIsDisplayed()
                pressBack()
            }
            assertHome()
        }
    }

    @Test
    fun fixedNavigationOpensLibraryAndBackRestoresScrolledGames() {
        compose.onNodeWithTag("bottom-navigation").assertIsDisplayed()
        compose.onNodeWithTag("inline-home-navigation").assertDoesNotExist()
        compose.onNodeWithText("Match Up").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("bottom-navigation").assertIsDisplayed()
        compose.onNode(hasText("Library") and hasAnyAncestor(hasTestTag("bottom-navigation")))
            .performClick()
        compose.onNodeWithText("My Library").assertIsDisplayed()
        compose.onNodeWithText("Game coming soon").assertDoesNotExist()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithText("My Library").assertIsDisplayed()
        compose.onNodeWithText("Photos").performScrollTo().performClick()
        assertMediaReady("Photos")
        pressBack()
        pressBack()
        compose.onNodeWithText("Match Up").assertIsDisplayed()
        compose.onNodeWithTag("bottom-navigation").assertIsDisplayed()
        compose.onNodeWithTag("home-content").performScrollToIndex(0)
        compose.onNodeWithTag("bottom-navigation").assertIsDisplayed()
    }

    @Test
    fun libraryKeepsBottomNavigationAndReturnsHomeThroughBackOrHome() {
        repeat(3) { index ->
            compose.onNode(hasText("Library") and hasAnyAncestor(hasTestTag("bottom-navigation")))
                .performClick()
            compose.onNodeWithText("My Library").assertIsDisplayed()
            compose.onNodeWithText("Puzzle Bubbles").assertDoesNotExist()
            compose.onNodeWithTag("inline-home-navigation").assertDoesNotExist()
            compose.onNodeWithTag("bottom-navigation").assertIsDisplayed()
            compose.onNodeWithText("Library").performClick()
            compose.onNodeWithText("My Library").assertIsDisplayed()
            compose.onNodeWithText("Back").assertDoesNotExist()
            compose.onNodeWithContentDescription("Back").assertDoesNotExist()
            if (index == 0) {
                compose.onNode(hasText("Home") and hasAnyAncestor(hasTestTag("bottom-navigation"))).performClick()
            } else if (index == 1) {
                pressBack()
            } else {
                compose.onNodeWithText("Home").performClick()
            }
            compose.onNodeWithText("My Library").assertDoesNotExist()
            compose.onNodeWithText("What would you like to play?").assertIsDisplayed()
        }
    }

    @Test
    fun libraryChildrenLoadAndBothBackActionsReturnToLibrary() {
        openLibrary()
        for (title in listOf("Photos", "Sounds")) {
            for (systemBack in listOf(false, true)) {
                compose.onNodeWithText(title).performScrollTo().assertHasClickAction().performClick()
                compose.onNodeWithText(title).assertIsDisplayed()
                assertMediaReady(title)
                compose.onNodeWithTag("inline-home-navigation").assertDoesNotExist()
                compose.onNodeWithTag("bottom-navigation").assertIsDisplayed()
                compose.onNodeWithContentDescription("Back").assertDoesNotExist()
                compose.onNode(hasText("Library") and hasAnyAncestor(hasTestTag("bottom-navigation"))).assertIsNotSelected()
                if (systemBack) pressBack()
                else compose.onNode(hasText("Library") and hasAnyAncestor(hasTestTag("bottom-navigation"))).performClick()
                compose.onNodeWithText("My Library").assertIsDisplayed()
            }
        }
    }

    @Test
    fun mediaNavigationReturnsToLibraryOrHomeWithoutExtraDestinations() {
        for (title in listOf("Photos", "Sounds")) {
            openLibrary()
            compose.onNodeWithText(title).performScrollTo().performClick()
            compose.onNodeWithText("Library").performClick()
            compose.onNodeWithText("My Library").assertIsDisplayed()
            compose.onNodeWithText(title).performScrollTo().performClick()
            compose.activityRule.scenario.recreate()
            compose.onNodeWithTag("bottom-navigation").assertIsDisplayed()
            compose.onNodeWithText("Home").performClick()
            compose.onNodeWithTag("home-content").performScrollToIndex(0)
            compose.onNodeWithText("What would you like to play?").assertIsDisplayed()
        }
    }

    @Test
    fun libraryChildDestinationsSurviveRecreationWithoutExtraBackStackEntries() {
        for (title in listOf("Photos", "Sounds")) {
            openLibrary()
            compose.onNodeWithText(title).performScrollTo().performClick()
            compose.activityRule.scenario.recreate()
            compose.onNodeWithText(title).assertIsDisplayed()
            assertMediaReady(title)
            pressBack()
            compose.onNodeWithText("My Library").assertIsDisplayed()
            pressBack()
            compose.onNodeWithTag("home-content").performScrollToIndex(0)
            compose.onNodeWithText("What would you like to play?").assertIsDisplayed()
        }
    }

    @Test
    fun photosNavigationAndRecreationWorkWithSavedContent() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<PuzzleBubblesApplication>()
        val draft = app.photoImages.putDraft(UUID.randomUUID().toString(),
            Bitmap.createBitmap(1024, 1024, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.CYAN) })
        var savedId: String? = null
        try {
            app.photoRepository.refresh()
            savedId = app.photoRepository.save(draft).id
            openLibrary()
            compose.onNodeWithText("Photos").performScrollTo().performClick()
            assertMediaReady("Photos")
            compose.onNodeWithTag("photos-grid").assertIsDisplayed()
            compose.activityRule.scenario.recreate()
            assertMediaReady("Photos")
            compose.onNodeWithTag("photos-grid").assertIsDisplayed()
            pressBack()
            compose.onNodeWithText("My Library").assertIsDisplayed()
            pressBack()
            assertHome()
        } finally {
            savedId?.let { app.photoRepository.delete(it) } // Remove only this test's photo.
            app.photoImages.discard(draft)
        }
    }

    private fun assertMediaReady(title: String) {
        // Both libraries may contain persisted content. Wait for storage loading
        // before checking navigation; isolated screen tests cover empty/list states.
        val add = if (title == "Photos") "Add Photo" else "Add Sound"
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodes(hasContentDescription(add) and isEnabled())
                .fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithContentDescription(add).assertIsDisplayed().assertIsEnabled()
    }

    private fun openLibrary() {
        compose.onNode(hasText("Library") and hasAnyAncestor(hasTestTag("bottom-navigation")))
            .performClick()
        compose.onNodeWithText("My Library").assertIsDisplayed()
        compose.waitForIdle()
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        Thread.sleep(250) // Allow the native Surface to present the new destination.
        val screenshot = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        try {
            java.io.File(compose.activity.externalCacheDir, "library-violet.png").outputStream().use {
                screenshot.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
            }
        } finally { screenshot.recycle() }
    }

    private fun openGame(title: String) {
        compose.onNodeWithText(title).performScrollTo().performClick()
        if (title == "What's That?") {
            compose.onNodeWithTag("whats-that-selection").assertIsDisplayed()
            compose.onNodeWithTag("bottom-navigation").assertIsDisplayed()
            compose.onNodeWithContentDescription("Replay sound").assertDoesNotExist()
            compose.onNodeWithContentDescription("Play Animals").performClick()
            compose.onNodeWithText(title).assertIsDisplayed()
            compose.onNodeWithContentDescription("Replay sound").assertIsDisplayed()
            compose.onNodeWithText("Game coming soon").assertDoesNotExist()
        } else {
            compose.onNodeWithText("Game coming soon").assertIsDisplayed()
            compose.onNodeWithText("Find the pictures that go together.").assertIsDisplayed()
        }
        compose.onNodeWithText("Puzzle Bubbles").assertDoesNotExist()
    }

    private fun assertHome() {
        for (title in listOf("What's That?", "Match Up")) {
            compose.onNodeWithText(title).performScrollTo().assertIsDisplayed().assertHasClickAction()
        }
        compose.onNodeWithText("Game coming soon").assertDoesNotExist()
    }
}
