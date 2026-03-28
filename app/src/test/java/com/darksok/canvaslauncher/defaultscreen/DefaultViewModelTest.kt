package com.darksok.canvaslauncher.defaultscreen

import com.darksok.canvaslauncher.core.model.ui.DarkThemePalette
import com.darksok.canvaslauncher.core.model.ui.LightThemePalette
import com.darksok.canvaslauncher.core.model.ui.ThemeMode
import com.darksok.canvaslauncher.domain.repository.ThemePreferencesRepository
import com.darksok.canvaslauncher.domain.usecase.ObserveDarkThemePaletteUseCase
import com.darksok.canvaslauncher.domain.usecase.ObserveLightThemePaletteUseCase
import com.darksok.canvaslauncher.domain.usecase.ObserveThemeModeUseCase
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultViewModelTest {

    @Test
    fun `ui state reflects stored theme mode`() = runTest {
        val repository = FakeThemePreferencesRepository(initial = ThemeMode.DARK)
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val viewModel = DefaultViewModel(
                observeThemeModeUseCase = ObserveThemeModeUseCase(repository),
                observeLightThemePaletteUseCase = ObserveLightThemePaletteUseCase(repository),
                observeDarkThemePaletteUseCase = ObserveDarkThemePaletteUseCase(repository),
            )
            advanceUntilIdle()
            assertThat(viewModel.uiState.value.themeMode).isEqualTo(ThemeMode.DARK)
            assertThat(viewModel.uiState.value.lightPalette).isEqualTo(LightThemePalette.SKY_BREEZE)
            assertThat(viewModel.uiState.value.darkPalette).isEqualTo(DarkThemePalette.MIDNIGHT_BLUE)
        } finally {
            Dispatchers.resetMain()
        }
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
