package com.darksok.canvaslauncher

import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.ext.junit.rules.ActivityScenarioRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DefaultScreenSmokeTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<DefaultActivity>()

    @Test
    fun defaultScreenRenders() {
        composeRule.onRoot().assertExists()
    }

    @Test
    fun defaultScreenPrimaryButtonsAreClickable() {
        composeRule.assertVisibleByStringId(R.string.default_make_launcher_button)
        composeRule.assertVisibleByStringId(R.string.default_open_settings_button)
        composeRule.assertVisibleByStringId(R.string.default_open_canvas_button)
    }

    private fun AndroidComposeTestRule<ActivityScenarioRule<DefaultActivity>, DefaultActivity>.assertVisibleByStringId(resId: Int) {
        val label = activity.getString(resId)
        onNodeWithText(label).assertExists()
    }
}
