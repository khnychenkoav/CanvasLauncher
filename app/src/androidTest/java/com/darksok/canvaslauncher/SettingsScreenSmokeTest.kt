package com.darksok.canvaslauncher

import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.darksok.canvaslauncher.settings.SettingsActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsScreenSmokeTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<SettingsActivity>()

    @Test
    fun settingsScreenRenders() {
        composeRule.onRoot().assertExists()
    }

    @Test
    fun settingsOptionsAreClickable() {
        composeRule.clickByStringId(R.string.theme_mode_light)
        composeRule.clickByStringId(R.string.theme_mode_dark)
        composeRule.clickByStringId(R.string.layout_mode_circle)
        composeRule.clickByStringId(R.string.light_palette_mint_garden)
        composeRule.clickByStringId(R.string.dark_palette_deep_ocean)
    }

    private fun AndroidComposeTestRule<ActivityScenarioRule<SettingsActivity>, SettingsActivity>.clickByStringId(resId: Int) {
        val label = activity.getString(resId)
        val node = onNodeWithText(label, useUnmergedTree = true)
        runCatching { node.performScrollTo() }
        node.assertExists().performClick()
    }
}
