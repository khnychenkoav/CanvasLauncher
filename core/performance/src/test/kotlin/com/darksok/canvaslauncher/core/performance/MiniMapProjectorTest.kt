package com.darksok.canvaslauncher.core.performance

import com.darksok.canvaslauncher.core.model.canvas.CameraState
import com.darksok.canvaslauncher.core.model.canvas.WorldPoint
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class MiniMapProjectorTest {

    @Test
    fun `show threshold triggers for strong zoom`() {
        assertThat(MiniMapProjector.shouldShow(1.34f)).isFalse()
        assertThat(MiniMapProjector.shouldShow(1.35f)).isTrue()
        assertThat(MiniMapProjector.shouldShow(1.8f)).isTrue()
    }

    @Test
    fun `user point projected inside map bounds`() {
        val camera = CameraState(
            worldCenter = WorldPoint(0f, 0f),
            scale = 1.6f,
            viewportWidthPx = 1080,
            viewportHeightPx = 1920,
        )

        val projection = MiniMapProjector.project(
            appPositions = listOf(WorldPoint(-300f, -300f), WorldPoint(300f, 300f)),
            camera = camera,
            mapWidthPx = 200f,
            mapHeightPx = 140f,
        )

        assertThat(projection.userPoint.x).isAtLeast(0f)
        assertThat(projection.userPoint.x).isAtMost(200f)
        assertThat(projection.userPoint.y).isAtLeast(0f)
        assertThat(projection.userPoint.y).isAtMost(140f)
    }

    @Test
    fun `viewport rect gets smaller as scale increases`() {
        val apps = listOf(
            WorldPoint(-500f, -500f),
            WorldPoint(500f, 500f),
        )
        val lowZoom = CameraState(
            worldCenter = WorldPoint(0f, 0f),
            scale = 1.35f,
            viewportWidthPx = 1000,
            viewportHeightPx = 1000,
        )
        val highZoom = lowZoom.copy(scale = 2.0f)

        val lowProjection = MiniMapProjector.project(apps, lowZoom, 200f, 200f)
        val highProjection = MiniMapProjector.project(apps, highZoom, 200f, 200f)

        val lowWidth = lowProjection.viewportRect.right - lowProjection.viewportRect.left
        val highWidth = highProjection.viewportRect.right - highProjection.viewportRect.left

        assertThat(highWidth).isLessThan(lowWidth)
    }
}
