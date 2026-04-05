package com.darksok.canvaslauncher.settings

import android.graphics.Bitmap
import com.darksok.canvaslauncher.R
import com.darksok.canvaslauncher.core.model.app.CanvasApp
import com.darksok.canvaslauncher.core.model.canvas.WorldPoint
import java.util.Locale
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

internal object SettingsLayoutAlgorithms {

    fun classifyIconColor(bitmap: Bitmap?): IconColorGroup {
        if (bitmap == null) return IconColorGroup.MONOCHROME
        return classifyIconColor(
            width = bitmap.width,
            height = bitmap.height,
            pixelAt = bitmap::getPixel,
        )
    }

    fun classifyIconColor(
        width: Int,
        height: Int,
        pixelAt: (x: Int, y: Int) -> Int,
    ): IconColorGroup {
        if (width <= 0 || height <= 0) {
            return IconColorGroup.MONOCHROME
        }

        val hsv = FloatArray(3)
        val step = max(1, min(width, height) / ICON_COLOR_SAMPLE_DIVISOR)
        var chromaWeight = 0f
        var achromaticWeight = 0f
        var hueX = 0f
        var hueY = 0f
        var valueWeighted = 0f
        var saturationWeighted = 0f

        var y = 0
        while (y < height) {
            var x = 0
            while (x < width) {
                val argb = pixelAt(x, y)
                val alpha = alpha(argb) / 255f
                if (alpha >= ICON_MIN_ALPHA) {
                    colorToHsv(argb, hsv)
                    val saturation = hsv[1]
                    val value = hsv[2]
                    val weight = alpha * (0.30f + saturation * 0.70f) * (0.20f + value * 0.80f)
                    if (weight > 0f) {
                        if (saturation < ICON_MONO_SATURATION_THRESHOLD || value < ICON_MONO_VALUE_THRESHOLD) {
                            achromaticWeight += weight
                        } else {
                            chromaWeight += weight
                            saturationWeighted += saturation * weight
                            valueWeighted += value * weight
                            val hueRad = (hsv[0].toDouble() / 180.0) * PI
                            hueX += (cos(hueRad) * weight).toFloat()
                            hueY += (sin(hueRad) * weight).toFloat()
                        }
                    }
                }
                x += step
            }
            y += step
        }

        if (chromaWeight <= 0.0001f || achromaticWeight > chromaWeight * ICON_MONO_DOMINANCE_FACTOR) {
            return IconColorGroup.MONOCHROME
        }

        val hue = ((Math.toDegrees(atan2(hueY.toDouble(), hueX.toDouble())) + 360.0) % 360.0).toFloat()
        val avgValue = valueWeighted / chromaWeight
        val avgSaturation = saturationWeighted / chromaWeight

        if (hue in 18f..46f && avgValue < 0.58f && avgSaturation > 0.25f) {
            return IconColorGroup.BROWN
        }

        return when {
            hue < 15f || hue >= 345f -> IconColorGroup.RED
            hue < 40f -> IconColorGroup.ORANGE
            hue < 62f -> IconColorGroup.YELLOW
            hue < 150f -> IconColorGroup.GREEN
            hue < 195f -> IconColorGroup.CYAN
            hue < 255f -> IconColorGroup.BLUE
            hue < 300f -> IconColorGroup.PURPLE
            else -> IconColorGroup.PINK
        }
    }

    fun computeGroupedPositions(
        groups: List<List<CanvasApp>>,
        center: WorldPoint,
    ): Map<String, WorldPoint> {
        if (groups.isEmpty()) return emptyMap()
        val activeGroups = groups.filter { group -> group.isNotEmpty() }
        if (activeGroups.isEmpty()) return emptyMap()

        val prepared = activeGroups.map { group ->
            val columns = ceil(sqrt(group.size.toDouble())).toInt().coerceAtLeast(2)
            val rows = ceil(group.size / columns.toDouble()).toInt().coerceAtLeast(1)
            PositionedGroup(
                apps = group,
                columns = columns,
                rows = rows,
            )
        }

        val maxColumns = prepared.maxOf { group -> group.columns }.coerceAtLeast(3)
        val maxRows = prepared.maxOf { group -> group.rows }.coerceAtLeast(2)
        val groupWidth = (maxColumns - 1) * GRID_STEP_WORLD + GROUP_PADDING_WORLD * 2f
        val groupHeight = (maxRows - 1) * GRID_STEP_WORLD + GROUP_PADDING_WORLD * 2f

        val groupCount = prepared.size
        val groupsPerRow = ceil(sqrt(groupCount.toDouble())).toInt().coerceAtLeast(1)
        val totalRows = ceil(groupCount / groupsPerRow.toDouble()).toInt().coerceAtLeast(1)

        val positionByPackage = mutableMapOf<String, WorldPoint>()
        prepared.forEachIndexed { groupIndex, group ->
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

    fun stableSlug(raw: String): String {
        val normalized = raw.lowercase(Locale.ROOT)
            .replace("[^a-z0-9]+".toRegex(), "-")
            .trim('-')
        if (normalized.isNotBlank()) return normalized
        return raw.hashCode().toString().replace('-', 'n')
    }

    fun smartGroupTitleResId(groupId: String): Int? {
        return when (groupId) {
            "communication" -> R.string.smart_group_communication
            "social" -> R.string.smart_group_social
            "media" -> R.string.smart_group_media
            "games" -> R.string.smart_group_games
            "work" -> R.string.smart_group_work
            "finance" -> R.string.smart_group_finance
            "shopping" -> R.string.smart_group_shopping
            "travel" -> R.string.smart_group_travel
            "health" -> R.string.smart_group_health
            "education" -> R.string.smart_group_education
            "system" -> R.string.smart_group_system
            "utilities" -> R.string.smart_group_utilities
            else -> null
        }
    }

    private data class PositionedGroup(
        val apps: List<CanvasApp>,
        val columns: Int,
        val rows: Int,
    )

    private fun alpha(argb: Int): Int = argb ushr 24 and 0xFF

    private fun colorToHsv(
        argb: Int,
        outHsv: FloatArray,
    ) {
        val red = ((argb ushr 16) and 0xFF) / 255f
        val green = ((argb ushr 8) and 0xFF) / 255f
        val blue = (argb and 0xFF) / 255f

        val maxChannel = max(red, max(green, blue))
        val minChannel = min(red, min(green, blue))
        val delta = maxChannel - minChannel

        val hue = when {
            delta == 0f -> 0f
            maxChannel == red -> 60f * (((green - blue) / delta) % 6f)
            maxChannel == green -> 60f * (((blue - red) / delta) + 2f)
            else -> 60f * (((red - green) / delta) + 4f)
        }.let { value ->
            if (value < 0f) value + 360f else value
        }

        val saturation = if (maxChannel == 0f) 0f else delta / maxChannel

        outHsv[0] = hue
        outHsv[1] = saturation
        outHsv[2] = maxChannel
    }

    private const val GRID_STEP_WORLD = 136f
    private const val GROUP_PADDING_WORLD = 48f
    private const val GROUP_GAP_WORLD = 208f

    private const val ICON_COLOR_SAMPLE_DIVISOR = 22
    private const val ICON_MIN_ALPHA = 0.16f
    private const val ICON_MONO_SATURATION_THRESHOLD = 0.16f
    private const val ICON_MONO_VALUE_THRESHOLD = 0.18f
    private const val ICON_MONO_DOMINANCE_FACTOR = 1.28f
}

internal enum class IconColorGroup(
    val id: String,
    val titleResId: Int,
    val frameColorArgb: Int,
    val sortOrder: Int,
) {
    RED(
        id = "red",
        titleResId = R.string.icon_color_group_red,
        frameColorArgb = 0xFFE53935.toInt(),
        sortOrder = 10,
    ),
    ORANGE(
        id = "orange",
        titleResId = R.string.icon_color_group_orange,
        frameColorArgb = 0xFFFB8C00.toInt(),
        sortOrder = 20,
    ),
    YELLOW(
        id = "yellow",
        titleResId = R.string.icon_color_group_yellow,
        frameColorArgb = 0xFFFDD835.toInt(),
        sortOrder = 30,
    ),
    GREEN(
        id = "green",
        titleResId = R.string.icon_color_group_green,
        frameColorArgb = 0xFF43A047.toInt(),
        sortOrder = 40,
    ),
    CYAN(
        id = "cyan",
        titleResId = R.string.icon_color_group_cyan,
        frameColorArgb = 0xFF00ACC1.toInt(),
        sortOrder = 50,
    ),
    BLUE(
        id = "blue",
        titleResId = R.string.icon_color_group_blue,
        frameColorArgb = 0xFF1E88E5.toInt(),
        sortOrder = 60,
    ),
    PURPLE(
        id = "purple",
        titleResId = R.string.icon_color_group_purple,
        frameColorArgb = 0xFF8E24AA.toInt(),
        sortOrder = 70,
    ),
    PINK(
        id = "pink",
        titleResId = R.string.icon_color_group_pink,
        frameColorArgb = 0xFFD81B60.toInt(),
        sortOrder = 80,
    ),
    BROWN(
        id = "brown",
        titleResId = R.string.icon_color_group_brown,
        frameColorArgb = 0xFF6D4C41.toInt(),
        sortOrder = 90,
    ),
    MONOCHROME(
        id = "monochrome",
        titleResId = R.string.icon_color_group_monochrome,
        frameColorArgb = 0xFF546E7A.toInt(),
        sortOrder = 100,
    ),
}
