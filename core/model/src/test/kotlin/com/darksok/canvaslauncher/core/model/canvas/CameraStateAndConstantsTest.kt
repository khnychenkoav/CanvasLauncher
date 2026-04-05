package com.darksok.canvaslauncher.core.model.canvas

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CameraStateAndConstantsTest {

    @Test
    fun `camera state defaults to origin center`() {
        val state = CameraState()
        assertThat(state.worldCenter).isEqualTo(WorldPoint(0f, 0f))
    }

    @Test
    fun `camera state defaults to initial scale`() {
        val state = CameraState()
        assertThat(state.scale).isEqualTo(CanvasConstants.Defaults.INITIAL_SCALE)
    }

    @Test
    fun `camera state defaults to one pixel viewport width`() {
        val state = CameraState()
        assertThat(state.viewportWidthPx).isEqualTo(1)
    }

    @Test
    fun `camera state defaults to one pixel viewport height`() {
        val state = CameraState()
        assertThat(state.viewportHeightPx).isEqualTo(1)
    }

    @Test
    fun `camera state copy can replace center`() {
        val state = CameraState()
        val updated = state.copy(worldCenter = WorldPoint(10f, -8f))
        assertThat(updated.worldCenter).isEqualTo(WorldPoint(10f, -8f))
        assertThat(updated.scale).isEqualTo(state.scale)
    }

    @Test
    fun `camera state copy can replace scale`() {
        val state = CameraState()
        val updated = state.copy(scale = 1.75f)
        assertThat(updated.scale).isEqualTo(1.75f)
    }

    @Test
    fun `camera state copy can replace viewport size`() {
        val state = CameraState()
        val updated = state.copy(viewportWidthPx = 1080, viewportHeightPx = 1920)
        assertThat(updated.viewportWidthPx).isEqualTo(1080)
        assertThat(updated.viewportHeightPx).isEqualTo(1920)
    }

    @Test
    fun `scale minimum is below maximum`() {
        assertThat(CanvasConstants.Scale.MIN_SCALE).isLessThan(CanvasConstants.Scale.MAX_SCALE)
    }

    @Test
    fun `label visibility threshold lies inside scale range`() {
        assertThat(CanvasConstants.Scale.LABEL_VISIBLE_THRESHOLD).isAtLeast(CanvasConstants.Scale.MIN_SCALE)
        assertThat(CanvasConstants.Scale.LABEL_VISIBLE_THRESHOLD).isAtMost(CanvasConstants.Scale.MAX_SCALE)
    }

    @Test
    fun `icon cache bitmap size is positive`() {
        assertThat(CanvasConstants.Icon.CACHE_BITMAP_SIZE_PX).isGreaterThan(0)
    }

    @Test
    fun `icon cache max bytes is positive`() {
        assertThat(CanvasConstants.Icon.CACHE_MAX_BYTES).isGreaterThan(0)
    }

    @Test
    fun `icon world size is positive`() {
        assertThat(CanvasConstants.Sizes.ICON_WORLD_SIZE).isGreaterThan(0f)
    }

    @Test
    fun `culling buffer is positive`() {
        assertThat(CanvasConstants.Sizes.CULLING_BUFFER_WORLD).isGreaterThan(0f)
    }

    @Test
    fun `initial scale lies inside allowed range`() {
        assertThat(CanvasConstants.Defaults.INITIAL_SCALE).isAtLeast(CanvasConstants.Scale.MIN_SCALE)
        assertThat(CanvasConstants.Defaults.INITIAL_SCALE).isAtMost(CanvasConstants.Scale.MAX_SCALE)
    }
}
