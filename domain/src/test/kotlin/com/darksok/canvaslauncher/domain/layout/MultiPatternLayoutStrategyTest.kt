package com.darksok.canvaslauncher.domain.layout

import com.darksok.canvaslauncher.core.model.app.InstalledApp
import com.darksok.canvaslauncher.core.model.canvas.WorldPoint
import com.darksok.canvaslauncher.core.model.ui.AppLayoutMode
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test

class MultiPatternLayoutStrategyTest {

    @Test
    fun `rectangle mode places all apps with safe spacing`() {
        val strategy = MultiPatternLayoutStrategy()
        val apps = List(36) { index ->
            InstalledApp(
                packageName = "pkg.rectangle.$index",
                label = "Rectangle $index",
            )
        }

        val result = strategy.layout(
            existingApps = emptyList(),
            newApps = apps,
            center = WorldPoint(0f, 0f),
            mode = AppLayoutMode.RECTANGLE,
        )

        assertThat(result).hasSize(apps.size)
        assertThat(result.map { it.packageName }).containsExactlyElementsIn(apps.map { it.packageName })
        assertNoClosePairs(result.map { it.position }, minDistance = 100f)
    }

    @Test
    fun `circle and oval modes do not overlap icons`() {
        val strategy = MultiPatternLayoutStrategy()
        val apps = List(48) { index ->
            InstalledApp(
                packageName = "pkg.radial.$index",
                label = "Radial $index",
            )
        }

        val circle = strategy.layout(
            existingApps = emptyList(),
            newApps = apps,
            center = WorldPoint(0f, 0f),
            mode = AppLayoutMode.CIRCLE,
        )
        val oval = strategy.layout(
            existingApps = emptyList(),
            newApps = apps,
            center = WorldPoint(0f, 0f),
            mode = AppLayoutMode.OVAL,
        )

        assertNoClosePairs(circle.map { it.position }, minDistance = 100f)
        assertNoClosePairs(oval.map { it.position }, minDistance = 100f)
    }

    @Test
    fun `smart auto keeps groups separated and spaced`() {
        val strategy = MultiPatternLayoutStrategy()
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

        val result = strategy.layout(
            existingApps = emptyList(),
            newApps = apps,
            center = WorldPoint(0f, 0f),
            mode = AppLayoutMode.SMART_AUTO,
        )

        val positions = result.map { it.position }
        assertNoClosePairs(positions, minDistance = 100f)
        val width = positions.maxOf { it.x } - positions.minOf { it.x }
        val height = positions.maxOf { it.y } - positions.minOf { it.y }
        assertThat(width).isGreaterThan(200f)
        assertThat(height).isGreaterThan(200f)
    }

    private fun assertNoClosePairs(
        points: List<WorldPoint>,
        minDistance: Float,
    ) {
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
