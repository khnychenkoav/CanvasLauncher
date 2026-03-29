package com.darksok.canvaslauncher.settings

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.darksok.canvaslauncher.R
import com.darksok.canvaslauncher.core.common.coroutines.DispatchersProvider
import com.darksok.canvaslauncher.core.database.dao.CanvasEditDao
import com.darksok.canvaslauncher.core.database.entity.CanvasFrameObjectEntity
import com.darksok.canvaslauncher.core.model.app.CanvasApp
import com.darksok.canvaslauncher.core.model.app.InstalledApp
import com.darksok.canvaslauncher.core.model.canvas.WorldPoint
import com.darksok.canvaslauncher.core.model.ui.AppLayoutMode
import com.darksok.canvaslauncher.core.model.ui.DarkThemePalette
import com.darksok.canvaslauncher.core.model.ui.LightThemePalette
import com.darksok.canvaslauncher.core.model.ui.ThemeMode
import com.darksok.canvaslauncher.core.packages.icon.IconBitmapStore
import com.darksok.canvaslauncher.domain.layout.SmartLayoutGrouping
import com.darksok.canvaslauncher.domain.repository.CanvasAppsStore
import com.darksok.canvaslauncher.domain.repository.IconCacheGateway
import com.darksok.canvaslauncher.domain.usecase.ObserveDarkThemePaletteUseCase
import com.darksok.canvaslauncher.domain.usecase.ObserveLayoutModeUseCase
import com.darksok.canvaslauncher.domain.usecase.ObserveLightThemePaletteUseCase
import com.darksok.canvaslauncher.domain.usecase.ObserveThemeModeUseCase
import com.darksok.canvaslauncher.domain.usecase.RearrangeAppsUseCase
import com.darksok.canvaslauncher.domain.usecase.SetDarkThemePaletteUseCase
import com.darksok.canvaslauncher.domain.usecase.SetLayoutModeUseCase
import com.darksok.canvaslauncher.domain.usecase.SetLightThemePaletteUseCase
import com.darksok.canvaslauncher.domain.usecase.SetThemeModeUseCase
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Locale
import javax.inject.Inject
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    observeThemeModeUseCase: ObserveThemeModeUseCase,
    observeLightThemePaletteUseCase: ObserveLightThemePaletteUseCase,
    observeDarkThemePaletteUseCase: ObserveDarkThemePaletteUseCase,
    observeLayoutModeUseCase: ObserveLayoutModeUseCase,
    private val setThemeModeUseCase: SetThemeModeUseCase,
    private val setLightThemePaletteUseCase: SetLightThemePaletteUseCase,
    private val setDarkThemePaletteUseCase: SetDarkThemePaletteUseCase,
    private val setLayoutModeUseCase: SetLayoutModeUseCase,
    private val rearrangeAppsUseCase: RearrangeAppsUseCase,
    private val appsStore: CanvasAppsStore,
    private val canvasEditDao: CanvasEditDao,
    private val iconCacheGateway: IconCacheGateway,
    private val iconBitmapStore: IconBitmapStore,
    private val dispatchersProvider: DispatchersProvider,
    @ApplicationContext private val appContext: Context,
) : ViewModel() {

    val uiState = combine(
        observeThemeModeUseCase(),
        observeLightThemePaletteUseCase(),
        observeDarkThemePaletteUseCase(),
        observeLayoutModeUseCase(),
    ) { themeMode, lightPalette, darkPalette, layoutMode ->
        SettingsUiState(
            themeMode = themeMode,
            lightPalette = lightPalette,
            darkPalette = darkPalette,
            layoutMode = layoutMode,
        )
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SettingsUiState(),
        )

    fun onThemeModeSelected(themeMode: ThemeMode) {
        viewModelScope.launch(dispatchersProvider.io) {
            setThemeModeUseCase(themeMode)
        }
    }

    fun onLightPaletteSelected(palette: LightThemePalette) {
        viewModelScope.launch(dispatchersProvider.io) {
            setLightThemePaletteUseCase(palette)
        }
    }

    fun onDarkPaletteSelected(palette: DarkThemePalette) {
        viewModelScope.launch(dispatchersProvider.io) {
            setDarkThemePaletteUseCase(palette)
        }
    }

    fun onLayoutModeSelected(layoutMode: AppLayoutMode) {
        viewModelScope.launch(dispatchersProvider.io) {
            setLayoutModeUseCase(layoutMode)
            when (layoutMode) {
                AppLayoutMode.ICON_COLOR -> rearrangeByIconColor()
                else -> rearrangeAppsUseCase(layoutMode)
            }
            upsertAutoLayoutFrames(layoutMode)
        }
    }

    private suspend fun rearrangeByIconColor() {
        val snapshot = appsStore.getAppsSnapshot()
            .sortedBy { app -> app.label.lowercase(Locale.ROOT) }
        if (snapshot.isEmpty()) return

        val colorByPackage = detectColorGroups(snapshot.map { app -> app.packageName })
        val localizedTitles = IconColorGroup.entries.associateWith { group -> iconColorTitle(group) }
        val grouped = snapshot
            .groupBy { app -> colorByPackage[app.packageName] ?: IconColorGroup.MONOCHROME }
            .entries
            .sortedWith(
                compareBy<Map.Entry<IconColorGroup, List<CanvasApp>>> { entry -> entry.key.sortOrder }
                    .thenByDescending { entry -> entry.value.size }
                    .thenBy { entry -> localizedTitles[entry.key].orEmpty() },
            )
            .map { entry ->
                AppGroupForLayout(
                    id = entry.key.id,
                    title = localizedTitles[entry.key].orEmpty(),
                    colorArgb = entry.key.frameColorArgb,
                    apps = entry.value.sortedBy { app -> app.label.lowercase(Locale.ROOT) },
                )
            }

        val positioned = computeGroupedPositions(
            groups = grouped,
            center = WorldPoint(0f, 0f),
        )
        val rearranged = snapshot.map { app ->
            app.copy(position = positioned[app.packageName] ?: app.position)
        }
        appsStore.upsertApps(rearranged)
    }

    private suspend fun upsertAutoLayoutFrames(layoutMode: AppLayoutMode) {
        val existingAutoFrames = canvasEditDao.getFrameObjects()
            .filter { frame -> frame.id.startsWith(AUTO_FRAME_PREFIX) }

        if (layoutMode != AppLayoutMode.SMART_AUTO && layoutMode != AppLayoutMode.ICON_COLOR) {
            existingAutoFrames.forEach { frame -> canvasEditDao.deleteFrameObjectById(frame.id) }
            return
        }

        val snapshot = appsStore.getAppsSnapshot()
        if (snapshot.isEmpty()) {
            existingAutoFrames.forEach { frame -> canvasEditDao.deleteFrameObjectById(frame.id) }
            return
        }

        val groups = when (layoutMode) {
            AppLayoutMode.SMART_AUTO -> {
                SmartLayoutGrouping.group(
                    snapshot.map { app ->
                        InstalledApp(packageName = app.packageName, label = app.label)
                    },
                ).map { group ->
                    AppGroupForLayout(
                        id = group.id,
                        title = smartGroupTitle(group.id, group.title),
                        colorArgb = group.colorArgb,
                        apps = group.apps.mapNotNull { grouped ->
                            snapshot.firstOrNull { app -> app.packageName == grouped.packageName }
                        },
                    )
                }
            }

            AppLayoutMode.ICON_COLOR -> {
                val colorByPackage = detectColorGroups(snapshot.map { app -> app.packageName })
                val localizedTitles = IconColorGroup.entries.associateWith { group -> iconColorTitle(group) }
                snapshot
                    .groupBy { app -> colorByPackage[app.packageName] ?: IconColorGroup.MONOCHROME }
                    .entries
                    .sortedWith(
                        compareBy<Map.Entry<IconColorGroup, List<CanvasApp>>> { entry -> entry.key.sortOrder }
                            .thenByDescending { entry -> entry.value.size }
                            .thenBy { entry -> localizedTitles[entry.key].orEmpty() },
                    )
                    .map { entry ->
                        AppGroupForLayout(
                            id = entry.key.id,
                            title = localizedTitles[entry.key].orEmpty(),
                            colorArgb = entry.key.frameColorArgb,
                            apps = entry.value.sortedBy { app -> app.label.lowercase(Locale.ROOT) },
                        )
                    }
            }

            else -> emptyList()
        }

        val nextFrames = groups.mapNotNull { group ->
            if (group.apps.isEmpty()) return@mapNotNull null

            val minX = group.apps.minOf { app -> app.position.x }
            val maxX = group.apps.maxOf { app -> app.position.x }
            val minY = group.apps.minOf { app -> app.position.y }
            val maxY = group.apps.maxOf { app -> app.position.y }

            val left = minX - FRAME_PADDING_WORLD
            val right = maxX + FRAME_PADDING_WORLD
            val top = minY - FRAME_PADDING_WORLD - FRAME_TOP_TITLE_EXTRA_WORLD
            val bottom = maxY + FRAME_PADDING_WORLD
            val width = (right - left).coerceAtLeast(FRAME_MIN_WIDTH_WORLD)
            val height = (bottom - top).coerceAtLeast(FRAME_MIN_HEIGHT_WORLD)

            val id = "$AUTO_FRAME_PREFIX${layoutMode.name.lowercase(Locale.ROOT)}-${stableSlug(group.id)}"

            CanvasFrameObjectEntity(
                id = id,
                title = group.title,
                centerX = (left + right) / 2f,
                centerY = (top + bottom) / 2f,
                widthWorld = width,
                heightWorld = height,
                colorArgb = group.colorArgb,
            )
        }

        val nextIds = nextFrames.mapTo(HashSet()) { frame -> frame.id }
        existingAutoFrames
            .filter { frame -> frame.id !in nextIds }
            .forEach { frame -> canvasEditDao.deleteFrameObjectById(frame.id) }
        nextFrames.forEach { frame -> canvasEditDao.upsertFrameObject(frame) }
    }

    private suspend fun detectColorGroups(packageNames: List<String>): Map<String, IconColorGroup> {
        val distinctPackages = packageNames.distinct()
        if (distinctPackages.isEmpty()) return emptyMap()
        iconCacheGateway.preload(distinctPackages)
        return distinctPackages.associateWith { packageName ->
            classifyIconColor(iconBitmapStore.getCached(packageName))
        }
    }

    private fun classifyIconColor(bitmap: Bitmap?): IconColorGroup {
        if (bitmap == null || bitmap.width == 0 || bitmap.height == 0) {
            return IconColorGroup.MONOCHROME
        }

        val hsv = FloatArray(3)
        val step = max(1, min(bitmap.width, bitmap.height) / ICON_COLOR_SAMPLE_DIVISOR)
        var chromaWeight = 0f
        var achromaticWeight = 0f
        var hueX = 0f
        var hueY = 0f
        var valueWeighted = 0f
        var saturationWeighted = 0f

        var y = 0
        while (y < bitmap.height) {
            var x = 0
            while (x < bitmap.width) {
                val argb = bitmap.getPixel(x, y)
                val alpha = Color.alpha(argb) / 255f
                if (alpha >= ICON_MIN_ALPHA) {
                    Color.colorToHSV(argb, hsv)
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

    private fun computeGroupedPositions(
        groups: List<AppGroupForLayout>,
        center: WorldPoint,
    ): Map<String, WorldPoint> {
        if (groups.isEmpty()) return emptyMap()
        val activeGroups = groups.filter { group -> group.apps.isNotEmpty() }
        if (activeGroups.isEmpty()) return emptyMap()

        val prepared = activeGroups.map { group ->
            val columns = ceil(sqrt(group.apps.size.toDouble())).toInt().coerceAtLeast(2)
            val rows = ceil(group.apps.size / columns.toDouble()).toInt().coerceAtLeast(1)
            PositionedGroup(
                source = group,
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

            group.source.apps.forEachIndexed { index, app ->
                val itemRow = index / group.columns
                val itemCol = index % group.columns
                val x = groupCenter.x + (itemCol - (group.columns - 1) / 2f) * GRID_STEP_WORLD
                val y = groupCenter.y + (itemRow - (group.rows - 1) / 2f) * GRID_STEP_WORLD
                positionByPackage[app.packageName] = WorldPoint(x, y)
            }
        }
        return positionByPackage
    }

    private fun stableSlug(raw: String): String {
        val normalized = raw.lowercase(Locale.ROOT)
            .replace("[^a-z0-9]+".toRegex(), "-")
            .trim('-')
        if (normalized.isNotBlank()) return normalized
        return raw.hashCode().toString().replace('-', 'n')
    }

    private fun iconColorTitle(group: IconColorGroup): String {
        return appContext.getString(group.titleResId)
    }

    private fun smartGroupTitle(groupId: String, fallbackTitle: String): String {
        val titleResId = when (groupId) {
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
        return titleResId?.let(appContext::getString) ?: fallbackTitle
    }

    private data class AppGroupForLayout(
        val id: String,
        val title: String,
        val colorArgb: Int,
        val apps: List<CanvasApp>,
    )

    private data class PositionedGroup(
        val source: AppGroupForLayout,
        val columns: Int,
        val rows: Int,
    )

    private enum class IconColorGroup(
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

    private companion object {
        private const val AUTO_FRAME_PREFIX = "auto-layout-frame-"

        private const val GRID_STEP_WORLD = 136f
        private const val GROUP_PADDING_WORLD = 48f
        private const val GROUP_GAP_WORLD = 208f

        private const val FRAME_PADDING_WORLD = 106f
        private const val FRAME_TOP_TITLE_EXTRA_WORLD = 62f
        private const val FRAME_MIN_WIDTH_WORLD = 320f
        private const val FRAME_MIN_HEIGHT_WORLD = 260f

        private const val ICON_COLOR_SAMPLE_DIVISOR = 22
        private const val ICON_MIN_ALPHA = 0.16f
        private const val ICON_MONO_SATURATION_THRESHOLD = 0.16f
        private const val ICON_MONO_VALUE_THRESHOLD = 0.18f
        private const val ICON_MONO_DOMINANCE_FACTOR = 1.28f
    }
}
