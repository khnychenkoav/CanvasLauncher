package com.darksok.canvaslauncher.domain.layout

import com.darksok.canvaslauncher.core.model.app.CanvasApp
import com.darksok.canvaslauncher.core.model.app.InstalledApp
import com.darksok.canvaslauncher.core.model.canvas.WorldPoint
import com.darksok.canvaslauncher.core.model.ui.AppLayoutMode
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import kotlin.math.abs

class MultiPatternLayoutStrategyTest {

    private val strategy = MultiPatternLayoutStrategy()

    @Test
    fun `empty input returns empty output`() {
        val result = strategy.layout(emptyList(), emptyList(), WorldPoint(0f, 0f), AppLayoutMode.RECTANGLE)
        assertThat(result).isEmpty()
    }

    @Test
    fun `spiral mode places apps through spiral branch`() {
        val result = strategy.layout(emptyList(), sampleApps(3), WorldPoint(0f, 0f), AppLayoutMode.SPIRAL)
        assertThat(result).hasSize(3)
        assertThat(result.first().position).isEqualTo(WorldPoint(0f, 0f))
    }

    @Test
    fun `rectangle mode places all apps with safe spacing`() {
        val result = strategy.layout(emptyList(), sampleApps(36), WorldPoint(0f, 0f), AppLayoutMode.RECTANGLE)
        assertThat(result).hasSize(36)
        assertNoClosePairs(result.map { it.position }, 100f)
    }

    @Test
    fun `rectangle mode keeps single app at center`() {
        val result = strategy.layout(emptyList(), sampleApps(1), WorldPoint(50f, -40f), AppLayoutMode.RECTANGLE)
        assertThat(result.single().position).isEqualTo(WorldPoint(50f, -40f))
    }

    @Test
    fun `rectangle mode respects existing app count offset`() {
        val existing = listOf(CanvasApp("existing", "Existing", WorldPoint(0f, 0f)))
        val result = strategy.layout(existing, sampleApps(1), WorldPoint(0f, 0f), AppLayoutMode.RECTANGLE)
        assertThat(result.single().position).isNotEqualTo(WorldPoint(0f, 0f))
    }

    @Test
    fun `circle and oval modes do not overlap icons`() {
        val apps = sampleApps(48)
        val circle = strategy.layout(emptyList(), apps, WorldPoint(0f, 0f), AppLayoutMode.CIRCLE)
        val oval = strategy.layout(emptyList(), apps, WorldPoint(0f, 0f), AppLayoutMode.OVAL)
        assertNoClosePairs(circle.map { it.position }, 100f)
        assertNoClosePairs(oval.map { it.position }, 100f)
    }

    @Test
    fun `circle mode keeps first app at center`() {
        val result = strategy.layout(emptyList(), sampleApps(1), WorldPoint(10f, 20f), AppLayoutMode.CIRCLE)
        assertThat(result.single().position).isEqualTo(WorldPoint(10f, 20f))
    }

    @Test
    fun `oval mode stretches horizontal spread more than vertical`() {
        val result = strategy.layout(emptyList(), sampleApps(24), WorldPoint(0f, 0f), AppLayoutMode.OVAL)
        val xSpread = result.maxOf { it.position.x } - result.minOf { it.position.x }
        val ySpread = result.maxOf { it.position.y } - result.minOf { it.position.y }
        assertThat(xSpread).isGreaterThan(ySpread)
    }

    @Test
    fun `smart auto keeps groups separated and spaced`() {
        val apps = listOf(
            InstalledApp("org.telegram.messenger", "Telegram"),
            InstalledApp("com.whatsapp", "WhatsApp"),
            InstalledApp("com.instagram.android", "Instagram"),
            InstalledApp("com.reddit.frontpage", "Reddit"),
            InstalledApp("com.google.android.youtube", "YouTube"),
            InstalledApp("com.spotify.music", "Spotify"),
            InstalledApp("com.supercell.clashofclans", "Clash of Clans"),
            InstalledApp("com.roblox.client", "Roblox"),
            InstalledApp("com.google.android.gm", "Gmail"),
            InstalledApp("com.microsoft.office.excel", "Excel"),
            InstalledApp("com.mybank.mobile", "My Bank"),
            InstalledApp("com.binance.dev", "Binance"),
            InstalledApp("com.amazon.mShop.android.shopping", "Amazon"),
            InstalledApp("ru.ozon.app.android", "Ozon"),
            InstalledApp("com.android.settings", "Settings"),
            InstalledApp("com.android.chrome", "Chrome"),
        )
        val result = strategy.layout(emptyList(), apps, WorldPoint(0f, 0f), AppLayoutMode.SMART_AUTO)
        val positions = result.map { it.position }
        assertNoClosePairs(positions, 100f)
        assertThat(positions.maxOf { it.x } - positions.minOf { it.x }).isGreaterThan(200f)
        assertThat(positions.maxOf { it.y } - positions.minOf { it.y }).isGreaterThan(200f)
    }

    @Test
    fun `smart auto resolves collisions against existing apps`() {
        val existing = listOf(CanvasApp("existing", "Existing", WorldPoint(0f, 0f)))
        val result = strategy.layout(existing, listOf(InstalledApp("pkg.new", "New")), WorldPoint(0f, 0f), AppLayoutMode.SMART_AUTO)
        assertThat(result.single().position).isNotEqualTo(WorldPoint(0f, 0f))
    }

    @Test
    fun `smart auto handles duplicate packages without duplicate preferred slots`() {
        val result = strategy.layout(
            emptyList(),
            listOf(InstalledApp("pkg.same", "Same"), InstalledApp("pkg.same", "Same copy")),
            WorldPoint(0f, 0f),
            AppLayoutMode.SMART_AUTO,
        )
        assertThat(result).hasSize(2)
        assertThat(result.map { it.position }.distinct()).hasSize(2)
    }

    @Test
    fun `icon color mode keeps safe spacing as rectangle fallback`() {
        val result = strategy.layout(emptyList(), sampleApps(24), WorldPoint(0f, 0f), AppLayoutMode.ICON_COLOR)
        assertThat(result).hasSize(24)
        assertNoClosePairs(result.map { it.position }, 100f)
    }

    private fun sampleApps(count: Int): List<InstalledApp> = List(count) { index ->
        InstalledApp(packageName = "pkg.$index", label = "App $index")
    }

    private fun assertNoClosePairs(points: List<WorldPoint>, minDistance: Float) {
        val minDistanceSquared = minDistance * minDistance
        points.forEachIndexed { index, first ->
            points.drop(index + 1).forEach { second ->
                val dx = first.x - second.x
                val dy = first.y - second.y
                val distanceSquared = dx * dx + dy * dy
                assertWithMessage("Points are too close: $first and $second")
                    .that(distanceSquared)
                    .isAtLeast(minDistanceSquared)
            }
        }
    }
}
