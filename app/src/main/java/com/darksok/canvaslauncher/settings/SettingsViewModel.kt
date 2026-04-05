package com.darksok.canvaslauncher.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
            SettingsLayoutAlgorithms.classifyIconColor(iconBitmapStore.getCached(packageName))
        }
    }

    private fun computeGroupedPositions(
        groups: List<AppGroupForLayout>,
        center: WorldPoint,
    ): Map<String, WorldPoint> {
        return SettingsLayoutAlgorithms.computeGroupedPositions(
            groups = groups.map { group -> group.apps },
            center = center,
        )
    }

    private fun stableSlug(raw: String): String {
        return SettingsLayoutAlgorithms.stableSlug(raw)
    }

    private fun iconColorTitle(group: IconColorGroup): String {
        return appContext.getString(group.titleResId)
    }

    private fun smartGroupTitle(groupId: String, fallbackTitle: String): String {
        val titleResId = SettingsLayoutAlgorithms.smartGroupTitleResId(groupId)
        return titleResId?.let(appContext::getString) ?: fallbackTitle
    }

    private data class AppGroupForLayout(
        val id: String,
        val title: String,
        val colorArgb: Int,
        val apps: List<CanvasApp>,
    )

    private companion object {
        private const val AUTO_FRAME_PREFIX = "auto-layout-frame-"

        private const val FRAME_PADDING_WORLD = 106f
        private const val FRAME_TOP_TITLE_EXTRA_WORLD = 62f
        private const val FRAME_MIN_WIDTH_WORLD = 320f
        private const val FRAME_MIN_HEIGHT_WORLD = 260f
    }
}
