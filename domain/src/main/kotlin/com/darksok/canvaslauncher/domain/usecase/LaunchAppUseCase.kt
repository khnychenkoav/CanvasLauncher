package com.darksok.canvaslauncher.domain.usecase

import com.darksok.canvaslauncher.core.common.result.AppResult
import com.darksok.canvaslauncher.domain.repository.AppLaunchService
import javax.inject.Inject

class LaunchAppUseCase @Inject constructor(
    private val appLaunchService: AppLaunchService,
) {
    suspend operator fun invoke(packageName: String): AppResult<Unit> {
        return appLaunchService.launch(packageName)
    }
}
