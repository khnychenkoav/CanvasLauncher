package com.darksok.canvaslauncher.core.settings

import com.darksok.canvaslauncher.core.model.ui.AppLayoutMode

internal fun AppLayoutMode.toStoredValue(): String = name

internal fun String?.toLayoutModeOrDefault(): AppLayoutMode {
    if (this == null) return AppLayoutMode.SPIRAL
    return AppLayoutMode.entries.firstOrNull { it.name == this } ?: AppLayoutMode.SPIRAL
}
