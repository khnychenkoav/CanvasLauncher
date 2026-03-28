package com.darksok.canvaslauncher.domain.repository

import com.darksok.canvaslauncher.core.model.ui.AppLayoutMode
import kotlinx.coroutines.flow.Flow

interface LayoutPreferencesRepository {
    fun observeLayoutMode(): Flow<AppLayoutMode>
    suspend fun setLayoutMode(layoutMode: AppLayoutMode)
}
