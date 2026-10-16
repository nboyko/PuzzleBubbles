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
        compose.onNodeWithText("Match Up").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("bottom-navigation").assertIsDisplayed()
        compose.onNode(hasText("Library") and hasAnyAncestor(hasTestTag("bottom-navigation")))
            .performClick()
        compose.onNodeWithText("Library is coming soon.").assertIsDisplayed()
        compose.onNodeWithText("Game coming soon").assertDoesNotExist()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithText("Library is coming soon.").assertIsDisplayed()
        pressBack()
        compose.onNodeWithText("Match Up").assertIsDisplayed()
        compose.onNodeWithTag("bottom-navigation").assertIsDisplayed()
        compose.onNodeWithTag("home-content").performScrollToIndex(0)
        compose.onNodeWithTag("bottom-navigation").assertIsDisplayed()
    }

    @Test
    fun libraryVisibleBackAndHomeNavigationReturnToHome() {
        repeat(2) { index ->
            compose.onNode(hasText("Library") and hasAnyAncestor(hasTestTag("bottom-navigation")))
                .performClick()
            compose.onNodeWithText("Library is coming soon.").assertIsDisplayed()
            if (index == 0) {
                compose.onNodeWithText("Back").performClick()
            } else {
                compose.onNodeWithText("Home").performClick()
            }
            compose.onNodeWithText("Library is coming soon.").assertDoesNotExist()
            compose.onNodeWithText("What would you like to play?").assertIsDisplayed()
        }
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
