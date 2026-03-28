package com.darksok.canvaslauncher.domain.usecase

import com.darksok.canvaslauncher.core.model.ui.DarkThemePalette
import com.darksok.canvaslauncher.domain.repository.ThemePreferencesRepository
import javax.inject.Inject

class SetDarkThemePaletteUseCase @Inject constructor(
    private val themePreferencesRepository: ThemePreferencesRepository,
) {
    suspend operator fun invoke(palette: DarkThemePalette) {
        themePreferencesRepository.setDarkThemePalette(palette)
    }
}
