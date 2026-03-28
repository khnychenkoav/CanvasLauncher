package com.darksok.canvaslauncher.core.model.ui

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ThemeModeTest {

    @Test
    fun `system follows system dark flag`() {
        assertThat(ThemeMode.SYSTEM.resolveDarkTheme(isSystemDark = true)).isTrue()
        assertThat(ThemeMode.SYSTEM.resolveDarkTheme(isSystemDark = false)).isFalse()
    }

    @Test
    fun `light mode always resolves to light`() {
        assertThat(ThemeMode.LIGHT.resolveDarkTheme(isSystemDark = true)).isFalse()
        assertThat(ThemeMode.LIGHT.resolveDarkTheme(isSystemDark = false)).isFalse()
    }

    @Test
    fun `dark mode always resolves to dark`() {
        assertThat(ThemeMode.DARK.resolveDarkTheme(isSystemDark = true)).isTrue()
        assertThat(ThemeMode.DARK.resolveDarkTheme(isSystemDark = false)).isTrue()
    }
}
