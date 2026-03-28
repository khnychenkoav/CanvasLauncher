package com.darksok.canvaslauncher.core.settings

import com.darksok.canvaslauncher.core.model.ui.DarkThemePalette
import com.darksok.canvaslauncher.core.model.ui.LightThemePalette
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ThemePalettePreferencesMapperTest {

    @Test
    fun `null values resolve to default palettes`() {
        assertThat((null as String?).toLightThemePaletteOrDefault()).isEqualTo(LightThemePalette.SKY_BREEZE)
        assertThat((null as String?).toDarkThemePaletteOrDefault()).isEqualTo(DarkThemePalette.MIDNIGHT_BLUE)
    }

    @Test
    fun `invalid values resolve to default palettes`() {
        assertThat("invalid".toLightThemePaletteOrDefault()).isEqualTo(LightThemePalette.SKY_BREEZE)
        assertThat("invalid".toDarkThemePaletteOrDefault()).isEqualTo(DarkThemePalette.MIDNIGHT_BLUE)
    }

    @Test
    fun `palette values serialize to enum names`() {
        assertThat(LightThemePalette.MINT_GARDEN.toStoredValue()).isEqualTo("MINT_GARDEN")
        assertThat(DarkThemePalette.FOREST_NIGHT.toStoredValue()).isEqualTo("FOREST_NIGHT")
    }
}
