package com.darksok.canvaslauncher.domain.repository

import com.darksok.canvaslauncher.core.model.ui.DarkThemePalette
import com.darksok.canvaslauncher.core.model.ui.LightThemePalette
import com.darksok.canvaslauncher.core.model.ui.ThemeMode
import kotlinx.coroutines.flow.Flow

interface ThemePreferencesRepository {
    fun observeThemeMode(): Flow<ThemeMode>
    fun observeLightThemePalette(): Flow<LightThemePalette>
    fun observeDarkThemePalette(): Flow<DarkThemePalette>
    suspend fun setThemeMode(themeMode: ThemeMode)
    suspend fun setLightThemePalette(palette: LightThemePalette)
    suspend fun setDarkThemePalette(palette: DarkThemePalette)
}
