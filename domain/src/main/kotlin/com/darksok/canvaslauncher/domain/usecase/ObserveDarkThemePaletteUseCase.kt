package com.darksok.canvaslauncher.domain.usecase

import com.darksok.canvaslauncher.core.model.ui.DarkThemePalette
import com.darksok.canvaslauncher.domain.repository.ThemePreferencesRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveDarkThemePaletteUseCase @Inject constructor(
    private val themePreferencesRepository: ThemePreferencesRepository,
) {
    operator fun invoke(): Flow<DarkThemePalette> = themePreferencesRepository.observeDarkThemePalette()
}
