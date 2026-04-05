package com.darksok.canvaslauncher.core.settings

import com.darksok.canvaslauncher.core.model.ui.ThemeMode
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ThemeModePreferencesMapperTest {

    @Test
    fun `stored null value resolves to system`() {
        assertThat((null as String?).toThemeModeOrDefault()).isEqualTo(ThemeMode.SYSTEM)
    }

    @Test
    fun `stored invalid value resolves to system`() {
        assertThat("UNSUPPORTED".toThemeModeOrDefault()).isEqualTo(ThemeMode.SYSTEM)
    }

    @Test
    fun `stored lowercase value does not resolve`() {
        assertThat("light".toThemeModeOrDefault()).isEqualTo(ThemeMode.SYSTEM)
    }

    @Test
    fun `stored blank value does not resolve`() {
        assertThat("".toThemeModeOrDefault()).isEqualTo(ThemeMode.SYSTEM)
    }

    @Test
    fun `stored system value resolves correctly`() {
        assertThat("SYSTEM".toThemeModeOrDefault()).isEqualTo(ThemeMode.SYSTEM)
    }

    @Test
    fun `stored light value resolves correctly`() {
        assertThat("LIGHT".toThemeModeOrDefault()).isEqualTo(ThemeMode.LIGHT)
    }

    @Test
    fun `stored dark value resolves correctly`() {
        assertThat("DARK".toThemeModeOrDefault()).isEqualTo(ThemeMode.DARK)
    }

    @Test
    fun `system serializes to enum name`() {
        assertThat(ThemeMode.SYSTEM.toStoredValue()).isEqualTo("SYSTEM")
    }

    @Test
    fun `light serializes to enum name`() {
        assertThat(ThemeMode.LIGHT.toStoredValue()).isEqualTo("LIGHT")
    }

    @Test
    fun `dark serializes to enum name`() {
        assertThat(ThemeMode.DARK.toStoredValue()).isEqualTo("DARK")
    }
}
