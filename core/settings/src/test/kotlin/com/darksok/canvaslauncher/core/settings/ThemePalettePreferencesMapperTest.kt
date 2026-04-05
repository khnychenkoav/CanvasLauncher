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
    fun `blank values resolve to default palettes`() {
        assertThat("".toLightThemePaletteOrDefault()).isEqualTo(LightThemePalette.SKY_BREEZE)
        assertThat("".toDarkThemePaletteOrDefault()).isEqualTo(DarkThemePalette.MIDNIGHT_BLUE)
    }

    @Test
    fun `sky breeze resolves correctly`() {
        assertThat("SKY_BREEZE".toLightThemePaletteOrDefault()).isEqualTo(LightThemePalette.SKY_BREEZE)
    }

    @Test
    fun `mint garden resolves correctly`() {
        assertThat("MINT_GARDEN".toLightThemePaletteOrDefault()).isEqualTo(LightThemePalette.MINT_GARDEN)
    }

    @Test
    fun `sunset glow resolves correctly`() {
        assertThat("SUNSET_GLOW".toLightThemePaletteOrDefault()).isEqualTo(LightThemePalette.SUNSET_GLOW)
    }

    @Test
    fun `rose dawn resolves correctly`() {
        assertThat("ROSE_DAWN".toLightThemePaletteOrDefault()).isEqualTo(LightThemePalette.ROSE_DAWN)
    }

    @Test
    fun `midnight blue resolves correctly`() {
        assertThat("MIDNIGHT_BLUE".toDarkThemePaletteOrDefault()).isEqualTo(DarkThemePalette.MIDNIGHT_BLUE)
    }

    @Test
    fun `deep ocean resolves correctly`() {
        assertThat("DEEP_OCEAN".toDarkThemePaletteOrDefault()).isEqualTo(DarkThemePalette.DEEP_OCEAN)
    }

    @Test
    fun `forest night resolves correctly`() {
        assertThat("FOREST_NIGHT".toDarkThemePaletteOrDefault()).isEqualTo(DarkThemePalette.FOREST_NIGHT)
    }

    @Test
    fun `charcoal amber resolves correctly`() {
        assertThat("CHARCOAL_AMBER".toDarkThemePaletteOrDefault()).isEqualTo(DarkThemePalette.CHARCOAL_AMBER)
    }

    @Test
    fun `light palettes serialize to enum names`() {
        assertThat(LightThemePalette.SKY_BREEZE.toStoredValue()).isEqualTo("SKY_BREEZE")
        assertThat(LightThemePalette.MINT_GARDEN.toStoredValue()).isEqualTo("MINT_GARDEN")
        assertThat(LightThemePalette.SUNSET_GLOW.toStoredValue()).isEqualTo("SUNSET_GLOW")
        assertThat(LightThemePalette.ROSE_DAWN.toStoredValue()).isEqualTo("ROSE_DAWN")
    }

    @Test
    fun `dark palettes serialize to enum names`() {
        assertThat(DarkThemePalette.MIDNIGHT_BLUE.toStoredValue()).isEqualTo("MIDNIGHT_BLUE")
        assertThat(DarkThemePalette.DEEP_OCEAN.toStoredValue()).isEqualTo("DEEP_OCEAN")
        assertThat(DarkThemePalette.FOREST_NIGHT.toStoredValue()).isEqualTo("FOREST_NIGHT")
        assertThat(DarkThemePalette.CHARCOAL_AMBER.toStoredValue()).isEqualTo("CHARCOAL_AMBER")
    }
}
