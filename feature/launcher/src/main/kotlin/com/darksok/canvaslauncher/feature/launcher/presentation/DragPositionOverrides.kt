package com.darksok.canvaslauncher.feature.launcher.presentation

import com.darksok.canvaslauncher.core.model.app.CanvasApp
import com.darksok.canvaslauncher.core.model.canvas.WorldPoint
import kotlin.math.abs

internal object DragPositionOverrides {

    fun apply(
        apps: List<CanvasApp>,
        overrides: Map<String, WorldPoint>,
    ): List<CanvasApp> {
        if (overrides.isEmpty()) return apps
        return apps.map { app ->
            val overridden = overrides[app.packageName] ?: return@map app
            app.copy(position = overridden)
        }
    }

    fun pruneCommitted(
        overrides: Map<String, WorldPoint>,
        persistedApps: List<CanvasApp>,
    ): Map<String, WorldPoint> {
        if (overrides.isEmpty()) return overrides
        val persistedByPackage = persistedApps.associateBy { it.packageName }
        return overrides.filterValuesWithKey { packageName, overridePosition ->
            val persisted = persistedByPackage[packageName] ?: return@filterValuesWithKey false
            !persisted.position.isApproximatelyEqual(overridePosition)
        }
    }

    private fun <K, V> Map<K, V>.filterValuesWithKey(
        predicate: (K, V) -> Boolean,
    ): Map<K, V> {
        return entries
            .filter { (key, value) -> predicate(key, value) }
            .associate { it.toPair() }
    }

    private fun WorldPoint.isApproximatelyEqual(other: WorldPoint): Boolean {
        return abs(x - other.x) <= POSITION_EPSILON && abs(y - other.y) <= POSITION_EPSILON
    }

    private const val POSITION_EPSILON = 0.01f
}
