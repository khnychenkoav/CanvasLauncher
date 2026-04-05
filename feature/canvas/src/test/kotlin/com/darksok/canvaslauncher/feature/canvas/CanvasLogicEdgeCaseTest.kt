package com.darksok.canvaslauncher.feature.canvas

import androidx.compose.ui.graphics.Color
import com.darksok.canvaslauncher.core.model.canvas.CameraState
import com.darksok.canvaslauncher.core.model.canvas.ScreenPoint
import com.darksok.canvaslauncher.core.model.canvas.WorldPoint
import com.darksok.canvaslauncher.core.performance.WorldScreenTransformer
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.junit.Test

class CanvasLogicEdgeCaseTest {

    @Test
    fun `tap triggers when move equals touch slop`() {
        assertThat(AppTouchClassifier.shouldTriggerTap(1, 12f, 12f, false)).isTrue()
    }

    @Test
    fun `negative move distance still counts as within slop`() {
        assertThat(AppTouchClassifier.shouldTriggerTap(1, -1f, 12f, false)).isTrue()
    }

    @Test
    fun `zero slop allows stationary tap`() {
        assertThat(AppTouchClassifier.shouldTriggerTap(1, 0f, 0f, false)).isTrue()
    }

    @Test
    fun `zero slop rejects any movement`() {
        assertThat(AppTouchClassifier.shouldTriggerTap(1, 0.01f, 0f, false)).isFalse()
    }

    @Test
    fun `zero pointer count does not trigger tap`() {
        assertThat(AppTouchClassifier.shouldTriggerTap(0, 0f, 12f, false)).isFalse()
    }

    @Test
    fun `three pointers do not trigger tap`() {
        assertThat(AppTouchClassifier.shouldTriggerTap(3, 0f, 12f, false)).isFalse()
    }

    @Test
    fun `gesture handler no op does not set camera`() {
        val viewport = RecordingViewportController(CameraState())
        val handler = DefaultCanvasGestureHandler(viewport)

        handler.onTransform(ScreenPoint(0f, 0f), 1f, ScreenPoint(0f, 0f))

        assertThat(viewport.setCameraCalls).isEqualTo(0)
    }

    @Test
    fun `gesture handler pan only updates world center`() {
        val viewport = RecordingViewportController(CameraState())
        val handler = DefaultCanvasGestureHandler(viewport)

        handler.onTransform(ScreenPoint(30f, -20f), 1f, ScreenPoint(0f, 0f))

        assertThat(viewport.cameraState.value.worldCenter).isNotEqualTo(WorldPoint(0f, 0f))
    }

    @Test
    fun `gesture handler zoom only updates scale`() {
        val viewport = RecordingViewportController(CameraState(scale = 1f, viewportWidthPx = 1080, viewportHeightPx = 1920))
        val handler = DefaultCanvasGestureHandler(viewport)

        handler.onTransform(ScreenPoint(0f, 0f), 1.2f, ScreenPoint(540f, 960f))

        assertThat(viewport.cameraState.value.scale).isGreaterThan(1f)
    }

    @Test
    fun `gesture handler at min scale blocks tiny pan jitter`() {
        val viewport = RecordingViewportController(
            CameraState(scale = 0.3f, viewportWidthPx = 1080, viewportHeightPx = 1920),
        )
        val handler = DefaultCanvasGestureHandler(viewport)

        handler.onTransform(ScreenPoint(1f, 1f), 0.9f, ScreenPoint(540f, 960f))

        assertThat(viewport.cameraState.value.worldCenter).isEqualTo(WorldPoint(0f, 0f))
    }

    @Test
    fun `gesture handler at min scale allows large pan`() {
        val viewport = RecordingViewportController(
            CameraState(scale = 0.3f, viewportWidthPx = 1080, viewportHeightPx = 1920),
        )
        val handler = DefaultCanvasGestureHandler(viewport)

        handler.onTransform(ScreenPoint(16f, 0f), 0.9f, ScreenPoint(540f, 960f))

        assertThat(viewport.cameraState.value.worldCenter).isNotEqualTo(WorldPoint(0f, 0f))
    }

    @Test
    fun `arbiter does not suppress taps by default`() {
        val arbiter = CanvasInteractionArbiter()

        assertThat(arbiter.shouldSuppressTap(1L)).isFalse()
    }

    @Test
    fun `single pointer does not suppress taps`() {
        val arbiter = CanvasInteractionArbiter()
        arbiter.onPointerCountChanged(1, 100L)

        assertThat(arbiter.shouldSuppressTap(100L)).isFalse()
    }

    @Test
    fun `suppression window is inclusive at boundary`() {
        val arbiter = CanvasInteractionArbiter()
        arbiter.onCanvasTransformDetected(100L)

        assertThat(arbiter.shouldSuppressTap(320L)).isTrue()
    }

    @Test
    fun `later transform extends suppression window`() {
        val arbiter = CanvasInteractionArbiter()
        arbiter.onCanvasTransformDetected(100L)
        arbiter.onCanvasTransformDetected(200L)

        assertThat(arbiter.shouldSuppressTap(390L)).isTrue()
        assertThat(arbiter.shouldSuppressTap(421L)).isFalse()
    }

    @Test
    fun `drag by before start is ignored`() {
        val controller = DefaultDragDropController()

        controller.dragBy(ScreenPoint(100f, 50f), 2f)

        assertThat(controller.dragState.value).isNull()
    }

    @Test
    fun `start drag replaces previous drag package`() {
        val controller = DefaultDragDropController()
        controller.startDrag("one", WorldPoint(0f, 0f))

        controller.startDrag("two", WorldPoint(5f, 6f))

        assertThat(controller.dragState.value?.packageName).isEqualTo("two")
        assertThat(controller.dragState.value?.worldPosition).isEqualTo(WorldPoint(5f, 6f))
    }

    @Test
    fun `drag by with zero scale is ignored`() {
        val controller = DefaultDragDropController()
        controller.startDrag("pkg", WorldPoint(1f, 2f))

        controller.dragBy(ScreenPoint(50f, 50f), 0f)

        assertThat(controller.dragState.value?.worldPosition).isEqualTo(WorldPoint(1f, 2f))
    }

    @Test
    fun `set dragged position before start is ignored`() {
        val controller = DefaultDragDropController()
        controller.setDraggedPosition(WorldPoint(9f, 9f))

        assertThat(controller.dragState.value).isNull()
    }

    @Test
    fun `viewport width clamps to one when non positive`() {
        val controller = DefaultViewportController()

        controller.updateViewportSize(0, 100)

        assertThat(controller.cameraState.value.viewportWidthPx).isEqualTo(1)
    }

    @Test
    fun `viewport height clamps to one when non positive`() {
        val controller = DefaultViewportController()

        controller.updateViewportSize(100, -5)

        assertThat(controller.cameraState.value.viewportHeightPx).isEqualTo(1)
    }

    @Test
    fun `pan by changes world center`() {
        val controller = DefaultViewportController()
        controller.updateViewportSize(1080, 1920)

        controller.panBy(ScreenPoint(30f, 40f))

        assertThat(controller.cameraState.value.worldCenter).isNotEqualTo(WorldPoint(0f, 0f))
    }

    @Test
    fun `zoom by changes scale`() {
        val controller = DefaultViewportController()
        controller.updateViewportSize(1080, 1920)

        controller.zoomBy(1.4f, ScreenPoint(540f, 960f))

        assertThat(controller.cameraState.value.scale).isGreaterThan(1f)
    }

    @Test
    fun `screen to world and back round trips after camera set`() {
        val controller = DefaultViewportController()
        controller.setCamera(
            CameraState(
                worldCenter = WorldPoint(40f, -20f),
                scale = 1.5f,
                viewportWidthPx = 1080,
                viewportHeightPx = 1920,
            ),
        )

        val original = ScreenPoint(700f, 333f)
        val world = controller.screenToWorld(original)
        val roundTrip = controller.worldToScreen(world)

        assertThat(roundTrip.x).isWithin(0.001f).of(original.x)
        assertThat(roundTrip.y).isWithin(0.001f).of(original.y)
    }

    @Test
    fun `canvas background config exposes defaults`() {
        val config = CanvasBackgroundConfig(fillColor = Color.White, dotColor = Color.Black)

        assertThat(config.dotSpacingWorld).isEqualTo(120f)
        assertThat(config.dotRadiusPx).isEqualTo(1.5f)
    }

    @Test
    fun `canvas renderable app defaults to normal search visual state`() {
        val app = CanvasRenderableApp(
            packageName = "pkg",
            label = "Label",
            worldPosition = WorldPoint(0f, 0f),
            icon = null,
        )

        assertThat(app.searchVisualState).isEqualTo(CanvasSearchVisualState.Normal)
    }

    private class RecordingViewportController(initialCamera: CameraState) : ViewportController {
        private val camera = MutableStateFlow(initialCamera)
        override val cameraState: StateFlow<CameraState> = camera.asStateFlow()
        var setCameraCalls: Int = 0

        override fun updateViewportSize(widthPx: Int, heightPx: Int) = Unit

        override fun panBy(deltaPx: ScreenPoint) {
            camera.value = WorldScreenTransformer.applyPan(camera.value, deltaPx)
        }

        override fun zoomBy(zoomFactor: Float, focusPx: ScreenPoint) {
            camera.value = WorldScreenTransformer.applyZoom(camera.value, zoomFactor, focusPx)
        }

        override fun centerOn(worldPoint: WorldPoint, screenOffsetPx: ScreenPoint) = Unit

        override fun setCamera(cameraState: CameraState) {
            setCameraCalls += 1
            camera.value = cameraState
        }

        override fun screenToWorld(screenPoint: ScreenPoint): WorldPoint {
            return WorldScreenTransformer.screenToWorld(screenPoint, camera.value)
        }

        override fun worldToScreen(worldPoint: WorldPoint): ScreenPoint {
            return WorldScreenTransformer.worldToScreen(worldPoint, camera.value)
        }
    }
}
