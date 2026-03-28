package com.darksok.canvaslauncher.feature.launcher.presentation

import com.darksok.canvaslauncher.core.model.app.CanvasApp
import com.darksok.canvaslauncher.core.model.canvas.WorldPoint
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DragPositionOverridesTest {

    @Test
    fun `apply keeps overridden position until persistence catches up`() {
        val apps = listOf(
            CanvasApp(packageName = "pkg.a", label = "A", position = WorldPoint(1f, 1f)),
            CanvasApp(packageName = "pkg.b", label = "B", position = WorldPoint(5f, 5f)),
        )
        val overrides = mapOf("pkg.a" to WorldPoint(10f, 12f))

        val result = DragPositionOverrides.apply(apps, overrides)

        assertThat(result.first { it.packageName == "pkg.a" }.position).isEqualTo(WorldPoint(10f, 12f))
        assertThat(result.first { it.packageName == "pkg.b" }.position).isEqualTo(WorldPoint(5f, 5f))
    }

    @Test
    fun `prune removes override once persisted position matches`() {
        val overrides = mapOf(
            "pkg.a" to WorldPoint(10f, 12f),
            "pkg.b" to WorldPoint(20f, 22f),
        )
        val persisted = listOf(
            CanvasApp(packageName = "pkg.a", label = "A", position = WorldPoint(10f, 12f)),
            CanvasApp(packageName = "pkg.b", label = "B", position = WorldPoint(18f, 18f)),
        )

        val pruned = DragPositionOverrides.pruneCommitted(overrides, persisted)

        assertThat(pruned.keys).containsExactly("pkg.b")
        assertThat(pruned["pkg.b"]).isEqualTo(WorldPoint(20f, 22f))
    }
}
