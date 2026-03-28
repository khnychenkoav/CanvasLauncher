package com.darksok.canvaslauncher.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "canvas_strokes_table")
data class CanvasStrokeEntity(
    @PrimaryKey val id: String,
    val colorArgb: Int,
    val widthWorld: Float,
)

