package com.darksok.canvaslauncher.feature.apps.data

import com.darksok.canvaslauncher.core.database.entity.AppEntity
import com.darksok.canvaslauncher.core.model.app.CanvasApp
import com.darksok.canvaslauncher.core.model.canvas.WorldPoint

internal fun AppEntity.toDomain(): CanvasApp {
    return CanvasApp(
        packageName = packageName,
        label = label,
        position = WorldPoint(x, y),
    )
}

internal fun CanvasApp.toEntity(): AppEntity {
    return AppEntity(
        packageName = packageName,
        label = label,
        x = position.x,
        y = position.y,
    )
}
