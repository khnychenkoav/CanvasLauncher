package com.darksok.canvaslauncher.domain.layout

import com.darksok.canvaslauncher.core.model.app.CanvasApp
import com.darksok.canvaslauncher.core.model.app.InstalledApp
import com.darksok.canvaslauncher.core.model.canvas.WorldPoint
import com.darksok.canvaslauncher.core.model.ui.AppLayoutMode
import java.util.Locale
import javax.inject.Inject
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

class MultiPatternLayoutStrategy @Inject constructor() : InitialLayoutStrategy {

    private val spiral = SpiralLayoutStrategy()

    override fun layout(
        existingApps: List<CanvasApp>,
        newApps: List<InstalledApp>,
        center: WorldPoint,
        mode: AppLayoutMode,
    ): List<CanvasApp> {
        if (newApps.isEmpty()) return emptyList()
        return when (mode) {
            AppLayoutMode.SPIRAL -> spiral.layout(existingApps, newApps, center, mode)
            AppLayoutMode.RECTANGLE -> layoutRectangle(existingApps, newApps, center)
            AppLayoutMode.CIRCLE -> layoutRadial(existingApps, newApps, center, xScale = 1f, yScale = 1f)
            AppLayoutMode.OVAL -> layoutRadial(existingApps, newApps, center, xScale = 1.46f, yScale = 0.92f)
            AppLayoutMode.SMART_AUTO -> layoutSmart(existingApps, newApps, center)
        }
    }

    private fun layoutRectangle(
        existingApps: List<CanvasApp>,
        newApps: List<InstalledApp>,
        center: WorldPoint,
    ): List<CanvasApp> {
        val totalCount = existingApps.size + newApps.size
        val columns = rectangleColumns(totalCount)
        val rows = ceil(totalCount / columns.toDouble()).toInt().coerceAtLeast(1)
        return placeByIndexSequence(
            existingApps = existingApps,
            newApps = newApps,
            startIndex = existingApps.size,
        ) { index ->
            rectanglePoint(
                index = index,
                center = center,
                columns = columns,
                rows = rows,
            )
        }
    }

    private fun layoutRadial(
        existingApps: List<CanvasApp>,
        newApps: List<InstalledApp>,
        center: WorldPoint,
        xScale: Float,
        yScale: Float,
    ): List<CanvasApp> {
        return placeByIndexSequence(
            existingApps = existingApps,
            newApps = newApps,
            startIndex = existingApps.size,
        ) { index ->
            radialPoint(
                index = index,
                center = center,
                xScale = xScale,
                yScale = yScale,
            )
        }
    }

    private fun layoutSmart(
        existingApps: List<CanvasApp>,
        newApps: List<InstalledApp>,
        center: WorldPoint,
    ): List<CanvasApp> {
        val all = buildList {
            existingApps.forEach { app ->
                add(InstalledApp(packageName = app.packageName, label = app.label))
            }
            addAll(newApps)
        }.distinctBy { app -> app.packageName }
            .sortedBy { app -> app.label.lowercase(Locale.ROOT) }
        if (all.isEmpty()) return emptyList()

        val positionByPackage = computeSmartLayoutPositions(
            allApps = all,
            center = center,
        )
        if (positionByPackage.isEmpty()) {
            return layoutRectangle(existingApps, newApps, center)
        }

        val occupied = existingApps.map { app -> app.position }.toMutableList()

        return newApps.map { app ->
            val preferred = positionByPackage[app.packageName] ?: center
            val safePosition = if (isPositionFree(preferred, occupied)) {
                preferred
            } else {
                findNearestFreePosition(preferred, occupied)
            }
            occupied += safePosition
            CanvasApp(
                packageName = app.packageName,
                label = app.label,
                position = safePosition,
            )
        }
    }

    private fun placeByIndexSequence(
        existingApps: List<CanvasApp>,
        newApps: List<InstalledApp>,
        startIndex: Int,
        pointForIndex: (Int) -> WorldPoint,
    ): List<CanvasApp> {
        val occupied = existingApps.map { app -> app.position }.toMutableList()
        var candidateIndex = startIndex
        return newApps.map { app ->
            val placement = findNextFreePlacement(
                startIndex = candidateIndex,
                occupied = occupied,
                pointForIndex = pointForIndex,
            )
            candidateIndex = placement.nextIndex
            occupied += placement.position
            CanvasApp(
                packageName = app.packageName,
                label = app.label,
                position = placement.position,
            )
        }
    }

    private fun findNextFreePlacement(
        startIndex: Int,
        occupied: List<WorldPoint>,
        pointForIndex: (Int) -> WorldPoint,
    ): IndexedPlacement {
        var index = startIndex
        repeat(MAX_INDEX_PLACEMENT_ATTEMPTS) {
            val candidate = pointForIndex(index)
            if (isPositionFree(candidate, occupied)) {
                return IndexedPlacement(
                    position = candidate,
                    nextIndex = index + 1,
                )
            }
            index += 1
        }

        val fallback = findNearestFreePosition(pointForIndex(startIndex), occupied)
        return IndexedPlacement(
            position = fallback,
            nextIndex = index + 1,
        )
    }

    private fun findNearestFreePosition(
        preferred: WorldPoint,
        occupied: List<WorldPoint>,
    ): WorldPoint {
        if (isPositionFree(preferred, occupied)) return preferred

        for (ring in 1..FREE_POSITION_SEARCH_RINGS) {
            val radius = ring * GRID_STEP_WORLD
            val pointsOnRing = max(8, ring * 8)
            for (pointIndex in 0 until pointsOnRing) {
                val angle = (2.0 * PI * pointIndex) / pointsOnRing
                val candidate = WorldPoint(
                    x = preferred.x + (cos(angle) * radius).toFloat(),
                    y = preferred.y + (sin(angle) * radius).toFloat(),
                )
                if (isPositionFree(candidate, occupied)) {
                    return candidate
                }
            }
        }
        return preferred
    }

    private fun computeSmartLayoutPositions(
        allApps: List<InstalledApp>,
        center: WorldPoint,
    ): Map<String, WorldPoint> {
        val grouped = SmartCategory.entries.mapNotNull { category ->
            val apps = allApps.filter { app -> classifySmartCategory(app) == category }
            if (apps.isEmpty()) null else apps
        }
        if (grouped.isEmpty()) return emptyMap()

        val groups = grouped.map { apps ->
            val columns = ceil(sqrt(apps.size.toDouble())).toInt().coerceAtLeast(2)
            val rows = ceil(apps.size / columns.toDouble()).toInt().coerceAtLeast(1)
            SmartGroup(
                apps = apps,
                columns = columns,
                rows = rows,
            )
        }

        val maxColumns = groups.maxOf { group -> group.columns }.coerceAtLeast(3)
        val maxRows = groups.maxOf { group -> group.rows }.coerceAtLeast(2)
        val groupWidth = (maxColumns - 1) * GRID_STEP_WORLD + GROUP_PADDING_WORLD * 2f
        val groupHeight = (maxRows - 1) * GRID_STEP_WORLD + GROUP_PADDING_WORLD * 2f

        val groupCount = groups.size
        val groupsPerRow = ceil(sqrt(groupCount.toDouble())).toInt().coerceAtLeast(1)
        val totalRows = ceil(groupCount / groupsPerRow.toDouble()).toInt().coerceAtLeast(1)

        val positionByPackage = mutableMapOf<String, WorldPoint>()
        groups.forEachIndexed { groupIndex, group ->
            val row = groupIndex / groupsPerRow
            val col = groupIndex % groupsPerRow
            val groupCenter = WorldPoint(
                x = center.x + (col - (groupsPerRow - 1) / 2f) * (groupWidth + GROUP_GAP_WORLD),
                y = center.y + (row - (totalRows - 1) / 2f) * (groupHeight + GROUP_GAP_WORLD),
            )

            group.apps.forEachIndexed { index, app ->
                val itemRow = index / group.columns
                val itemCol = index % group.columns
                val x = groupCenter.x + (itemCol - (group.columns - 1) / 2f) * GRID_STEP_WORLD
                val y = groupCenter.y + (itemRow - (group.rows - 1) / 2f) * GRID_STEP_WORLD
                positionByPackage[app.packageName] = WorldPoint(x, y)
            }
        }
        return positionByPackage
    }

    private fun rectanglePoint(
        index: Int,
        center: WorldPoint,
        columns: Int,
        rows: Int,
    ): WorldPoint {
        val row = index / columns
        val col = index % columns
        val xOffset = (col - (columns - 1) / 2f) * GRID_STEP_WORLD
        val yOffset = (row - (rows - 1) / 2f) * GRID_STEP_WORLD
        return WorldPoint(
            x = center.x + xOffset,
            y = center.y + yOffset,
        )
    }

    private fun rectangleColumns(totalCount: Int): Int {
        if (totalCount <= 1) return 1
        return ceil(sqrt(totalCount * RECTANGLE_TARGET_ASPECT_RATIO).toDouble())
            .toInt()
            .coerceAtLeast(2)
    }

    private fun radialPoint(
        index: Int,
        center: WorldPoint,
        xScale: Float,
        yScale: Float,
    ): WorldPoint {
        if (index <= 0) return center
        var remaining = index - 1
        var ring = 1
        while (true) {
            val radius = ring * RING_STEP_WORLD
            val capacity = max(
                6,
                ((2.0 * PI * radius) / GRID_STEP_WORLD).toInt(),
            )
            if (remaining < capacity) {
                val angle = (2.0 * PI * remaining) / capacity - PI / 2.0
                val x = center.x + (radius * cos(angle)).toFloat() * xScale
                val y = center.y + (radius * sin(angle)).toFloat() * yScale
                return WorldPoint(x, y)
            }
            remaining -= capacity
            ring += 1
        }
    }

    private fun classifySmartCategory(app: InstalledApp): SmartCategory {
        val source = "${app.packageName} ${app.label}".lowercase(Locale.ROOT)
        return when {
            source.containsAny(MESSAGING_KEYWORDS) -> SmartCategory.Messaging
            source.containsAny(SOCIAL_KEYWORDS) -> SmartCategory.Social
            source.containsAny(ENTERTAINMENT_KEYWORDS) -> SmartCategory.Entertainment
            source.containsAny(GAMES_KEYWORDS) -> SmartCategory.Games
            source.containsAny(PRODUCTIVITY_KEYWORDS) -> SmartCategory.Productivity
            source.containsAny(FINANCE_KEYWORDS) -> SmartCategory.Finance
            source.containsAny(SHOPPING_KEYWORDS) -> SmartCategory.Shopping
            source.containsAny(TOOLS_KEYWORDS) -> SmartCategory.Tools
            else -> SmartCategory.Other
        }
    }

    private fun String.containsAny(keywords: Set<String>): Boolean {
        return keywords.any { keyword -> contains(keyword) }
    }

    private fun isPositionFree(
        candidate: WorldPoint,
        occupied: List<WorldPoint>,
    ): Boolean {
        return occupied.none { point ->
            distanceSquared(point, candidate) < MIN_DISTANCE_BETWEEN_APPS_WORLD * MIN_DISTANCE_BETWEEN_APPS_WORLD
        }
    }

    private fun distanceSquared(first: WorldPoint, second: WorldPoint): Float {
        val dx = first.x - second.x
        val dy = first.y - second.y
        return dx * dx + dy * dy
    }

    private data class IndexedPlacement(
        val position: WorldPoint,
        val nextIndex: Int,
    )

    private data class SmartGroup(
        val apps: List<InstalledApp>,
        val columns: Int,
        val rows: Int,
    )

    private enum class SmartCategory {
        Messaging,
        Social,
        Entertainment,
        Games,
        Productivity,
        Finance,
        Shopping,
        Tools,
        Other,
    }

    private companion object {
        private const val GRID_STEP_WORLD = 136f
        private const val RING_STEP_WORLD = 160f
        private const val RECTANGLE_TARGET_ASPECT_RATIO = 0.58f
        private const val GROUP_PADDING_WORLD = 48f
        private const val GROUP_GAP_WORLD = 208f
        private const val MIN_DISTANCE_BETWEEN_APPS_WORLD = 108f
        private const val FREE_POSITION_SEARCH_RINGS = 12
        private const val MAX_INDEX_PLACEMENT_ATTEMPTS = 25_000

        private val MESSAGING_KEYWORDS = setOf(
            "telegram",
            "whatsapp",
            "signal",
            "messenger",
            "imessage",
            "wechat",
            "viber",
            "line",
            "chat",
            "mail",
            "gmail",
            "outlook",
            "teams",
            "slack",
            "discord",
            "skype",
        )

        private val SOCIAL_KEYWORDS = setOf(
            "instagram",
            "facebook",
            "snapchat",
            "twitter",
            "x.com",
            "reddit",
            "vk",
            "social",
            "threads",
            "pinterest",
            "linkedin",
            "mastodon",
        )

        private val ENTERTAINMENT_KEYWORDS = setOf(
            "youtube",
            "music",
            "spotify",
            "netflix",
            "primevideo",
            "hbo",
            "disney",
            "twitch",
            "tiktok",
            "video",
            "stream",
            "cinema",
            "tv",
            "podcast",
        )

        private val GAMES_KEYWORDS = setOf(
            "game",
            "games",
            "play games",
            "clash",
            "roblox",
            "minecraft",
            "steam",
            "epic",
            "riot",
            "brawl",
        )

        private val PRODUCTIVITY_KEYWORDS = setOf(
            "docs",
            "sheets",
            "slides",
            "calendar",
            "drive",
            "office",
            "word",
            "excel",
            "powerpoint",
            "notion",
            "todo",
            "task",
            "notes",
            "keep",
            "workspace",
        )

        private val FINANCE_KEYWORDS = setOf(
            "bank",
            "wallet",
            "finance",
            "pay",
            "money",
            "invest",
            "broker",
            "coin",
            "crypto",
        )

        private val SHOPPING_KEYWORDS = setOf(
            "shop",
            "store",
            "market",
            "aliexpress",
            "amazon",
            "ebay",
            "ozon",
            "wildberries",
            "delivery",
            "food",
            "uber eats",
            "doordash",
            "instacart",
        )

        private val TOOLS_KEYWORDS = setOf(
            "settings",
            "camera",
            "gallery",
            "photos",
            "files",
            "browser",
            "chrome",
            "maps",
            "calculator",
            "clock",
            "contacts",
            "phone",
            "dialer",
            "launcher",
            "tools",
        )
    }
}
