package com.darksok.canvaslauncher.feature.canvas

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.graphics.Color
import com.darksok.canvaslauncher.core.model.canvas.CameraState
import com.darksok.canvaslauncher.core.model.canvas.ScreenPoint
import com.darksok.canvaslauncher.core.model.canvas.WorldPoint
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class InfiniteCanvasComposeTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `infinite canvas renders root for empty apps`() {
        composeRule.setContent {
            InfiniteCanvas(
                cameraState = CameraState(
                    worldCenter = WorldPoint(0f, 0f),
                    scale = 1f,
                    viewportWidthPx = 1080,
                    viewportHeightPx = 1920,
                ),
                apps = emptyList(),
                draggingPackageName = null,
                appDragEnabled = false,
                transformEnabled = true,
                labelsEnabled = true,
                backgroundConfig = CanvasBackgroundConfig(
                    fillColor = Color(0xFFF2F2F2),
                    dotColor = Color(0xFF444444),
                ),
                onViewportSizeChanged = {},
                onTransform = { _: ScreenPoint, _: Float, _: ScreenPoint -> },
                onAppClick = {},
                onAppDragStart = {},
                onAppDragDelta = { _: String, _: ScreenPoint -> },
                onAppDragEnd = {},
                onAppDragCancel = {},
                onAppAutoPanDelta = {},
            )
        }

        composeRule.onRoot().assertExists()
    }

    @Test
    fun `infinite canvas shows label when labels enabled and reveal delay passed`() {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            InfiniteCanvas(
                cameraState = CameraState(
                    worldCenter = WorldPoint(0f, 0f),
                    scale = 1f,
                    viewportWidthPx = 1080,
                    viewportHeightPx = 1920,
                ),
                apps = listOf(
                    CanvasRenderableApp(
                        packageName = "pkg.one",
                        label = "Canvas App",
                        worldPosition = WorldPoint(0f, 0f),
                        icon = null,
                    ),
                ),
                draggingPackageName = null,
                appDragEnabled = false,
                transformEnabled = true,
                labelsEnabled = true,
                backgroundConfig = CanvasBackgroundConfig(
                    fillColor = Color(0xFFF2F2F2),
                    dotColor = Color(0xFF444444),
                ),
                onViewportSizeChanged = {},
                onTransform = { _: ScreenPoint, _: Float, _: ScreenPoint -> },
                onAppClick = {},
                onAppDragStart = {},
                onAppDragDelta = { _: String, _: ScreenPoint -> },
                onAppDragEnd = {},
                onAppDragCancel = {},
                onAppAutoPanDelta = {},
            )
        }

        composeRule.mainClock.advanceTimeBy(250L)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Canvas App").assertExists()
    }
}
