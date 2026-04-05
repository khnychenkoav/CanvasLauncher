package com.darksok.canvaslauncher.domain.usecase

import com.darksok.canvaslauncher.core.model.ui.DarkThemePalette
import com.darksok.canvaslauncher.core.model.ui.LightThemePalette
import com.darksok.canvaslauncher.domain.support.FakeThemePreferencesRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ThemePaletteUseCasesTest {

    @Test
    fun `observe light palette exposes stored value`() = runTest {
        val repository = FakeThemePreferencesRepository(initialLight = LightThemePalette.MINT_GARDEN)
        assertThat(ObserveLightThemePaletteUseCase(repository)().first()).isEqualTo(LightThemePalette.MINT_GARDEN)
    }

    @Test
    fun `observe dark palette exposes stored value`() = runTest {
        val repository = FakeThemePreferencesRepository(initialDark = DarkThemePalette.DEEP_OCEAN)
        assertThat(ObserveDarkThemePaletteUseCase(repository)().first()).isEqualTo(DarkThemePalette.DEEP_OCEAN)
    }

    @Test
    fun `set light palette persists value`() = runTest {
        val repository = FakeThemePreferencesRepository()
        SetLightThemePaletteUseCase(repository)(LightThemePalette.ROSE_DAWN)
        assertThat(repository.observeLightThemePalette().first()).isEqualTo(LightThemePalette.ROSE_DAWN)
    }

    @Test
    fun `set dark palette persists value`() = runTest {
        val repository = FakeThemePreferencesRepository()
        SetDarkThemePaletteUseCase(repository)(DarkThemePalette.CHARCOAL_AMBER)
        assertThat(repository.observeDarkThemePalette().first()).isEqualTo(DarkThemePalette.CHARCOAL_AMBER)
    }

    @Test
    fun `set light palette records call`() = runTest {
        val repository = FakeThemePreferencesRepository()
        SetLightThemePaletteUseCase(repository)(LightThemePalette.SUNSET_GLOW)
        assertThat(repository.setLightCalls).containsExactly(LightThemePalette.SUNSET_GLOW)
    }

    @Test
    fun `set dark palette records call`() = runTest {
        val repository = FakeThemePreferencesRepository()
        SetDarkThemePaletteUseCase(repository)(DarkThemePalette.FOREST_NIGHT)
        assertThat(repository.setDarkCalls).containsExactly(DarkThemePalette.FOREST_NIGHT)
    }
}
