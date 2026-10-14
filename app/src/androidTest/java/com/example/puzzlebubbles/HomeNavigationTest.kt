package com.example.puzzlebubbles

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
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
