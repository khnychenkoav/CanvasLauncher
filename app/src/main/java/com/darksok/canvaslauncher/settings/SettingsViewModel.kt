package com.darksok.canvaslauncher.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.darksok.canvaslauncher.core.common.coroutines.DispatchersProvider
import com.darksok.canvaslauncher.core.model.ui.AppLayoutMode
import com.darksok.canvaslauncher.core.model.ui.DarkThemePalette
import com.darksok.canvaslauncher.core.model.ui.LightThemePalette
import com.darksok.canvaslauncher.core.model.ui.ThemeMode
import com.darksok.canvaslauncher.domain.usecase.ObserveDarkThemePaletteUseCase
import com.darksok.canvaslauncher.domain.usecase.ObserveLayoutModeUseCase
import com.darksok.canvaslauncher.domain.usecase.ObserveLightThemePaletteUseCase
import com.darksok.canvaslauncher.domain.usecase.RearrangeAppsUseCase
import com.darksok.canvaslauncher.domain.usecase.ObserveThemeModeUseCase
import com.darksok.canvaslauncher.domain.usecase.SetDarkThemePaletteUseCase
import com.darksok.canvaslauncher.domain.usecase.SetLayoutModeUseCase
import com.darksok.canvaslauncher.domain.usecase.SetLightThemePaletteUseCase
import com.darksok.canvaslauncher.domain.usecase.SetThemeModeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    observeThemeModeUseCase: ObserveThemeModeUseCase,
    observeLightThemePaletteUseCase: ObserveLightThemePaletteUseCase,
    observeDarkThemePaletteUseCase: ObserveDarkThemePaletteUseCase,
    observeLayoutModeUseCase: ObserveLayoutModeUseCase,
    private val setThemeModeUseCase: SetThemeModeUseCase,
    private val setLightThemePaletteUseCase: SetLightThemePaletteUseCase,
    private val setDarkThemePaletteUseCase: SetDarkThemePaletteUseCase,
    private val setLayoutModeUseCase: SetLayoutModeUseCase,
    private val rearrangeAppsUseCase: RearrangeAppsUseCase,
    private val dispatchersProvider: DispatchersProvider,
) : ViewModel() {

    val uiState = combine(
        observeThemeModeUseCase(),
        observeLightThemePaletteUseCase(),
        observeDarkThemePaletteUseCase(),
        observeLayoutModeUseCase(),
    ) { themeMode, lightPalette, darkPalette, layoutMode ->
        SettingsUiState(
            themeMode = themeMode,
            lightPalette = lightPalette,
            darkPalette = darkPalette,
            layoutMode = layoutMode,
        )
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SettingsUiState(),
        )

    fun onThemeModeSelected(themeMode: ThemeMode) {
        viewModelScope.launch(dispatchersProvider.io) {
            setThemeModeUseCase(themeMode)
        }
    }

    fun onLightPaletteSelected(palette: LightThemePalette) {
        viewModelScope.launch(dispatchersProvider.io) {
            setLightThemePaletteUseCase(palette)
        }
    }

    fun onDarkPaletteSelected(palette: DarkThemePalette) {
        viewModelScope.launch(dispatchersProvider.io) {
            setDarkThemePaletteUseCase(palette)
        }
    }

    fun onLayoutModeSelected(layoutMode: AppLayoutMode) {
        viewModelScope.launch(dispatchersProvider.io) {
            setLayoutModeUseCase(layoutMode)
            rearrangeAppsUseCase(layoutMode)
        }
    }
}
