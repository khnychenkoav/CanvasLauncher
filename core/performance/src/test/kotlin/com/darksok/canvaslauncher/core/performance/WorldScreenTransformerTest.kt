package com.darksok.canvaslauncher.core.performance

import com.darksok.canvaslauncher.core.model.canvas.CameraState
import com.darksok.canvaslauncher.core.model.canvas.ScreenPoint
import com.darksok.canvaslauncher.core.model.canvas.WorldPoint
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class WorldScreenTransformerTest {

    @Test
    fun `world to screen to world keeps same point`() {
        val camera = CameraState(
            worldCenter = WorldPoint(100f, -50f),
            scale = 1.4f,
            viewportWidthPx = 1080,
            viewportHeightPx = 1920,
        )
        val world = WorldPoint(160f, 20f)

        val screen = WorldScreenTransformer.worldToScreen(world, camera)
        val restored = WorldScreenTransformer.screenToWorld(screen, camera)

        assertThat(restored.x).isWithin(0.001f).of(world.x)
        assertThat(restored.y).isWithin(0.001f).of(world.y)
    }

    @Test
    fun `pan moves center opposite to finger movement`() {
        val camera = CameraState(
            worldCenter = WorldPoint(0f, 0f),
            scale = 2f,
            viewportWidthPx = 1000,
            viewportHeightPx = 1000,
        )

        val updated = WorldScreenTransformer.applyPan(camera, ScreenPoint(40f, -20f))

        assertThat(updated.worldCenter.x).isWithin(0.001f).of(-20f)
        assertThat(updated.worldCenter.y).isWithin(0.001f).of(10f)
    }

    @Test
    fun `zoom clamps to min max scale`() {
        val camera = CameraState(
            worldCenter = WorldPoint(0f, 0f),
            scale = 1f,
            viewportWidthPx = 1000,
            viewportHeightPx = 1000,
        )
        val focus = ScreenPoint(500f, 500f)

        val zoomOut = WorldScreenTransformer.applyZoom(camera, 0.0001f, focus)
        val zoomIn = WorldScreenTransformer.applyZoom(camera, 99f, focus)

        assertThat(zoomOut.scale).isAtLeast(0.3f)
        assertThat(zoomIn.scale).isAtMost(2.0f)
    }

    @Test
    fun `zoom snaps to max scale near upper edge`() {
        val camera = CameraState(
            worldCenter = WorldPoint(0f, 0f),
            scale = 2.0f,
            viewportWidthPx = 1000,
            viewportHeightPx = 1000,
        )

        val updated = WorldScreenTransformer.applyZoom(
            camera = camera,
            zoomFactor = 0.999f,
            focus = ScreenPoint(500f, 500f),
        )

        assertThat(updated.scale).isEqualTo(2.0f)
    }
}
