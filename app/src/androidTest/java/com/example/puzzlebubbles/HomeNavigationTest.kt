package com.example.puzzlebubbles

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
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
    fun eachCardOpensItsPlaceholderAndVisibleBackReturnsHome() {
        for (title in listOf("What's That?", "Match Up")) {
            openGame(title)
            compose.onNodeWithText("Back").performClick()
            assertHome()
        }
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
            compose.onNodeWithText("Game coming soon").assertIsDisplayed()
            pressBack()
            assertHome()
        }
    }

    private fun openGame(title: String) {
        compose.onNodeWithText(title).performScrollTo().performClick()
        compose.onNodeWithText(title).assertIsDisplayed()
        compose.onNodeWithText("Game coming soon").assertIsDisplayed()
        val description = if (title == "What's That?") {
            "Listen and choose a picture."
        } else {
            "Find the pictures that go together."
        }
        compose.onNodeWithText(description).assertIsDisplayed()
        compose.onNodeWithText("Puzzle Bubbles").assertDoesNotExist()
    }

    private fun assertHome() {
        for (title in listOf("What's That?", "Match Up")) {
            compose.onNodeWithText(title).performScrollTo().assertIsDisplayed().assertHasClickAction()
        }
        compose.onNodeWithText("Game coming soon").assertDoesNotExist()
    }
}
