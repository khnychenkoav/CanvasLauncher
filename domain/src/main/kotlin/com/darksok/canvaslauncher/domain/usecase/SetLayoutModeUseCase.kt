package com.darksok.canvaslauncher.domain.usecase

import com.darksok.canvaslauncher.core.model.ui.AppLayoutMode
import com.darksok.canvaslauncher.domain.repository.LayoutPreferencesRepository
import javax.inject.Inject

class SetLayoutModeUseCase @Inject constructor(
    private val layoutPreferencesRepository: LayoutPreferencesRepository,
) {
    suspend operator fun invoke(layoutMode: AppLayoutMode) {
        layoutPreferencesRepository.setLayoutMode(layoutMode)
    }
}
