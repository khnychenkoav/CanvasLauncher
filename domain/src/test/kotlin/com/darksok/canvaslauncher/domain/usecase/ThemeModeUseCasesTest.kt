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

class ThemeModeUseCasesTest {

    @Test
    fun `observe theme mode returns repository flow value`() = runTest {
        val repository = FakeThemePreferencesRepository(ThemeMode.DARK)
        val useCase = ObserveThemeModeUseCase(repository)

        val result = useCase().first()

        assertThat(result).isEqualTo(ThemeMode.DARK)
    }

    @Test
    fun `set theme mode updates repository`() = runTest {
        val repository = FakeThemePreferencesRepository(ThemeMode.SYSTEM)
        val useCase = SetThemeModeUseCase(repository)

        useCase(ThemeMode.LIGHT)

        assertThat(repository.observeThemeMode().first()).isEqualTo(ThemeMode.LIGHT)
    }

    private class FakeThemePreferencesRepository(
        initial: ThemeMode,
    ) : ThemePreferencesRepository {

        private val state = MutableStateFlow(initial)
        private val lightState = MutableStateFlow(LightThemePalette.SKY_BREEZE)
        private val darkState = MutableStateFlow(DarkThemePalette.MIDNIGHT_BLUE)

        override fun observeThemeMode(): Flow<ThemeMode> = state
        override fun observeLightThemePalette(): Flow<LightThemePalette> = lightState
        override fun observeDarkThemePalette(): Flow<DarkThemePalette> = darkState

        override suspend fun setThemeMode(themeMode: ThemeMode) {
            state.value = themeMode
        }

        override suspend fun setLightThemePalette(palette: LightThemePalette) {
            lightState.value = palette
        }

        override suspend fun setDarkThemePalette(palette: DarkThemePalette) {
            darkState.value = palette
        }
    }
}
