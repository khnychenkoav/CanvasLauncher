package com.darksok.canvaslauncher.core.database.dao

import androidx.room.Embedded
import androidx.room.Relation
import com.darksok.canvaslauncher.core.database.entity.CanvasStrokeEntity
import com.darksok.canvaslauncher.core.database.entity.CanvasStrokePointEntity

data class CanvasStrokeWithPointsEntity(
    @Embedded val stroke: CanvasStrokeEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "strokeId",
    )
    val points: List<CanvasStrokePointEntity>,
)

