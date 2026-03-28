package com.darksok.canvaslauncher.domain.layout

import com.darksok.canvaslauncher.core.model.app.CanvasApp
import com.darksok.canvaslauncher.core.model.app.InstalledApp
import com.darksok.canvaslauncher.core.model.canvas.WorldPoint
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SpiralLayoutStrategyTest {

    @Test
    fun `places required number of apps around center`() {
        val strategy = SpiralLayoutStrategy()
        val newApps = listOf(
            InstalledApp("p1", "A"),
            InstalledApp("p2", "B"),
            InstalledApp("p3", "C"),
        )

        val result = strategy.layout(
            existingApps = emptyList(),
            newApps = newApps,
            center = WorldPoint(0f, 0f),
        )

        assertThat(result).hasSize(3)
        assertThat(result.map { it.packageName }).containsExactly("p1", "p2", "p3")
        assertThat(result.distinctBy { it.position }.size).isEqualTo(3)
    }

    @Test
    fun `keeps spreading when existing apps already present`() {
        val strategy = SpiralLayoutStrategy()
        val existing = List(10) { index ->
            CanvasApp(
                packageName = "existing$index",
                label = "existing$index",
                position = WorldPoint(index.toFloat(), index.toFloat()),
            )
        }

        val result = strategy.layout(
            existingApps = existing,
            newApps = listOf(InstalledApp("p11", "P11")),
            center = WorldPoint(0f, 0f),
        )

        assertThat(result).hasSize(1)
        assertThat(result.first().position.x).isNotEqualTo(0f)
        assertThat(result.first().position.y).isNotEqualTo(0f)
    }
}
