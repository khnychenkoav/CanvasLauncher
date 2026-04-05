package com.darksok.canvaslauncher.core.performance

import com.darksok.canvaslauncher.core.model.canvas.CameraState
import com.darksok.canvaslauncher.core.model.canvas.ScreenPoint
import com.darksok.canvaslauncher.core.model.canvas.WorldPoint
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class WorldScreenTransformerTest {

    @Test
    fun `world center maps to screen center`() {
        val camera = camera(center = WorldPoint(100f, -50f), scale = 1.4f)

        val screen = WorldScreenTransformer.worldToScreen(camera.worldCenter, camera)

        assertThat(screen.x).isWithin(0.001f).of(540f)
        assertThat(screen.y).isWithin(0.001f).of(960f)
    }

    @Test
    fun `screen center maps to world center`() {
        val camera = camera(center = WorldPoint(100f, -50f), scale = 1.4f)

        val world = WorldScreenTransformer.screenToWorld(ScreenPoint(540f, 960f), camera)

        assertThat(world).isEqualTo(camera.worldCenter)
    }

    @Test
    fun `world to screen to world keeps same point`() {
        val camera = camera(center = WorldPoint(100f, -50f), scale = 1.4f)
        val world = WorldPoint(160f, 20f)

        val screen = WorldScreenTransformer.worldToScreen(world, camera)
        val restored = WorldScreenTransformer.screenToWorld(screen, camera)

        assertThat(restored.x).isWithin(0.001f).of(world.x)
        assertThat(restored.y).isWithin(0.001f).of(world.y)
    }

    @Test
    fun `pan moves center opposite to finger movement`() {
        val updated = WorldScreenTransformer.applyPan(camera(scale = 2f), ScreenPoint(40f, -20f))

        assertThat(updated.worldCenter.x).isWithin(0.001f).of(-20f)
        assertThat(updated.worldCenter.y).isWithin(0.001f).of(10f)
    }

    @Test
    fun `pan is smaller in world space at higher scale`() {
        val lowScale = WorldScreenTransformer.applyPan(camera(scale = 1f), ScreenPoint(100f, 0f))
        val highScale = WorldScreenTransformer.applyPan(camera(scale = 2f), ScreenPoint(100f, 0f))

        assertThat(kotlin.math.abs(highScale.worldCenter.x)).isLessThan(kotlin.math.abs(lowScale.worldCenter.x))
    }

    @Test
    fun `zoom clamps to min max scale`() {
        val focus = ScreenPoint(540f, 960f)
        val zoomOut = WorldScreenTransformer.applyZoom(camera(scale = 1f), 0.0001f, focus)
        val zoomIn = WorldScreenTransformer.applyZoom(camera(scale = 1f), 99f, focus)

        assertThat(zoomOut.scale).isAtLeast(0.3f)
        assertThat(zoomIn.scale).isAtMost(2.0f)
    }

    @Test
    fun `zoom snaps to max scale near upper edge`() {
        val updated = WorldScreenTransformer.applyZoom(camera(scale = 2.0f), 0.999f, ScreenPoint(540f, 960f))
        assertThat(updated.scale).isEqualTo(2.0f)
    }

    @Test
    fun `zoom snaps to min scale near lower edge`() {
        val updated = WorldScreenTransformer.applyZoom(camera(scale = 0.3f), 1.001f, ScreenPoint(540f, 960f))
        assertThat(updated.scale).isEqualTo(0.3f)
    }

    @Test
    fun `zoom keeps focused world point stable at screen center`() {
        val camera = camera(center = WorldPoint(100f, 50f), scale = 1f)
        val focus = ScreenPoint(540f, 960f)
        val anchoredBefore = WorldScreenTransformer.screenToWorld(focus, camera)

        val updated = WorldScreenTransformer.applyZoom(camera, 1.5f, focus)
        val anchoredAfter = WorldScreenTransformer.screenToWorld(focus, updated)

        assertThat(anchoredAfter.x).isWithin(0.001f).of(anchoredBefore.x)
        assertThat(anchoredAfter.y).isWithin(0.001f).of(anchoredBefore.y)
    }

    @Test
    fun `zoom keeps focused world point stable off center`() {
        val camera = camera(center = WorldPoint(100f, 50f), scale = 1f)
        val focus = ScreenPoint(100f, 200f)
        val anchoredBefore = WorldScreenTransformer.screenToWorld(focus, camera)

        val updated = WorldScreenTransformer.applyZoom(camera, 1.5f, focus)
        val anchoredAfter = WorldScreenTransformer.screenToWorld(focus, updated)

        assertThat(anchoredAfter.x).isWithin(0.001f).of(anchoredBefore.x)
        assertThat(anchoredAfter.y).isWithin(0.001f).of(anchoredBefore.y)
    }

    @Test
    fun `apply zoom returns same instance when scale does not change`() {
        val camera = camera(scale = 2f)

        val updated = WorldScreenTransformer.applyZoom(camera, 1.1f, ScreenPoint(540f, 960f))

        assertThat(updated).isSameInstanceAs(camera)
    }

    @Test
    fun `clamp scale returns interior value unchanged`() {
        assertThat(WorldScreenTransformer.clampScale(1.2f)).isWithin(0.0001f).of(1.2f)
    }

    @Test
    fun `clamp scale snaps values above max`() {
        assertThat(WorldScreenTransformer.clampScale(9f)).isEqualTo(2.0f)
    }

    @Test
    fun `clamp scale snaps values below min`() {
        assertThat(WorldScreenTransformer.clampScale(0.01f)).isEqualTo(0.3f)
    }

    private fun camera(
        center: WorldPoint = WorldPoint(0f, 0f),
        scale: Float = 1f,
    ) = CameraState(
        worldCenter = center,
        scale = scale,
        viewportWidthPx = 1080,
        viewportHeightPx = 1920,
    )
}
