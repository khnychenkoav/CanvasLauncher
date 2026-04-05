package com.darksok.canvaslauncher.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.darksok.canvaslauncher.core.model.ui.DarkThemePalette
import com.darksok.canvaslauncher.core.model.ui.LightThemePalette
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CanvasLauncherThemeComposeTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `theme applies light palette in composable tree`() {
        var primaryColor: Color? = null

        composeRule.setContent {
            CanvasLauncherTheme(
                darkTheme = false,
                lightPalette = LightThemePalette.SUNSET_GLOW,
                darkPalette = DarkThemePalette.MIDNIGHT_BLUE,
            ) {
                primaryColor = MaterialTheme.colorScheme.primary
                Text("light-theme")
            }
        }

        composeRule.onNodeWithText("light-theme").assertExists()
        composeRule.runOnIdle {
            assertThat(primaryColor).isEqualTo(Color(0xFFC4673B))
        }
    }

    @Test
    fun `theme applies dark palette in composable tree`() {
        var primaryColor: Color? = null

        composeRule.setContent {
            CanvasLauncherTheme(
                darkTheme = true,
                lightPalette = LightThemePalette.SKY_BREEZE,
                darkPalette = DarkThemePalette.DEEP_OCEAN,
            ) {
                primaryColor = MaterialTheme.colorScheme.primary
                Text("dark-theme")
            }
        }

        composeRule.onNodeWithText("dark-theme").assertExists()
        composeRule.runOnIdle {
            assertThat(primaryColor).isEqualTo(Color(0xFF6CB7D6))
        }
    }
}
