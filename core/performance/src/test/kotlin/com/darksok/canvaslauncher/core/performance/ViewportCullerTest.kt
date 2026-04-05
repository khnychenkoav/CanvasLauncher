package com.darksok.canvaslauncher.core.performance

import com.darksok.canvaslauncher.core.model.app.CanvasApp
import com.darksok.canvaslauncher.core.model.canvas.CameraState
import com.darksok.canvaslauncher.core.model.canvas.WorldPoint
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ViewportCullerTest {

    @Test
    fun `visible world rect matches viewport around center`() {
        val rect = ViewportCuller.visibleWorldRect(camera(), bufferWorld = 0f)

        assertThat(rect.left).isWithin(0.001f).of(-500f)
        assertThat(rect.top).isWithin(0.001f).of(-500f)
        assertThat(rect.right).isWithin(0.001f).of(500f)
        assertThat(rect.bottom).isWithin(0.001f).of(500f)
    }

    @Test
    fun `buffer expands visible world rect equally on all sides`() {
        val rect = ViewportCuller.visibleWorldRect(camera(), bufferWorld = 120f)

        assertThat(rect.left).isWithin(0.001f).of(-620f)
        assertThat(rect.top).isWithin(0.001f).of(-620f)
        assertThat(rect.right).isWithin(0.001f).of(620f)
        assertThat(rect.bottom).isWithin(0.001f).of(620f)
    }

    @Test
    fun `returns only items inside visible world with buffer`() {
        val apps = listOf(
            CanvasApp("a", "A", WorldPoint(0f, 0f)),
            CanvasApp("b", "B", WorldPoint(400f, 400f)),
            CanvasApp("c", "C", WorldPoint(10000f, 10000f)),
        )

        val visible = ViewportCuller.cullVisibleApps(apps, camera(), iconWorldSize = 96f, bufferWorld = 0f)

        assertThat(visible.map { it.packageName }).containsExactly("a", "b")
    }

    @Test
    fun `cull keeps boundary touching icon visible`() {
        val apps = listOf(CanvasApp("edge", "Edge", WorldPoint(548f, 0f)))

        val visible = ViewportCuller.cullVisibleApps(apps, camera(), iconWorldSize = 96f, bufferWorld = 0f)

        assertThat(visible.map { it.packageName }).containsExactly("edge")
    }

    @Test
    fun `cull removes icon just outside boundary`() {
        val apps = listOf(CanvasApp("outside", "Outside", WorldPoint(548.1f, 0f)))

        val visible = ViewportCuller.cullVisibleApps(apps, camera(), iconWorldSize = 96f, bufferWorld = 0f)

        assertThat(visible).isEmpty()
    }

    @Test
    fun `larger buffer includes farther item`() {
        val apps = listOf(CanvasApp("far", "Far", WorldPoint(650f, 0f)))

        val noBuffer = ViewportCuller.cullVisibleApps(apps, camera(), iconWorldSize = 96f, bufferWorld = 0f)
        val withBuffer = ViewportCuller.cullVisibleApps(apps, camera(), iconWorldSize = 96f, bufferWorld = 200f)

        assertThat(noBuffer).isEmpty()
        assertThat(withBuffer.map { it.packageName }).containsExactly("far")
    }

    @Test
    fun `lower scale reveals more apps`() {
        val apps = listOf(CanvasApp("far", "Far", WorldPoint(700f, 0f)))

        val zoomedIn = ViewportCuller.cullVisibleApps(apps, camera(scale = 2f), iconWorldSize = 96f, bufferWorld = 0f)
        val zoomedOut = ViewportCuller.cullVisibleApps(apps, camera(scale = 0.5f), iconWorldSize = 96f, bufferWorld = 0f)

        assertThat(zoomedIn).isEmpty()
        assertThat(zoomedOut.map { it.packageName }).containsExactly("far")
    }

    @Test
    fun `cull preserves input order of visible apps`() {
        val apps = listOf(
            CanvasApp("b", "B", WorldPoint(100f, 0f)),
            CanvasApp("a", "A", WorldPoint(0f, 0f)),
            CanvasApp("c", "C", WorldPoint(2000f, 0f)),
        )

        val visible = ViewportCuller.cullVisibleApps(apps, camera(), iconWorldSize = 96f, bufferWorld = 0f)

        assertThat(visible.map { it.packageName }).containsExactly("b", "a").inOrder()
    }

    @Test
    fun `cull returns empty list for empty input`() {
        assertThat(ViewportCuller.cullVisibleApps(emptyList(), camera())).isEmpty()
    }

    private fun camera(scale: Float = 1f) = CameraState(
        worldCenter = WorldPoint(0f, 0f),
        scale = scale,
        viewportWidthPx = 1000,
        viewportHeightPx = 1000,
    )
}
