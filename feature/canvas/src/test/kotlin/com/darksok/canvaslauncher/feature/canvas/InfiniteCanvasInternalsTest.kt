package com.darksok.canvaslauncher.feature.canvas

import androidx.compose.ui.geometry.Offset
import com.darksok.canvaslauncher.core.model.canvas.ScreenPoint
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class InfiniteCanvasInternalsTest {

    @Test
    fun `edge auto pan returns null for invalid canvas values`() {
        val result = edgeAutoPanDelta(
            pointer = Offset(10f, 10f),
            width = 0f,
            height = 100f,
            zone = 72f,
            maxStep = 16f,
        )

        assertThat(result).isNull()
    }

    @Test
    fun `edge auto pan returns directional deltas near each border`() {
        val leftTop = edgeAutoPanDelta(
            pointer = Offset(2f, 3f),
            width = 500f,
            height = 900f,
            zone = 72f,
            maxStep = 16f,
        )
        assertThat(leftTop).isNotNull()
        assertThat(leftTop!!.x).isLessThan(0f)
        assertThat(leftTop.y).isLessThan(0f)

        val rightBottom = edgeAutoPanDelta(
            pointer = Offset(498f, 899f),
            width = 500f,
            height = 900f,
            zone = 72f,
            maxStep = 16f,
        )
        assertThat(rightBottom).isNotNull()
        assertThat(rightBottom!!.x).isGreaterThan(0f)
        assertThat(rightBottom.y).isGreaterThan(0f)

        val center = edgeAutoPanDelta(
            pointer = Offset(250f, 450f),
            width = 500f,
            height = 900f,
            zone = 72f,
            maxStep = 16f,
        )
        assertThat(center).isNull()
    }

    @Test
    fun `dot point buffer reuses capacity and grows geometrically`() {
        val dotBufferClass = Class.forName("com.darksok.canvaslauncher.feature.canvas.DotPointBuffer")
        val ctor = dotBufferClass.getDeclaredConstructor().apply { isAccessible = true }
        val buffer = ctor.newInstance()
        val obtain = dotBufferClass.getDeclaredMethod("obtain", Int::class.javaPrimitiveType).apply {
            isAccessible = true
        }

        val initial = obtain.invoke(buffer, 16) as FloatArray
        val reused = obtain.invoke(buffer, 8) as FloatArray
        val grown = obtain.invoke(buffer, initial.size + 1) as FloatArray

        assertThat(reused).isSameInstanceAs(initial)
        assertThat(grown.size).isGreaterThan(initial.size)
    }

    private fun edgeAutoPanDelta(
        pointer: Offset,
        width: Float,
        height: Float,
        zone: Float,
        maxStep: Float,
    ): ScreenPoint? {
        val method = infiniteCanvasKtClass.declaredMethods.first { candidate ->
            candidate.name.startsWith("access\$edgeAutoPanDelta")
        }
        method.isAccessible = true
        return method.invoke(null, packOffset(pointer), width, height, zone, maxStep) as ScreenPoint?
    }

    private fun packOffset(offset: Offset): Long {
        val method = Offset::class.java.getDeclaredMethod("unbox-impl")
        method.isAccessible = true
        return method.invoke(offset) as Long
    }

    private companion object {
        private val infiniteCanvasKtClass: Class<*> =
            Class.forName("com.darksok.canvaslauncher.feature.canvas.InfiniteCanvasKt")
    }
}
