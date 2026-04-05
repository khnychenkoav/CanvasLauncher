package com.darksok.canvaslauncher.feature.launcher.presentation

import com.darksok.canvaslauncher.core.database.dao.CanvasStrokeWithPointsEntity
import com.darksok.canvaslauncher.core.database.entity.CanvasStrokeEntity
import com.darksok.canvaslauncher.core.database.entity.CanvasStrokePointEntity
import com.darksok.canvaslauncher.core.model.app.CanvasApp
import com.darksok.canvaslauncher.core.model.canvas.WorldPoint
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class LauncherViewModelInternalsTest {

    @Test
    fun `world bounds methods handle contains intersects offset and mapping`() {
        val source = worldBounds(0f, 0f, 10f, 20f)
        val inside = WorldPoint(2f, 4f)
        val outside = WorldPoint(22f, 4f)
        val target = worldBounds(-20f, -10f, 20f, 30f)

        assertThat(callBoolean(source, "contains", WorldPoint::class.java, inside)).isTrue()
        assertThat(callBoolean(source, "contains", WorldPoint::class.java, outside)).isFalse()
        assertThat(callBoolean(source, "containsRect", worldBoundsClass, worldBounds(1f, 1f, 8f, 12f))).isTrue()
        assertThat(callBoolean(source, "containsRect", worldBoundsClass, worldBounds(-1f, 1f, 8f, 12f))).isFalse()
        assertThat(callBoolean(source, "intersects", worldBoundsClass, worldBounds(8f, 19f, 16f, 28f))).isTrue()
        assertThat(callBoolean(source, "intersects", worldBoundsClass, worldBounds(30f, 30f, 40f, 40f))).isFalse()

        val offset = callObj(source, "offset", arrayOf(Float::class.javaPrimitiveType!!, Float::class.javaPrimitiveType!!), 5f, -3f)
        assertThat(getFloat(offset, "getLeft")).isEqualTo(5f)
        assertThat(getFloat(offset, "getTop")).isEqualTo(-3f)

        val mappedPoint = callObj(source, "mapPointTo", arrayOf(worldBoundsClass, WorldPoint::class.java), target, WorldPoint(5f, 10f)) as WorldPoint
        assertThat(mappedPoint.x).isWithin(0.0001f).of(0f)
        assertThat(mappedPoint.y).isWithin(0.0001f).of(10f)

        val mappedX = getFloatFromAny(callObj(source, "mapXTo", arrayOf(worldBoundsClass, Float::class.javaPrimitiveType!!), target, 2.5f))
        val mappedY = getFloatFromAny(callObj(source, "mapYTo", arrayOf(worldBoundsClass, Float::class.javaPrimitiveType!!), target, 5f))
        assertThat(mappedX).isWithin(0.0001f).of(-10f)
        assertThat(mappedY).isWithin(0.0001f).of(0f)
    }

    @Test
    fun `resize helpers enforce minimum dimensions for all handles`() {
        val startBounds = worldBounds(-100f, -80f, 100f, 80f)
        CanvasFrameResizeHandle.entries.forEach { handle ->
            val resizedBounds = callObj(
                startBounds,
                "resizeByHandle",
                arrayOf(
                    CanvasFrameResizeHandle::class.java,
                    Float::class.javaPrimitiveType!!,
                    Float::class.javaPrimitiveType!!,
                    Float::class.javaPrimitiveType!!,
                    Float::class.javaPrimitiveType!!,
                ),
                handle,
                -500f,
                -500f,
                90f,
                72f,
            )
            assertThat(getFloat(resizedBounds, "getWidth")).isAtLeast(90f)
            assertThat(getFloat(resizedBounds, "getHeight")).isAtLeast(72f)

            val resizedRect = callStatic(
                "resizeWorldRectByHandleDrag",
                arrayOf(
                    WorldPoint::class.java,
                    Float::class.javaPrimitiveType!!,
                    Float::class.javaPrimitiveType!!,
                    CanvasFrameResizeHandle::class.java,
                    Float::class.javaPrimitiveType!!,
                    Float::class.javaPrimitiveType!!,
                    Float::class.javaPrimitiveType!!,
                    Float::class.javaPrimitiveType!!,
                ),
                WorldPoint(0f, 0f),
                200f,
                140f,
                handle,
                -600f,
                520f,
                120f,
                96f,
            )
            val resizedRectObj = resizedRect!!
            assertThat(getFloat(resizedRectObj, "getWidthWorld")).isAtLeast(120f)
            assertThat(getFloat(resizedRectObj, "getHeightWorld")).isAtLeast(96f)
        }
    }

    @Test
    fun `top level helpers cover interpolation widget parsing and stroke bounds`() {
        val corners = callStatic(
            "worldBoundsOfCorners",
            arrayOf(WorldPoint::class.java, WorldPoint::class.java),
            WorldPoint(10f, 6f),
            WorldPoint(-4f, -8f),
        )
        val cornersObj = corners!!
        assertThat(getFloat(cornersObj, "getLeft")).isEqualTo(-4f)
        assertThat(getFloat(cornersObj, "getBottom")).isEqualTo(6f)

        val distance = getFloatFromAny(
            callStatic(
                "distance",
                arrayOf(WorldPoint::class.java, WorldPoint::class.java),
                WorldPoint(0f, 0f),
                WorldPoint(3f, 4f),
            ),
        )
        assertThat(distance).isWithin(0.0001f).of(5f)

        val easedOut = getFloatFromAny(callStatic("easeOutCubic", arrayOf(Float::class.javaPrimitiveType!!), 0.5f))
        val easedInOut = getFloatFromAny(callStatic("easeInOutCubic", arrayOf(Float::class.javaPrimitiveType!!), 0.75f))
        assertThat(easedOut).isGreaterThan(0.5f)
        assertThat(easedInOut).isAtMost(1f)

        val lerpedFloat = getFloatFromAny(
            callStatic(
                "lerp",
                arrayOf(Float::class.javaPrimitiveType!!, Float::class.javaPrimitiveType!!, Float::class.javaPrimitiveType!!),
                10f,
                30f,
                0.25f,
            ),
        )
        assertThat(lerpedFloat).isWithin(0.0001f).of(15f)

        val lerpedPoint = callStatic(
            "lerp",
            arrayOf(WorldPoint::class.java, WorldPoint::class.java, Float::class.javaPrimitiveType!!),
            WorldPoint(0f, 0f),
            WorldPoint(20f, 40f),
            0.5f,
        ) as WorldPoint
        assertThat(lerpedPoint).isEqualTo(WorldPoint(10f, 20f))

        val validWidget = callStatic(
            "toCanvasWidgetTypeOrNull",
            arrayOf(String::class.java),
            CanvasWidgetType.Weather.name,
        ) as CanvasWidgetType?
        val invalidWidget = callStatic("toCanvasWidgetTypeOrNull", arrayOf(String::class.java), "not-a-widget")
        assertThat(validWidget).isEqualTo(CanvasWidgetType.Weather)
        assertThat(invalidWidget).isNull()

        val textBounds = callStatic(
            "estimatedWorldBounds",
            arrayOf(CanvasTextObjectUiState::class.java),
            CanvasTextObjectUiState(
                id = "text-1",
                text = "",
                position = WorldPoint(16f, 12f),
                textSizeWorld = 40f,
                colorArgb = CanvasEditDefaults.DEFAULT_COLOR,
            ),
        )
        val textBoundsObj = textBounds!!
        assertThat(getFloat(textBoundsObj, "getRight")).isGreaterThan(getFloat(textBoundsObj, "getLeft"))

        val emptyStrokeBounds = callStatic(
            "worldBoundsWithStrokeWidth",
            arrayOf(CanvasStrokeUiState::class.java),
            CanvasStrokeUiState(
                id = "empty",
                points = emptyList(),
                colorArgb = CanvasEditDefaults.DEFAULT_COLOR,
                widthWorld = 8f,
            ),
        )
        assertThat(emptyStrokeBounds).isNull()

        val nonEmptyStrokeBounds = callStatic(
            "worldBoundsWithStrokeWidth",
            arrayOf(CanvasStrokeUiState::class.java),
            CanvasStrokeUiState(
                id = "stroke-1",
                points = listOf(WorldPoint(-10f, -5f), WorldPoint(12f, 8f)),
                colorArgb = CanvasEditDefaults.DEFAULT_COLOR,
                widthWorld = 10f,
            ),
        )
        assertThat(nonEmptyStrokeBounds).isNotNull()
    }

    @Test
    fun `private contact model and stroke conversion helpers stay consistent`() {
        val searchContact = searchContactClass.getDeclaredConstructor(
            String::class.java,
            String::class.java,
            String::class.java,
        ).apply {
            isAccessible = true
        }.newInstance("contact:7", "Alice", "+79990001122")

        val searchable = callStatic("asSearchableApp", arrayOf(searchContactClass), searchContact) as CanvasApp
        assertThat(searchable.packageName).isEqualTo("contact:7")
        assertThat(searchable.label).isEqualTo("Alice")

        val strokeUi = callStatic(
            "toUiState",
            arrayOf(CanvasStrokeWithPointsEntity::class.java),
            CanvasStrokeWithPointsEntity(
                stroke = CanvasStrokeEntity(
                    id = "s-1",
                    colorArgb = 0xFF123456.toInt(),
                    widthWorld = 9f,
                ),
                points = listOf(
                    CanvasStrokePointEntity("s-1", 1, 5f, 6f),
                    CanvasStrokePointEntity("s-1", 0, -2f, -3f),
                ),
            ),
        ) as CanvasStrokeUiState
        assertThat(strokeUi.points.first()).isEqualTo(WorldPoint(-2f, -3f))
        assertThat(strokeUi.points.last()).isEqualTo(WorldPoint(5f, 6f))
    }

    private fun worldBounds(left: Float, top: Float, right: Float, bottom: Float): Any {
        return worldBoundsClass.getDeclaredConstructor(
            Float::class.javaPrimitiveType,
            Float::class.javaPrimitiveType,
            Float::class.javaPrimitiveType,
            Float::class.javaPrimitiveType,
        ).apply {
            isAccessible = true
        }.newInstance(left, top, right, bottom)
    }

    private fun callStatic(name: String, parameterTypes: Array<Class<*>>, vararg args: Any?): Any? {
        val method = launcherViewModelKtClass.getDeclaredMethod(name, *parameterTypes)
        method.isAccessible = true
        return method.invoke(null, *args)
    }

    private fun callObj(target: Any, name: String, parameterTypes: Array<Class<*>>, vararg args: Any?): Any {
        val method = target.javaClass.getDeclaredMethod(name, *parameterTypes)
        method.isAccessible = true
        return method.invoke(target, *args)!!
    }

    private fun callBoolean(target: Any, name: String, parameterType: Class<*>, arg: Any): Boolean {
        val method = target.javaClass.getDeclaredMethod(name, parameterType)
        method.isAccessible = true
        return method.invoke(target, arg) as Boolean
    }

    private fun getFloat(target: Any, getterName: String): Float {
        val getter = target.javaClass.getDeclaredMethod(getterName)
        getter.isAccessible = true
        return getFloatFromAny(getter.invoke(target))
    }

    private fun getFloatFromAny(value: Any?): Float {
        return (value as Number).toFloat()
    }

    private companion object {
        private val launcherViewModelKtClass = Class.forName(
            "com.darksok.canvaslauncher.feature.launcher.presentation.LauncherViewModelKt",
        )
        private val worldBoundsClass = Class.forName(
            "com.darksok.canvaslauncher.feature.launcher.presentation.WorldBounds",
        )
        private val searchContactClass = Class.forName(
            "com.darksok.canvaslauncher.feature.launcher.presentation.SearchContact",
        )
    }
}
