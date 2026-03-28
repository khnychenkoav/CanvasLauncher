package com.darksok.canvaslauncher.core.performance

import com.darksok.canvaslauncher.core.model.app.CanvasApp
import com.darksok.canvaslauncher.core.model.canvas.CameraState
import com.darksok.canvaslauncher.core.model.canvas.WorldPoint
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ViewportCullerTest {

    @Test
    fun `returns only items inside visible world with buffer`() {
        val camera = CameraState(
            worldCenter = WorldPoint(0f, 0f),
            scale = 1f,
            viewportWidthPx = 1000,
            viewportHeightPx = 1000,
        )
        val apps = listOf(
            CanvasApp("a", "A", WorldPoint(0f, 0f)),
            CanvasApp("b", "B", WorldPoint(400f, 400f)),
            CanvasApp("c", "C", WorldPoint(10000f, 10000f)),
        )

        val visible = ViewportCuller.cullVisibleApps(
            apps = apps,
            camera = camera,
            iconWorldSize = 96f,
            bufferWorld = 0f,
        )

        assertThat(visible.map { it.packageName }).containsExactly("a", "b")
    }
}
