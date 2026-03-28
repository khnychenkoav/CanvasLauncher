package com.darksok.canvaslauncher.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "canvas_frame_objects_table")
data class CanvasFrameObjectEntity(
    @PrimaryKey val id: String,
    val title: String,
    val centerX: Float,
    val centerY: Float,
    val widthWorld: Float,
    val heightWorld: Float,
    val colorArgb: Int,
)

