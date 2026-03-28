package com.darksok.canvaslauncher.defaultscreen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.darksok.canvaslauncher.domain.usecase.ObserveDarkThemePaletteUseCase
import com.darksok.canvaslauncher.domain.usecase.ObserveLightThemePaletteUseCase
import com.darksok.canvaslauncher.domain.usecase.ObserveThemeModeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class DefaultViewModel @Inject constructor(
    observeThemeModeUseCase: ObserveThemeModeUseCase,
    observeLightThemePaletteUseCase: ObserveLightThemePaletteUseCase,
    observeDarkThemePaletteUseCase: ObserveDarkThemePaletteUseCase,
) : ViewModel() {

    val uiState = combine(
        observeThemeModeUseCase(),
        observeLightThemePaletteUseCase(),
        observeDarkThemePaletteUseCase(),
    ) { themeMode, lightPalette, darkPalette ->
        DefaultUiState(
            themeMode = themeMode,
            lightPalette = lightPalette,
            darkPalette = darkPalette,
        )
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = DefaultUiState(),
        )
}
