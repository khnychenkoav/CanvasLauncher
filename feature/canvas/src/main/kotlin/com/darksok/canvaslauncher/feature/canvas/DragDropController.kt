package com.darksok.canvaslauncher.feature.canvas

import com.darksok.canvaslauncher.core.model.canvas.ScreenPoint
import com.darksok.canvaslauncher.core.model.canvas.WorldPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

interface DragDropController {
    val dragState: StateFlow<DragState?>
    fun startDrag(packageName: String, worldStart: WorldPoint)
    fun dragBy(screenDelta: ScreenPoint, scale: Float)
    fun setDraggedPosition(worldPosition: WorldPoint)
    fun endDrag(): DragState?
    fun finishDrag()
    fun cancelDrag()
}

class DefaultDragDropController @Inject constructor() : DragDropController {

    private val state = MutableStateFlow<DragState?>(null)
    override val dragState: StateFlow<DragState?> = state.asStateFlow()

    override fun startDrag(packageName: String, worldStart: WorldPoint) {
        state.value = DragState(packageName = packageName, worldPosition = worldStart)
    }

    override fun dragBy(screenDelta: ScreenPoint, scale: Float) {
        val current = state.value ?: return
        if (scale == 0f) return
        state.value = current.copy(
            worldPosition = WorldPoint(
                x = current.worldPosition.x + (screenDelta.x / scale),
                y = current.worldPosition.y + (screenDelta.y / scale),
            ),
        )
    }

    override fun setDraggedPosition(worldPosition: WorldPoint) {
        val current = state.value ?: return
        state.value = current.copy(worldPosition = worldPosition)
    }

    override fun endDrag(): DragState? {
        return state.value
    }

    override fun finishDrag() {
        state.value = null
    }

    override fun cancelDrag() {
        state.value = null
    }
}
