package com.darksok.canvaslauncher.settings

import com.darksok.canvaslauncher.core.model.ui.AppLayoutMode
import com.darksok.canvaslauncher.core.model.ui.DarkThemePalette
import com.darksok.canvaslauncher.core.model.ui.LightThemePalette
import com.darksok.canvaslauncher.core.model.ui.ThemeMode

data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val lightPalette: LightThemePalette = LightThemePalette.SKY_BREEZE,
    val darkPalette: DarkThemePalette = DarkThemePalette.MIDNIGHT_BLUE,
    val layoutMode: AppLayoutMode = AppLayoutMode.SPIRAL,
)
