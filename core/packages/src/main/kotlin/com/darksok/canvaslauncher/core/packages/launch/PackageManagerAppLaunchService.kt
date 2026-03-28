package com.darksok.canvaslauncher.core.packages.launch

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.darksok.canvaslauncher.core.common.result.AppError
import com.darksok.canvaslauncher.core.common.result.AppResult
import com.darksok.canvaslauncher.domain.repository.AppLaunchService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PackageManagerAppLaunchService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val packageManager: PackageManager,
) : AppLaunchService {

    override suspend fun launch(packageName: String): AppResult<Unit> {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
            ?: return AppResult.Failure(AppError.LaunchUnavailable)

        return try {
            context.startActivity(
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
            AppResult.Success(Unit)
        } catch (_: ActivityNotFoundException) {
            AppResult.Failure(AppError.LaunchUnavailable)
        } catch (_: SecurityException) {
            AppResult.Failure(AppError.LaunchUnavailable)
        } catch (throwable: Throwable) {
            AppResult.Failure(AppError.Unknown(throwable.message))
        }
    }
}
