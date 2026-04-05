package com.darksok.canvaslauncher.core.model.ui

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ThemeModeTest {

    @Test
    fun `system follows system dark flag when true`() {
        assertThat(ThemeMode.SYSTEM.resolveDarkTheme(isSystemDark = true)).isTrue()
    }

    @Test
    fun `system follows system dark flag when false`() {
        assertThat(ThemeMode.SYSTEM.resolveDarkTheme(isSystemDark = false)).isFalse()
    }

    @Test
    fun `light mode always resolves to light when system dark`() {
        assertThat(ThemeMode.LIGHT.resolveDarkTheme(isSystemDark = true)).isFalse()
    }

    @Test
    fun `light mode always resolves to light when system light`() {
        assertThat(ThemeMode.LIGHT.resolveDarkTheme(isSystemDark = false)).isFalse()
    }

    @Test
    fun `dark mode always resolves to dark when system dark`() {
        assertThat(ThemeMode.DARK.resolveDarkTheme(isSystemDark = true)).isTrue()
    }

    @Test
    fun `dark mode always resolves to dark when system light`() {
        assertThat(ThemeMode.DARK.resolveDarkTheme(isSystemDark = false)).isTrue()
    }

    @Test
    fun `theme mode values order is stable`() {
        assertThat(ThemeMode.values().map { it.name }).containsExactly(
            "SYSTEM",
            "LIGHT",
            "DARK",
        ).inOrder()
    }

    @Test
    fun `value of dark returns dark`() {
        assertThat(ThemeMode.valueOf("DARK")).isEqualTo(ThemeMode.DARK)
    }

    @Test
    fun `theme mode entries mirror values order`() {
        assertThat(ThemeMode.entries.map { it.name }).containsExactly(
            "SYSTEM",
            "LIGHT",
            "DARK",
        ).inOrder()
    }
}
