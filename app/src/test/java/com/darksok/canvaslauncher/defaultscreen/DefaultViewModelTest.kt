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
    fun `ui state reflects stored theme mode`() = runTestWithMain {
        val repository = FakeThemePreferencesRepository(initial = ThemeMode.DARK)

        val viewModel = createViewModel(repository)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.themeMode).isEqualTo(ThemeMode.DARK)
    }

    @Test
    fun `ui state reflects stored light palette`() = runTestWithMain {
        val repository = FakeThemePreferencesRepository(initial = ThemeMode.SYSTEM)
        repository.lightState.value = LightThemePalette.ROSE_DAWN

        val viewModel = createViewModel(repository)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.lightPalette).isEqualTo(LightThemePalette.ROSE_DAWN)
    }

    @Test
    fun `ui state reflects stored dark palette`() = runTestWithMain {
        val repository = FakeThemePreferencesRepository(initial = ThemeMode.SYSTEM)
        repository.darkState.value = DarkThemePalette.CHARCOAL_AMBER

        val viewModel = createViewModel(repository)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.darkPalette).isEqualTo(DarkThemePalette.CHARCOAL_AMBER)
    }

    @Test
    fun `theme updates propagate after creation`() = runTestWithMain {
        val repository = FakeThemePreferencesRepository(initial = ThemeMode.SYSTEM)
        val viewModel = createViewModel(repository)

        repository.state.value = ThemeMode.LIGHT
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.themeMode).isEqualTo(ThemeMode.LIGHT)
    }

    @Test
    fun `light palette updates propagate after creation`() = runTestWithMain {
        val repository = FakeThemePreferencesRepository(initial = ThemeMode.SYSTEM)
        val viewModel = createViewModel(repository)

        repository.lightState.value = LightThemePalette.MINT_GARDEN
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.lightPalette).isEqualTo(LightThemePalette.MINT_GARDEN)
    }

    @Test
    fun `dark palette updates propagate after creation`() = runTestWithMain {
        val repository = FakeThemePreferencesRepository(initial = ThemeMode.SYSTEM)
        val viewModel = createViewModel(repository)

        repository.darkState.value = DarkThemePalette.FOREST_NIGHT
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.darkPalette).isEqualTo(DarkThemePalette.FOREST_NIGHT)
    }

    @Test
    fun `combined updates keep latest values from all flows`() = runTestWithMain {
        val repository = FakeThemePreferencesRepository(initial = ThemeMode.SYSTEM)
        val viewModel = createViewModel(repository)

        repository.state.value = ThemeMode.DARK
        repository.lightState.value = LightThemePalette.SUNSET_GLOW
        repository.darkState.value = DarkThemePalette.DEEP_OCEAN
        advanceUntilIdle()

        assertThat(viewModel.uiState.value).isEqualTo(
            DefaultUiState(
                themeMode = ThemeMode.DARK,
                lightPalette = LightThemePalette.SUNSET_GLOW,
                darkPalette = DarkThemePalette.DEEP_OCEAN,
            ),
        )
    }

    @Test
    fun `repeating same values keeps ui state stable`() = runTestWithMain {
        val repository = FakeThemePreferencesRepository(initial = ThemeMode.SYSTEM)
        val viewModel = createViewModel(repository)
        advanceUntilIdle()
        val before = viewModel.uiState.value

        repository.state.value = ThemeMode.SYSTEM
        repository.lightState.value = LightThemePalette.SKY_BREEZE
        repository.darkState.value = DarkThemePalette.MIDNIGHT_BLUE
        advanceUntilIdle()

        assertThat(viewModel.uiState.value).isEqualTo(before)
    }

    private fun createViewModel(repository: FakeThemePreferencesRepository): DefaultViewModel {
        return DefaultViewModel(
            observeThemeModeUseCase = ObserveThemeModeUseCase(repository),
            observeLightThemePaletteUseCase = ObserveLightThemePaletteUseCase(repository),
            observeDarkThemePaletteUseCase = ObserveDarkThemePaletteUseCase(repository),
        )
    }

    private fun runTestWithMain(block: suspend kotlinx.coroutines.test.TestScope.() -> Unit) = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            block()
        } finally {
            Dispatchers.resetMain()
        }
    }

    private class FakeThemePreferencesRepository(
        initial: ThemeMode,
    ) : ThemePreferencesRepository {

        val state = MutableStateFlow(initial)
        val lightState = MutableStateFlow(LightThemePalette.SKY_BREEZE)
        val darkState = MutableStateFlow(DarkThemePalette.MIDNIGHT_BLUE)

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
