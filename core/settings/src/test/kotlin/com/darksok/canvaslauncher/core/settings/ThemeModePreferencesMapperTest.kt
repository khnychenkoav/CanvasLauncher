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
    fun `stored enum name resolves to expected theme`() {
        assertThat("LIGHT".toThemeModeOrDefault()).isEqualTo(ThemeMode.LIGHT)
        assertThat("DARK".toThemeModeOrDefault()).isEqualTo(ThemeMode.DARK)
    }

    @Test
    fun `theme mode serializes to enum name`() {
        assertThat(ThemeMode.SYSTEM.toStoredValue()).isEqualTo("SYSTEM")
        assertThat(ThemeMode.DARK.toStoredValue()).isEqualTo("DARK")
    }
}
