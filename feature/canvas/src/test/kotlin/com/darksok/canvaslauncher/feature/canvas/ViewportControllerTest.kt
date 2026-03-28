package com.darksok.canvaslauncher.feature.canvas

import com.darksok.canvaslauncher.core.model.canvas.CameraState
import com.darksok.canvaslauncher.core.model.canvas.ScreenPoint
import com.darksok.canvaslauncher.core.model.canvas.WorldPoint
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ViewportControllerTest {

    @Test
    fun `centerOn projects target world point to screen center with offset`() {
        val controller = DefaultViewportController()
        controller.updateViewportSize(widthPx = 1080, heightPx = 1920)

        controller.centerOn(
            worldPoint = WorldPoint(120f, 240f),
            screenOffsetPx = ScreenPoint(0f, -320f),
        )

        val screen = controller.worldToScreen(WorldPoint(120f, 240f))
        assertThat(screen.x).isWithin(0.001f).of(540f)
        assertThat(screen.y).isWithin(0.001f).of(640f)
    }

    @Test
    fun `setCamera applies provided camera snapshot`() {
        val controller = DefaultViewportController()
        val snapshot = CameraState(
            worldCenter = WorldPoint(33f, 66f),
            scale = 1.35f,
            viewportWidthPx = 1440,
            viewportHeightPx = 3040,
        )

        controller.setCamera(snapshot)

        assertThat(controller.cameraState.value).isEqualTo(snapshot)
    }
}
