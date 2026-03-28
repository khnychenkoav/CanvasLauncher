package com.darksok.canvaslauncher.core.settings

import com.darksok.canvaslauncher.core.model.ui.ThemeMode

internal fun ThemeMode.toStoredValue(): String = name

internal fun String?.toThemeModeOrDefault(): ThemeMode {
    if (this == null) return ThemeMode.SYSTEM
    return ThemeMode.entries.firstOrNull { it.name == this } ?: ThemeMode.SYSTEM
}
