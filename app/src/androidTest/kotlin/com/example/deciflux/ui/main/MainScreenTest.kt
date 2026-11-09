package com.example.deciflux.ui.main

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.example.deciflux.ui.theme.DecifluxTheme
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/** UI tests for [com.example.deciflux.ui.main.MainScreen]. */
class MainScreenTest {

  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  @Before
  fun setup() {
    composeTestRule.setContent {
      DecifluxTheme {
        MainScreen()
      }
    }
  }

  @Test
  fun appTitle_andIntakeButtons_exist() {
    composeTestRule.onNodeWithText("Deciflux").assertExists()
    composeTestRule.onNodeWithText("Single").assertExists()
    composeTestRule.onNodeWithText("Files").assertExists()
    composeTestRule.onNodeWithText("Folder").assertExists()
  }
}
