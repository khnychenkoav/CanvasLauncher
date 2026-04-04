package com.darksok.canvaslauncher.feature.canvas

import com.darksok.canvaslauncher.core.model.canvas.CameraState
import com.darksok.canvaslauncher.core.model.canvas.ScreenPoint
import com.darksok.canvaslauncher.core.model.canvas.WorldPoint
import com.darksok.canvaslauncher.core.performance.WorldScreenTransformer
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.junit.Test

class CanvasGestureHandlerTest {

    @Test
    fun `pinch at max scale ignores pan jitter when zoom is clamped`() {
        val viewport = FakeViewportController(
            initialCamera = CameraState(
                worldCenter = WorldPoint(0f, 0f),
                scale = 2.0f,
                viewportWidthPx = 1080,
                viewportHeightPx = 1920,
            ),
        )
        val handler = DefaultCanvasGestureHandler(viewport)

        handler.onTransform(
            panDeltaPx = ScreenPoint(2f, 0f),
            zoomFactor = 1.03f,
            focusPx = ScreenPoint(540f, 960f),
        )

        assertThat(viewport.cameraState.value.worldCenter).isEqualTo(WorldPoint(0f, 0f))
    }

    @Test
    fun `pinch below max scale still applies pan`() {
        val viewport = FakeViewportController(
            initialCamera = CameraState(
                worldCenter = WorldPoint(0f, 0f),
                scale = 1.8f,
                viewportWidthPx = 1080,
                viewportHeightPx = 1920,
            ),
        )
        val handler = DefaultCanvasGestureHandler(viewport)

        handler.onTransform(
            panDeltaPx = ScreenPoint(18f, 0f),
            zoomFactor = 1.03f,
            focusPx = ScreenPoint(540f, 960f),
        )

        assertThat(viewport.cameraState.value.worldCenter).isNotEqualTo(WorldPoint(0f, 0f))
    }

    @Test
    fun `pinch at max scale still allows meaningful pan`() {
        val viewport = FakeViewportController(
            initialCamera = CameraState(
                worldCenter = WorldPoint(0f, 0f),
                scale = 2.0f,
                viewportWidthPx = 1080,
                viewportHeightPx = 1920,
            ),
        )
        val handler = DefaultCanvasGestureHandler(viewport)

        handler.onTransform(
            panDeltaPx = ScreenPoint(18f, 0f),
            zoomFactor = 1.03f,
            focusPx = ScreenPoint(540f, 960f),
        )

        assertThat(viewport.cameraState.value.worldCenter).isNotEqualTo(WorldPoint(0f, 0f))
    }

    private class FakeViewportController(
        initialCamera: CameraState,
    ) : ViewportController {
        private val camera = MutableStateFlow(initialCamera)
        override val cameraState: StateFlow<CameraState> = camera.asStateFlow()

        override fun updateViewportSize(widthPx: Int, heightPx: Int) = Unit

        override fun panBy(deltaPx: ScreenPoint) {
            camera.value = WorldScreenTransformer.applyPan(camera.value, deltaPx)
        }

        override fun zoomBy(zoomFactor: Float, focusPx: ScreenPoint) {
            camera.value = WorldScreenTransformer.applyZoom(camera.value, zoomFactor, focusPx)
        }

        override fun centerOn(worldPoint: WorldPoint, screenOffsetPx: ScreenPoint) = Unit

        override fun setCamera(cameraState: CameraState) {
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
