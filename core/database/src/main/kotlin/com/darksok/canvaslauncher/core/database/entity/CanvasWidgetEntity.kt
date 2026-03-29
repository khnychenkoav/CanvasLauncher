package com.darksok.canvaslauncher.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "canvas_widgets_table")
data class CanvasWidgetEntity(
    @PrimaryKey val id: String,
    val type: String,
    val centerX: Float,
    val centerY: Float,
    val widthWorld: Float,
    val heightWorld: Float,
    val colorArgb: Int,
)
