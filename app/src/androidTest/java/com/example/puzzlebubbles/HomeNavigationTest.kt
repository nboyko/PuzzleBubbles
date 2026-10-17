package com.example.puzzlebubbles

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
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
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

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
        compose.onNodeWithText("No photos yet").assertIsDisplayed()
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
            if (index == 0) {
                compose.onNodeWithContentDescription("Back").assertIsDisplayed().performClick()
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
    fun libraryChildrenHaveEmptyStatesAndBothBackActionsReturnToLibrary() {
        openLibrary()
        for ((title, emptyText) in listOf("Photos" to "No photos yet", "Sounds" to "No sounds yet")) {
            for (systemBack in listOf(false, true)) {
                compose.onNodeWithText(title).performScrollTo().assertHasClickAction().performClick()
                compose.onNodeWithText(title).assertIsDisplayed()
                compose.onNodeWithText(emptyText).assertIsDisplayed()
                compose.onNodeWithTag("inline-home-navigation").assertDoesNotExist()
                compose.onNodeWithTag("bottom-navigation").assertIsDisplayed()
                if (systemBack) pressBack() else compose.onNodeWithContentDescription("Back").performClick()
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
        for ((title, emptyText) in listOf("Photos" to "No photos yet", "Sounds" to "No sounds yet")) {
            openLibrary()
            compose.onNodeWithText(title).performScrollTo().performClick()
            compose.activityRule.scenario.recreate()
            compose.onNodeWithText(title).assertIsDisplayed()
            compose.onNodeWithText(emptyText).assertIsDisplayed()
            pressBack()
            compose.onNodeWithText("My Library").assertIsDisplayed()
            pressBack()
            compose.onNodeWithTag("home-content").performScrollToIndex(0)
            compose.onNodeWithText("What would you like to play?").assertIsDisplayed()
        }
    }

    private fun openLibrary() {
        compose.onNode(hasText("Library") and hasAnyAncestor(hasTestTag("bottom-navigation")))
            .performClick()
        compose.onNodeWithText("My Library").assertIsDisplayed()
    }

    private fun openGame(title: String) {
        compose.onNodeWithText(title).performScrollTo().performClick()
        compose.onNodeWithText(title).assertIsDisplayed()
        if (title == "What's That?") {
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
