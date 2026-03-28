package com.darksok.canvaslauncher.domain.usecase

import com.darksok.canvaslauncher.core.model.ui.LightThemePalette
import com.darksok.canvaslauncher.domain.repository.ThemePreferencesRepository
import javax.inject.Inject

class SetLightThemePaletteUseCase @Inject constructor(
    private val themePreferencesRepository: ThemePreferencesRepository,
) {
    suspend operator fun invoke(palette: LightThemePalette) {
        themePreferencesRepository.setLightThemePalette(palette)
    }
}
