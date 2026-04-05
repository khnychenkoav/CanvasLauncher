package com.darksok.canvaslauncher.domain.layout

import com.darksok.canvaslauncher.core.model.app.CanvasApp
import com.darksok.canvaslauncher.core.model.app.InstalledApp
import com.darksok.canvaslauncher.core.model.canvas.WorldPoint
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import kotlin.math.sqrt

class SpiralLayoutStrategyTest {

    private val strategy = SpiralLayoutStrategy()

    @Test
    fun `empty new apps returns empty list`() {
        assertThat(strategy.layout(emptyList(), emptyList(), WorldPoint(0f, 0f))).isEmpty()
    }

    @Test
    fun `places required number of apps around center`() {
        val result = strategy.layout(emptyList(), sampleApps(3), WorldPoint(0f, 0f))
        assertThat(result).hasSize(3)
        assertThat(result.map { it.packageName }).containsExactly("p1", "p2", "p3").inOrder()
        assertThat(result.distinctBy { it.position }.size).isEqualTo(3)
    }

    @Test
    fun `first app starts at center`() {
        val result = strategy.layout(emptyList(), sampleApps(1), WorldPoint(10f, 20f))
        assertThat(result.single().position).isEqualTo(WorldPoint(10f, 20f))
    }

    @Test
    fun `keeps spreading when existing apps already present`() {
        val existing = List(10) { index -> CanvasApp("existing$index", "existing$index", WorldPoint(index.toFloat(), index.toFloat())) }
        val result = strategy.layout(existing, listOf(InstalledApp("p11", "P11")), WorldPoint(0f, 0f))
        assertThat(result.single().position).isNotEqualTo(WorldPoint(0f, 0f))
    }

    @Test
    fun `radius grows with each emitted index`() {
        val result = strategy.layout(emptyList(), sampleApps(4), WorldPoint(0f, 0f))
        val distances = result.map { point -> sqrt(point.position.x * point.position.x + point.position.y * point.position.y) }
        assertThat(distances[0]).isEqualTo(0f)
        assertThat(distances[3]).isGreaterThan(distances[1])
    }

    @Test
    fun `custom center offsets all points`() {
        val result = strategy.layout(emptyList(), sampleApps(2), WorldPoint(100f, -50f))
        assertThat(result.first().position).isEqualTo(WorldPoint(100f, -50f))
    }

    private fun sampleApps(count: Int): List<InstalledApp> = List(count) { index ->
        InstalledApp("p${index + 1}", "App ${index + 1}")
    }
}
