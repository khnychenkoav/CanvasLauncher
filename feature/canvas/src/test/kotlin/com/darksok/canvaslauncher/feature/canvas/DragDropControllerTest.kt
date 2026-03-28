package com.darksok.canvaslauncher.feature.canvas

import com.darksok.canvaslauncher.core.model.canvas.ScreenPoint
import com.darksok.canvaslauncher.core.model.canvas.WorldPoint
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DragDropControllerTest {

    @Test
    fun `end drag returns final state without clearing immediately`() {
        val controller = DefaultDragDropController()

        controller.startDrag(packageName = "pkg", worldStart = WorldPoint(10f, 20f))
        controller.dragBy(screenDelta = ScreenPoint(30f, -10f), scale = 2f)

        val endState = controller.endDrag()

        assertThat(endState?.worldPosition).isEqualTo(WorldPoint(25f, 15f))
        assertThat(controller.dragState.value).isNotNull()
    }

    @Test
    fun `finish drag clears state`() {
        val controller = DefaultDragDropController()

        controller.startDrag(packageName = "pkg", worldStart = WorldPoint(0f, 0f))
        controller.endDrag()
        controller.finishDrag()

        assertThat(controller.dragState.value).isNull()
    }

    @Test
    fun `cancel drag clears state`() {
        val controller = DefaultDragDropController()

        controller.startDrag(packageName = "pkg", worldStart = WorldPoint(1f, 1f))
        controller.cancelDrag()

        assertThat(controller.dragState.value).isNull()
    }
}
