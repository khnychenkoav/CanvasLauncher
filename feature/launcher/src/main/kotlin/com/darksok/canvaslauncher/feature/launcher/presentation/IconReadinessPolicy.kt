package com.darksok.canvaslauncher.feature.launcher.presentation

import com.darksok.canvaslauncher.core.model.app.CanvasApp

object IconReadinessPolicy {

    fun areReady(
        apps: List<CanvasApp>,
        loadedIconPackages: Set<String>,
    ): Boolean {
        if (apps.isEmpty()) return true
        return apps.all { app -> loadedIconPackages.contains(app.packageName) }
    }
}
