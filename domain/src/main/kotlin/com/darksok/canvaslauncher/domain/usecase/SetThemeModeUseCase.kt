package com.darksok.canvaslauncher.domain.usecase

import com.darksok.canvaslauncher.core.model.ui.ThemeMode
import com.darksok.canvaslauncher.domain.repository.ThemePreferencesRepository
import javax.inject.Inject

class SetThemeModeUseCase @Inject constructor(
    private val themePreferencesRepository: ThemePreferencesRepository,
) {
    suspend operator fun invoke(themeMode: ThemeMode) {
        themePreferencesRepository.setThemeMode(themeMode)
    }
}
