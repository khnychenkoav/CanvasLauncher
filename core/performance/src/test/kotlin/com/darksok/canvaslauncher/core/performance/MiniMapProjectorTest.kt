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
    fun `project preserves app point count`() {
        val apps = listOf(WorldPoint(-100f, -50f), WorldPoint(10f, 20f), WorldPoint(220f, 30f))
        val projection = MiniMapProjector.project(apps, camera(), 200f, 140f)
        assertThat(projection.appPoints).hasSize(3)
    }

    @Test
    fun `project uses camera center fallback when app list is empty`() {
        val camera = camera(center = WorldPoint(400f, -300f))
        val projection = MiniMapProjector.project(emptyList(), camera, 200f, 100f)
        assertThat(projection.userPoint.x).isWithin(0.001f).of(100f)
        assertThat(projection.userPoint.y).isWithin(0.001f).of(50f)
        assertThat(projection.appPoints).isEmpty()
    }

    @Test
    fun `user point projected inside map bounds`() {
        val projection = MiniMapProjector.project(
            appPositions = listOf(WorldPoint(-300f, -300f), WorldPoint(300f, 300f)),
            camera = camera(scale = 1.6f),
            mapWidthPx = 200f,
            mapHeightPx = 140f,
        )
        assertInside(projection.userPoint.x, 0f, 200f)
        assertInside(projection.userPoint.y, 0f, 140f)
    }

    @Test
    fun `all app points are clamped inside map bounds`() {
        val projection = MiniMapProjector.project(
            appPositions = listOf(WorldPoint(-10_000f, -10_000f), WorldPoint(0f, 0f), WorldPoint(10_000f, 10_000f)),
            camera = camera(),
            mapWidthPx = 200f,
            mapHeightPx = 140f,
        )
        projection.appPoints.forEach { point ->
            assertInside(point.x, 0f, 200f)
            assertInside(point.y, 0f, 140f)
        }
    }

    @Test
    fun `viewport rect gets smaller as scale increases`() {
        val apps = listOf(WorldPoint(-500f, -500f), WorldPoint(500f, 500f))
        val lowProjection = MiniMapProjector.project(apps, camera(scale = 1.35f), 200f, 200f)
        val highProjection = MiniMapProjector.project(apps, camera(scale = 2f), 200f, 200f)
        val lowWidth = lowProjection.viewportRect.right - lowProjection.viewportRect.left
        val highWidth = highProjection.viewportRect.right - highProjection.viewportRect.left
        assertThat(highWidth).isLessThan(lowWidth)
    }

    @Test
    fun `viewport rect stays inside map bounds near edges`() {
        val projection = MiniMapProjector.project(
            appPositions = listOf(WorldPoint(0f, 0f), WorldPoint(1_000f, 1_000f)),
            camera = camera(center = WorldPoint(1_000f, 1_000f), scale = 1.4f),
            mapWidthPx = 200f,
            mapHeightPx = 100f,
        )
        assertInside(projection.viewportRect.left, 0f, 200f)
        assertInside(projection.viewportRect.top, 0f, 100f)
        assertInside(projection.viewportRect.right, 0f, 200f)
        assertInside(projection.viewportRect.bottom, 0f, 100f)
    }

    @Test
    fun `project enforces minimum world span for nearby points`() {
        val projection = MiniMapProjector.project(
            appPositions = listOf(WorldPoint(0f, 0f), WorldPoint(1f, 1f)),
            camera = camera(),
            mapWidthPx = 200f,
            mapHeightPx = 100f,
            worldPadding = 0f,
        )
        val appXs = projection.appPoints.map { it.x }
        assertThat(appXs.min()).isGreaterThan(0f)
        assertThat(appXs.max()).isLessThan(200f)
    }

    @Test
    fun `world padding pushes extreme app points away from border`() {
        val noPadding = MiniMapProjector.project(
            appPositions = listOf(WorldPoint(0f, 0f), WorldPoint(1_000f, 0f)),
            camera = camera(),
            mapWidthPx = 200f,
            mapHeightPx = 100f,
            worldPadding = 0f,
        )
        val withPadding = MiniMapProjector.project(
            appPositions = listOf(WorldPoint(0f, 0f), WorldPoint(1_000f, 0f)),
            camera = camera(),
            mapWidthPx = 200f,
            mapHeightPx = 100f,
            worldPadding = 200f,
        )
        assertThat(withPadding.appPoints.first().x).isGreaterThan(noPadding.appPoints.first().x)
        assertThat(withPadding.appPoints.last().x).isLessThan(noPadding.appPoints.last().x)
    }

    @Test
    fun `user point tracks shifted camera center`() {
        val projection = MiniMapProjector.project(
            appPositions = listOf(WorldPoint(-500f, -500f), WorldPoint(500f, 500f)),
            camera = camera(center = WorldPoint(400f, 0f)),
            mapWidthPx = 200f,
            mapHeightPx = 100f,
        )
        assertThat(projection.userPoint.x).isGreaterThan(100f)
    }

    private fun camera(
        center: WorldPoint = WorldPoint(0f, 0f),
        scale: Float = 1.5f,
    ) = CameraState(
        worldCenter = center,
        scale = scale,
        viewportWidthPx = 1080,
        viewportHeightPx = 1920,
    )

    private fun assertInside(value: Float, min: Float, max: Float) {
        assertThat(value).isAtLeast(min)
        assertThat(value).isAtMost(max)
    }
}
