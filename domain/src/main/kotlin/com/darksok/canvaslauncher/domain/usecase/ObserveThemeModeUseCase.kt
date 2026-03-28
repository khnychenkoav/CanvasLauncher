package com.darksok.canvaslauncher.domain.usecase

import com.darksok.canvaslauncher.core.model.ui.ThemeMode
import com.darksok.canvaslauncher.domain.repository.ThemePreferencesRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveThemeModeUseCase @Inject constructor(
    private val themePreferencesRepository: ThemePreferencesRepository,
) {
    operator fun invoke(): Flow<ThemeMode> = themePreferencesRepository.observeThemeMode()
}
