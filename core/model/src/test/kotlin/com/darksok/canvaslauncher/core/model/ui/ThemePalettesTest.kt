package com.darksok.canvaslauncher.core.model.ui

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ThemePalettesTest {

    @Test
    fun `dark palette contains midnight blue`() {
        assertThat(DarkThemePalette.values()).asList().contains(DarkThemePalette.MIDNIGHT_BLUE)
    }

    @Test
    fun `dark palette contains deep ocean`() {
        assertThat(DarkThemePalette.values()).asList().contains(DarkThemePalette.DEEP_OCEAN)
    }

    @Test
    fun `dark palette contains forest night`() {
        assertThat(DarkThemePalette.values()).asList().contains(DarkThemePalette.FOREST_NIGHT)
    }

    @Test
    fun `dark palette contains charcoal amber`() {
        assertThat(DarkThemePalette.values()).asList().contains(DarkThemePalette.CHARCOAL_AMBER)
    }

    @Test
    fun `light palette contains sky breeze`() {
        assertThat(LightThemePalette.values()).asList().contains(LightThemePalette.SKY_BREEZE)
    }

    @Test
    fun `light palette contains mint garden`() {
        assertThat(LightThemePalette.values()).asList().contains(LightThemePalette.MINT_GARDEN)
    }

    @Test
    fun `light palette contains sunset glow`() {
        assertThat(LightThemePalette.values()).asList().contains(LightThemePalette.SUNSET_GLOW)
    }

    @Test
    fun `light palette contains rose dawn`() {
        assertThat(LightThemePalette.values()).asList().contains(LightThemePalette.ROSE_DAWN)
    }

    @Test
    fun `dark palette order is stable`() {
        assertThat(DarkThemePalette.values().map { it.name }).containsExactly(
            "MIDNIGHT_BLUE",
            "DEEP_OCEAN",
            "FOREST_NIGHT",
            "CHARCOAL_AMBER",
        ).inOrder()
    }

    @Test
    fun `light palette order is stable`() {
        assertThat(LightThemePalette.values().map { it.name }).containsExactly(
            "SKY_BREEZE",
            "MINT_GARDEN",
            "SUNSET_GLOW",
            "ROSE_DAWN",
        ).inOrder()
    }

    @Test
    fun `dark palette entries mirror values order`() {
        assertThat(DarkThemePalette.entries.map { it.name }).containsExactly(
            "MIDNIGHT_BLUE",
            "DEEP_OCEAN",
            "FOREST_NIGHT",
            "CHARCOAL_AMBER",
        ).inOrder()
    }

    @Test
    fun `light palette entries mirror values order`() {
        assertThat(LightThemePalette.entries.map { it.name }).containsExactly(
            "SKY_BREEZE",
            "MINT_GARDEN",
            "SUNSET_GLOW",
            "ROSE_DAWN",
        ).inOrder()
    }
}
