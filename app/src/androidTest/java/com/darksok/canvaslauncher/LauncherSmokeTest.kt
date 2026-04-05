package com.darksok.canvaslauncher

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LauncherSmokeTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun launcherScreenRenders() {
        composeRule.onRoot().assertExists()
    }

    @Test
    fun launcherPermissionGateButtonsAreVisibleWhenGateIsShown() {
        val grantLabel = composeRule.activity.getString(
            com.darksok.canvaslauncher.feature.launcher.R.string.permissions_gate_grant_button,
        )
        val settingsLabel = composeRule.activity.getString(
            com.darksok.canvaslauncher.feature.launcher.R.string.permissions_gate_settings_button,
        )

        val grantVisible = composeRule.onAllNodesWithText(grantLabel, useUnmergedTree = true)
            .fetchSemanticsNodes().isNotEmpty()
        if (grantVisible) {
            composeRule.onNodeWithText(grantLabel, useUnmergedTree = true)
                .assertExists()
            composeRule.onNodeWithText(settingsLabel, useUnmergedTree = true)
                .assertExists()
        } else {
            composeRule.onRoot().assertExists()
        }
    }
}
