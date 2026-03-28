package com.darksok.canvaslauncher.domain.usecase

import com.darksok.canvaslauncher.core.model.ui.DarkThemePalette
import com.darksok.canvaslauncher.core.model.ui.LightThemePalette
import com.darksok.canvaslauncher.core.model.ui.ThemeMode
import com.darksok.canvaslauncher.domain.repository.ThemePreferencesRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ThemePaletteUseCasesTest {

    @Test
    fun `observe palette use cases expose stored values`() = runTest {
        val repository = FakeThemePreferencesRepository()
        val observeLight = ObserveLightThemePaletteUseCase(repository)
        val observeDark = ObserveDarkThemePaletteUseCase(repository)

        assertThat(observeLight().first()).isEqualTo(LightThemePalette.MINT_GARDEN)
        assertThat(observeDark().first()).isEqualTo(DarkThemePalette.DEEP_OCEAN)
    }

    @Test
    fun `set palette use cases persist values`() = runTest {
        val repository = FakeThemePreferencesRepository()
        val setLight = SetLightThemePaletteUseCase(repository)
        val setDark = SetDarkThemePaletteUseCase(repository)

        setLight(LightThemePalette.ROSE_DAWN)
        setDark(DarkThemePalette.CHARCOAL_AMBER)

        assertThat(repository.observeLightThemePalette().first()).isEqualTo(LightThemePalette.ROSE_DAWN)
        assertThat(repository.observeDarkThemePalette().first()).isEqualTo(DarkThemePalette.CHARCOAL_AMBER)
    }

    private class FakeThemePreferencesRepository : ThemePreferencesRepository {
        private val modeState = MutableStateFlow(ThemeMode.SYSTEM)
        private val lightState = MutableStateFlow(LightThemePalette.MINT_GARDEN)
        private val darkState = MutableStateFlow(DarkThemePalette.DEEP_OCEAN)

        override fun observeThemeMode(): Flow<ThemeMode> = modeState
        override fun observeLightThemePalette(): Flow<LightThemePalette> = lightState
        override fun observeDarkThemePalette(): Flow<DarkThemePalette> = darkState

        override suspend fun setThemeMode(themeMode: ThemeMode) {
            modeState.value = themeMode
        }

        override suspend fun setLightThemePalette(palette: LightThemePalette) {
            lightState.value = palette
        }

        override suspend fun setDarkThemePalette(palette: DarkThemePalette) {
            darkState.value = palette
        }
    }
}
