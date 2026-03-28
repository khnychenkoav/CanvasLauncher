package com.darksok.canvaslauncher.domain.usecase

import com.darksok.canvaslauncher.core.model.ui.AppLayoutMode
import com.darksok.canvaslauncher.domain.repository.LayoutPreferencesRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveLayoutModeUseCase @Inject constructor(
    private val layoutPreferencesRepository: LayoutPreferencesRepository,
) {
    operator fun invoke(): Flow<AppLayoutMode> = layoutPreferencesRepository.observeLayoutMode()
}
