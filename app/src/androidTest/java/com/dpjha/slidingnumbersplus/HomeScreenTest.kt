package com.dpjha.slidingnumbersplus

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test

class HomeScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun homeShowsCoreNavigationAndProgress() {
        composeRule.onNodeWithText("SLIDING").assertIsDisplayed()
        composeRule.onNodeWithText("CITY LEVELS").assertIsDisplayed()
        composeRule.onNodeWithText("SETTINGS").assertIsDisplayed()
    }
}
