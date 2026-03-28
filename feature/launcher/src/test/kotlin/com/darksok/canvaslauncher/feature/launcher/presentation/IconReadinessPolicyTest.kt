package com.darksok.canvaslauncher.feature.launcher.presentation

import com.darksok.canvaslauncher.core.model.app.CanvasApp
import com.darksok.canvaslauncher.core.model.canvas.WorldPoint
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class IconReadinessPolicyTest {

    @Test
    fun `ready when no apps`() {
        val ready = IconReadinessPolicy.areReady(
            apps = emptyList(),
            loadedIconPackages = emptySet(),
        )

        assertThat(ready).isTrue()
    }

    @Test
    fun `ready when all app icons loaded`() {
        val apps = listOf(
            CanvasApp("a.pkg", "A", WorldPoint(0f, 0f)),
            CanvasApp("b.pkg", "B", WorldPoint(1f, 1f)),
        )

        val ready = IconReadinessPolicy.areReady(
            apps = apps,
            loadedIconPackages = setOf("a.pkg", "b.pkg"),
        )

        assertThat(ready).isTrue()
    }

    @Test
    fun `not ready when some icons missing`() {
        val apps = listOf(
            CanvasApp("a.pkg", "A", WorldPoint(0f, 0f)),
            CanvasApp("b.pkg", "B", WorldPoint(1f, 1f)),
        )

        val ready = IconReadinessPolicy.areReady(
            apps = apps,
            loadedIconPackages = setOf("a.pkg"),
        )

        assertThat(ready).isFalse()
    }
}
