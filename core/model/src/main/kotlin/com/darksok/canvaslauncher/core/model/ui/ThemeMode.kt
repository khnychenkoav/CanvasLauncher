package com.darksok.canvaslauncher.core.model.ui

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}

fun ThemeMode.resolveDarkTheme(isSystemDark: Boolean): Boolean {
    return when (this) {
        ThemeMode.SYSTEM -> isSystemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
}
