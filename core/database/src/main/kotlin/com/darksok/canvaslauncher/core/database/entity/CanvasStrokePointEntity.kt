package com.darksok.canvaslauncher.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "canvas_stroke_points_table",
    primaryKeys = ["strokeId", "pointIndex"],
    foreignKeys = [
        ForeignKey(
            entity = CanvasStrokeEntity::class,
            parentColumns = ["id"],
            childColumns = ["strokeId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("strokeId")],
)
data class CanvasStrokePointEntity(
    val strokeId: String,
    val pointIndex: Int,
    val x: Float,
    val y: Float,
)

