package com.darksok.canvaslauncher.core.settings

import com.darksok.canvaslauncher.core.model.ui.DarkThemePalette
import com.darksok.canvaslauncher.core.model.ui.LightThemePalette

internal fun LightThemePalette.toStoredValue(): String = name

internal fun DarkThemePalette.toStoredValue(): String = name

internal fun String?.toLightThemePaletteOrDefault(): LightThemePalette {
    if (this == null) return LightThemePalette.SKY_BREEZE
    return LightThemePalette.entries.firstOrNull { it.name == this } ?: LightThemePalette.SKY_BREEZE
}

internal fun String?.toDarkThemePaletteOrDefault(): DarkThemePalette {
    if (this == null) return DarkThemePalette.MIDNIGHT_BLUE
    return DarkThemePalette.entries.firstOrNull { it.name == this } ?: DarkThemePalette.MIDNIGHT_BLUE
}
