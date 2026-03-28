package com.darksok.canvaslauncher.defaultscreen

import com.darksok.canvaslauncher.core.model.ui.DarkThemePalette
import com.darksok.canvaslauncher.core.model.ui.LightThemePalette
import com.darksok.canvaslauncher.core.model.ui.ThemeMode

data class DefaultUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val lightPalette: LightThemePalette = LightThemePalette.SKY_BREEZE,
    val darkPalette: DarkThemePalette = DarkThemePalette.MIDNIGHT_BLUE,
)
