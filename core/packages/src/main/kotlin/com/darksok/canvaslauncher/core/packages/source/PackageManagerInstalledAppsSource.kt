package com.darksok.canvaslauncher.core.packages.source

import android.content.Intent
import android.content.pm.PackageManager
import com.darksok.canvaslauncher.core.common.coroutines.DispatchersProvider
import com.darksok.canvaslauncher.core.model.app.InstalledApp
import com.darksok.canvaslauncher.domain.repository.InstalledAppsSource
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PackageManagerInstalledAppsSource @Inject constructor(
    private val packageManager: PackageManager,
    private val dispatchersProvider: DispatchersProvider,
) : InstalledAppsSource {

    override suspend fun getInstalledApps(): List<InstalledApp> = withContext(dispatchersProvider.io) {
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        packageManager.queryIntentActivities(launcherIntent, 0)
            .asSequence()
            .mapNotNull { resolveInfo ->
                val activityInfo = resolveInfo.activityInfo ?: return@mapNotNull null
                val pkg = activityInfo.packageName ?: return@mapNotNull null
                val label = resolveInfo.loadLabel(packageManager)?.toString()
                    ?.takeIf { it.isNotBlank() }
                    ?: pkg
                InstalledApp(
                    packageName = pkg,
                    label = label,
                )
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
            .toList()
    }

    override suspend fun getInstalledApp(packageName: String): InstalledApp? = withContext(dispatchersProvider.io) {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName) ?: return@withContext null
        val resolveInfo = packageManager.resolveActivity(launchIntent, 0) ?: return@withContext null
        val label = resolveInfo.loadLabel(packageManager)?.toString()?.takeIf { it.isNotBlank() } ?: packageName
        InstalledApp(packageName = packageName, label = label)
    }
}
