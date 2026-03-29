package com.darksok.canvaslauncher.feature.launcher.presentation

import com.darksok.canvaslauncher.core.model.canvas.WorldPoint

enum class CanvasEditToolId {
    Move,
    Brush,
    Selection,
    StickyNote,
    Text,
    Frame,
    Delete,
}

enum class CanvasSnapOrientation {
    Vertical,
    Horizontal,
}

data class CanvasSnapGuideUiState(
    val orientation: CanvasSnapOrientation,
    val worldCoordinate: Float,
)

data class CanvasStrokeUiState(
    val id: String,
    val points: List<WorldPoint>,
    val colorArgb: Int,
    val widthWorld: Float,
)

data class CanvasStickyNoteUiState(
    val id: String,
    val text: String,
    val center: WorldPoint,
    val sizeWorld: Float,
    val textSizeWorld: Float,
    val colorArgb: Int,
)

data class CanvasTextObjectUiState(
    val id: String,
    val text: String,
    val position: WorldPoint,
    val textSizeWorld: Float,
    val colorArgb: Int,
)

data class CanvasFrameObjectUiState(
    val id: String,
    val title: String,
    val center: WorldPoint,
    val widthWorld: Float,
    val heightWorld: Float,
    val colorArgb: Int,
)

data class CanvasFrameDraftUiState(
    val startCorner: WorldPoint,
    val endCorner: WorldPoint,
    val colorArgb: Int,
)

data class CanvasSelectionDraftUiState(
    val startCorner: WorldPoint,
    val endCorner: WorldPoint,
)

data class CanvasSelectionUiState(
    val packageNames: Set<String> = emptySet(),
    val frameIds: Set<String> = emptySet(),
    val textIds: Set<String> = emptySet(),
    val strokeIds: Set<String> = emptySet(),
) {
    val hasIcons: Boolean
        get() = packageNames.isNotEmpty()

    val isEmpty: Boolean
        get() = packageNames.isEmpty() && frameIds.isEmpty() && textIds.isEmpty() && strokeIds.isEmpty()
}

data class CanvasSelectionBoundsUiState(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
    val hasIcons: Boolean,
    val canResizeAndDelete: Boolean,
) {
    fun contains(point: WorldPoint): Boolean {
        return point.x in left..right && point.y in top..bottom
    }
}

enum class CanvasFrameResizeHandle {
    Left,
    TopLeft,
    Top,
    TopRight,
    Right,
    BottomRight,
    Bottom,
    BottomLeft,
}

sealed interface CanvasInlineEditorTarget {
    data object None : CanvasInlineEditorTarget
    data class NewSticky(val worldPoint: WorldPoint) : CanvasInlineEditorTarget
    data class EditSticky(val id: String) : CanvasInlineEditorTarget
    data class NewText(val worldPoint: WorldPoint) : CanvasInlineEditorTarget
    data class EditText(val id: String) : CanvasInlineEditorTarget
    data class NewFrame(val worldPoint: WorldPoint) : CanvasInlineEditorTarget
    data class EditFrame(val id: String) : CanvasInlineEditorTarget
}

data class CanvasInlineEditorUiState(
    val isVisible: Boolean = false,
    val title: String = "",
    val placeholder: String = "",
    val value: String = "",
    val initialValue: String = "",
    val isDraft: Boolean = false,
    val target: CanvasInlineEditorTarget = CanvasInlineEditorTarget.None,
)

data class EditUiState(
    val selectedTool: CanvasEditToolId = CanvasEditToolId.Move,
    val selectedColorArgb: Int = CanvasEditDefaults.DEFAULT_COLOR,
    val brushWidthWorld: Float = CanvasEditDefaults.DEFAULT_BRUSH_WIDTH_WORLD,
    val textSizeWorld: Float = CanvasEditDefaults.DEFAULT_TEXT_SIZE_WORLD,
    val inlineEditor: CanvasInlineEditorUiState = CanvasInlineEditorUiState(),
)

object CanvasEditDefaults {
    const val DEFAULT_COLOR: Int = 0xFF2E7D32.toInt()
    const val DEFAULT_BRUSH_WIDTH_WORLD: Float = 18f
    const val MIN_BRUSH_WIDTH_WORLD: Float = 6f
    const val MAX_BRUSH_WIDTH_WORLD: Float = 46f
    const val DEFAULT_TEXT_SIZE_WORLD: Float = 44f
    const val MIN_TEXT_SIZE_WORLD: Float = 24f
    const val MAX_TEXT_SIZE_WORLD: Float = 96f
    const val DEFAULT_STICKY_SIZE_WORLD: Float = 220f
    const val DEFAULT_FRAME_WIDTH_WORLD: Float = 540f
    const val DEFAULT_FRAME_HEIGHT_WORLD: Float = 360f
    const val STICKY_MIN_SIZE_WORLD: Float = 150f
    const val STICKY_MAX_SIZE_WORLD: Float = 320f
    val PALETTE: List<Int> = listOf(
        0xFF2E7D32.toInt(),
        0xFFE65100.toInt(),
        0xFF1565C0.toInt(),
        0xFF6A1B9A.toInt(),
        0xFF37474F.toInt(),
        0xFFF9A825.toInt(),
    )
}
