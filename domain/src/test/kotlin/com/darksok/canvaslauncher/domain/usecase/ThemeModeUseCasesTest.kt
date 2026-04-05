package com.darksok.canvaslauncher.domain.usecase

import com.darksok.canvaslauncher.core.model.ui.ThemeMode
import com.darksok.canvaslauncher.domain.support.FakeThemePreferencesRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ThemeModeUseCasesTest {

    @Test
    fun `observe theme mode returns repository flow value`() = runTest {
        val repository = FakeThemePreferencesRepository(initialMode = ThemeMode.DARK)
        val useCase = ObserveThemeModeUseCase(repository)
        assertThat(useCase().first()).isEqualTo(ThemeMode.DARK)
    }

    @Test
    fun `set theme mode updates repository`() = runTest {
        val repository = FakeThemePreferencesRepository(initialMode = ThemeMode.SYSTEM)
        val useCase = SetThemeModeUseCase(repository)
        useCase(ThemeMode.LIGHT)
        assertThat(repository.observeThemeMode().first()).isEqualTo(ThemeMode.LIGHT)
    }

    @Test
    fun `set theme mode records call`() = runTest {
        val repository = FakeThemePreferencesRepository(initialMode = ThemeMode.SYSTEM)
        val useCase = SetThemeModeUseCase(repository)
        useCase(ThemeMode.DARK)
        assertThat(repository.setModeCalls).containsExactly(ThemeMode.DARK)
    }

    @Test
    fun `observe theme mode emits updated value after set`() = runTest {
        val repository = FakeThemePreferencesRepository(initialMode = ThemeMode.SYSTEM)
        SetThemeModeUseCase(repository)(ThemeMode.DARK)
        assertThat(ObserveThemeModeUseCase(repository)().first()).isEqualTo(ThemeMode.DARK)
    }
}
