package com.darksok.canvaslauncher.domain.support

import com.darksok.canvaslauncher.core.common.result.AppResult
import com.darksok.canvaslauncher.core.model.app.CanvasApp
import com.darksok.canvaslauncher.core.model.app.InstalledApp
import com.darksok.canvaslauncher.core.model.canvas.WorldPoint
import com.darksok.canvaslauncher.core.model.ui.AppLayoutMode
import com.darksok.canvaslauncher.core.model.ui.DarkThemePalette
import com.darksok.canvaslauncher.core.model.ui.LightThemePalette
import com.darksok.canvaslauncher.core.model.ui.ThemeMode
import com.darksok.canvaslauncher.domain.layout.InitialLayoutStrategy
import com.darksok.canvaslauncher.domain.repository.AppLaunchService
import com.darksok.canvaslauncher.domain.repository.CanvasAppsStore
import com.darksok.canvaslauncher.domain.repository.IconCacheGateway
import com.darksok.canvaslauncher.domain.repository.InstalledAppsSource
import com.darksok.canvaslauncher.domain.repository.LayoutPreferencesRepository
import com.darksok.canvaslauncher.domain.repository.ThemePreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf

class FakeInstalledAppsSource(
    private val items: List<InstalledApp> = emptyList(),
) : InstalledAppsSource {
    override suspend fun getInstalledApps(): List<InstalledApp> = items

    override suspend fun getInstalledApp(packageName: String): InstalledApp? {
        return items.firstOrNull { it.packageName == packageName }
    }
}

class FakeCanvasAppsStore(
    initial: List<CanvasApp> = emptyList(),
) : CanvasAppsStore {
    private val state = MutableStateFlow(initial)
    val upsertAppCalls = mutableListOf<CanvasApp>()
    val upsertAppsCalls = mutableListOf<List<CanvasApp>>()
    val removedPackagesCalls = mutableListOf<Set<String>>()
    val removedPackageCalls = mutableListOf<String>()
    val updatePositionCalls = mutableListOf<Pair<String, WorldPoint>>()

    override fun observeApps(): Flow<List<CanvasApp>> = state.asStateFlow()

    override suspend fun getAppsSnapshot(): List<CanvasApp> = state.value

    override suspend fun upsertApps(apps: List<CanvasApp>) {
        upsertAppsCalls += apps
        val current = state.value.associateBy { it.packageName }.toMutableMap()
        apps.forEach { current[it.packageName] = it }
        state.value = current.values.toList()
    }

    override suspend fun upsertApp(app: CanvasApp) {
        upsertAppCalls += app
        upsertApps(listOf(app))
    }

    override suspend fun removePackages(packages: Set<String>) {
        removedPackagesCalls += packages
        state.value = state.value.filterNot { it.packageName in packages }
    }

    override suspend fun removePackage(packageName: String) {
        removedPackageCalls += packageName
        state.value = state.value.filterNot { it.packageName == packageName }
    }

    override suspend fun updatePosition(packageName: String, position: WorldPoint) {
        updatePositionCalls += packageName to position
        state.value = state.value.map { app ->
            if (app.packageName == packageName) app.copy(position = position) else app
        }
    }

    fun snapshot(): List<CanvasApp> = state.value
}

class FakeLayoutStrategy(
    private val placement: (existingApps: List<CanvasApp>, newApps: List<InstalledApp>, center: WorldPoint, mode: AppLayoutMode) -> List<CanvasApp> = { _, newApps, center, _ ->
        newApps.mapIndexed { index, app ->
            CanvasApp(
                packageName = app.packageName,
                label = app.label,
                position = WorldPoint(center.x + index, center.y + index),
            )
        }
    },
) : InitialLayoutStrategy {
    val calls = mutableListOf<LayoutCall>()

    override fun layout(
        existingApps: List<CanvasApp>,
        newApps: List<InstalledApp>,
        center: WorldPoint,
        mode: AppLayoutMode,
    ): List<CanvasApp> {
        calls += LayoutCall(existingApps, newApps, center, mode)
        return placement(existingApps, newApps, center, mode)
    }
}

data class LayoutCall(
    val existingApps: List<CanvasApp>,
    val newApps: List<InstalledApp>,
    val center: WorldPoint,
    val mode: AppLayoutMode,
)

class FakeIconCacheGateway : IconCacheGateway {
    val preloaded = mutableListOf<Set<String>>()
    val invalidated = mutableListOf<String>()
    val removed = mutableListOf<String>()

    override suspend fun preload(packageNames: Collection<String>) {
        preloaded += packageNames.toSet()
    }

    override suspend fun invalidate(packageName: String) {
        invalidated += packageName
    }

    override suspend fun remove(packageName: String) {
        removed += packageName
    }
}

class FakeLayoutPreferencesRepository(
    initial: AppLayoutMode = AppLayoutMode.SPIRAL,
) : LayoutPreferencesRepository {
    private val state = MutableStateFlow(initial)
    val setCalls = mutableListOf<AppLayoutMode>()

    override fun observeLayoutMode(): Flow<AppLayoutMode> = state.asStateFlow()

    override suspend fun setLayoutMode(layoutMode: AppLayoutMode) {
        setCalls += layoutMode
        state.value = layoutMode
    }
}

class FakeThemePreferencesRepository(
    initialMode: ThemeMode = ThemeMode.SYSTEM,
    initialLight: LightThemePalette = LightThemePalette.SKY_BREEZE,
    initialDark: DarkThemePalette = DarkThemePalette.MIDNIGHT_BLUE,
) : ThemePreferencesRepository {
    private val modeState = MutableStateFlow(initialMode)
    private val lightState = MutableStateFlow(initialLight)
    private val darkState = MutableStateFlow(initialDark)
    val setModeCalls = mutableListOf<ThemeMode>()
    val setLightCalls = mutableListOf<LightThemePalette>()
    val setDarkCalls = mutableListOf<DarkThemePalette>()

    override fun observeThemeMode(): Flow<ThemeMode> = modeState.asStateFlow()
    override fun observeLightThemePalette(): Flow<LightThemePalette> = lightState.asStateFlow()
    override fun observeDarkThemePalette(): Flow<DarkThemePalette> = darkState.asStateFlow()

    override suspend fun setThemeMode(themeMode: ThemeMode) {
        setModeCalls += themeMode
        modeState.value = themeMode
    }

    override suspend fun setLightThemePalette(palette: LightThemePalette) {
        setLightCalls += palette
        lightState.value = palette
    }

    override suspend fun setDarkThemePalette(palette: DarkThemePalette) {
        setDarkCalls += palette
        darkState.value = palette
    }
}

class FakeAppLaunchService(
    private val result: AppResult<Unit>,
) : AppLaunchService {
    val calls = mutableListOf<String>()

    override suspend fun launch(packageName: String): AppResult<Unit> {
        calls += packageName
        return result
    }
}
