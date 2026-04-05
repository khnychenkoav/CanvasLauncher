package com.darksok.canvaslauncher.feature.launcher.presentation

import android.app.Application
import android.content.Context
import android.graphics.Paint as AndroidPaint
import androidx.test.core.app.ApplicationProvider
import com.darksok.canvaslauncher.core.model.canvas.ScreenPoint
import com.google.common.truth.Truth.assertThat
import org.json.JSONArray
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class CanvasEditLayerInternalsTest {

    private val appContext: Application = ApplicationProvider.getApplicationContext()

    @Test
    fun `weather parsing handles invalid values and forecast limits`() {
        val emptyHourly = callStatic(
            "parseHourlyForecast",
            arrayOf(JSONArray::class.java, JSONArray::class.java, String::class.java),
            null,
            null,
            "2026-01-01T12:00",
        ) as List<*>
        assertThat(emptyHourly).isEmpty()

        val hourly = callStatic(
            "parseHourlyForecast",
            arrayOf(JSONArray::class.java, JSONArray::class.java, String::class.java),
            JSONArray(
                listOf(
                    "2026-01-01T11:00",
                    "2026-01-01T12:00",
                    "2026-01-01T13:00",
                    "2026-01-01T14:00",
                    "2026-01-01T15:00",
                ),
            ),
            JSONArray(listOf(1.0, 2.4, Double.NaN, 5.9, 8.1)),
            "2026-01-01T12:00",
        ) as List<*>
        assertThat(hourly).hasSize(3)
        val firstTemp = getInt(hourly.first()!!, "getTemperatureC")
        assertThat(firstTemp).isEqualTo(2)

        val emptyDaily = callStatic(
            "parseDailyForecast",
            arrayOf(JSONArray::class.java, JSONArray::class.java, JSONArray::class.java),
            JSONArray(listOf("2026-01-01")),
            JSONArray(listOf(8.2, 10.0)),
            JSONArray(listOf(2.1)),
        ) as List<*>
        assertThat(emptyDaily).isEmpty()

        val daily = callStatic(
            "parseDailyForecast",
            arrayOf(JSONArray::class.java, JSONArray::class.java, JSONArray::class.java),
            JSONArray(
                listOf(
                    "2026-01-01",
                    "2026-01-02",
                    "bad-date",
                    "2026-01-04",
                    "2026-01-05",
                ),
            ),
            JSONArray(listOf(8.2, 10.0, 11.0, 12.0, 13.0)),
            JSONArray(listOf(1.0, 3.0, 5.0, 7.0, 9.0)),
        ) as List<*>
        assertThat(daily).hasSize(3)
        assertThat(getInt(daily.first()!!, "getMaxTempC")).isEqualTo(8)
    }

    @Test
    fun `weather summaries and labels format correctly for empty and populated data`() {
        val emptyHourlySummary = callStatic(
            "formatHourlyForecastSummary",
            arrayOf(Context::class.java, List::class.java),
            appContext,
            emptyList<Any>(),
        ) as String
        assertThat(emptyHourlySummary).isNotEmpty()

        val emptyDailySummary = callStatic(
            "formatDailyForecastSummary",
            arrayOf(Context::class.java, List::class.java),
            appContext,
            emptyList<Any>(),
        ) as String
        assertThat(emptyDailySummary).isNotEmpty()

        val hourlyItems = listOf(
            newHourlyForecast("12:00", 7),
            newHourlyForecast("13:00", 9),
        )
        val dailyItems = listOf(
            newDailyForecast("Mon", 12, 3),
            newDailyForecast("Tue", 10, 2),
        )
        val hourlySummary = callStatic(
            "formatHourlyForecastSummary",
            arrayOf(Context::class.java, List::class.java),
            appContext,
            hourlyItems,
        ) as String
        val dailySummary = callStatic(
            "formatDailyForecastSummary",
            arrayOf(Context::class.java, List::class.java),
            appContext,
            dailyItems,
        ) as String

        assertThat(hourlySummary).contains("12:00")
        assertThat(dailySummary).contains("Mon")

        val knownCodeLabel = callStatic(
            "weatherCodeToLabel",
            arrayOf(Context::class.java, Int::class.javaPrimitiveType!!),
            appContext,
            95,
        ) as String
        val unknownCodeLabel = callStatic(
            "weatherCodeToLabel",
            arrayOf(Context::class.java, Int::class.javaPrimitiveType!!),
            appContext,
            -1,
        ) as String
        assertThat(knownCodeLabel).isNotEmpty()
        assertThat(unknownCodeLabel).isNotEmpty()
    }

    @Test
    fun `canvas geometry helpers handle edge pan text fitting and wrapping`() {
        val noPan = callStatic(
            "access\$edgeAutoPanDelta-ULxng0E",
            arrayOf(
                Long::class.javaPrimitiveType!!,
                Float::class.javaPrimitiveType!!,
                Float::class.javaPrimitiveType!!,
                Float::class.javaPrimitiveType!!,
                Float::class.javaPrimitiveType!!,
            ),
            callStatic(
                "access\$toOffset",
                arrayOf(ScreenPoint::class.java),
                ScreenPoint(50f, 50f),
            ) as Long,
            100f,
            100f,
            0f,
            12f,
        )
        assertThat(noPan).isNull()

        val pan = callStatic(
            "access\$edgeAutoPanDelta-ULxng0E",
            arrayOf(
                Long::class.javaPrimitiveType!!,
                Float::class.javaPrimitiveType!!,
                Float::class.javaPrimitiveType!!,
                Float::class.javaPrimitiveType!!,
                Float::class.javaPrimitiveType!!,
            ),
            callStatic(
                "access\$toOffset",
                arrayOf(ScreenPoint::class.java),
                ScreenPoint(2f, 98f),
            ) as Long,
            100f,
            100f,
            20f,
            16f,
        ) as ScreenPoint?
        assertThat(pan).isNotNull()
        assertThat(pan!!.x).isLessThan(0f)
        assertThat(pan.y).isGreaterThan(0f)

        val placements = callStatic(
            "frameHandlePlacements",
            arrayOf(Float::class.javaPrimitiveType!!, Float::class.javaPrimitiveType!!),
            220f,
            120f,
        ) as List<*>
        assertThat(placements).hasSize(CanvasFrameResizeHandle.entries.size)

        val stickyLayout = callStatic(
            "fitStickyTextLayout",
            arrayOf(
                String::class.java,
                Float::class.javaPrimitiveType!!,
                Float::class.javaPrimitiveType!!,
                Float::class.javaPrimitiveType!!,
            ),
            "Long sticky text that should wrap into lines",
            42f,
            120f,
            70f,
        )
        assertThat(getFloat(stickyLayout!!, "getTextSizePx")).isAtLeast(6f)
        assertThat(getInt(stickyLayout, "getMaxLines")).isAtLeast(1)

        val estimated = callStatic(
            "estimateTextBoundsPx",
            arrayOf(String::class.java, Float::class.javaPrimitiveType!!),
            "hello\nworld",
            22f,
        )
        assertThat(getFloat(estimated!!, "getWidthPx")).isGreaterThan(0f)
        assertThat(getFloat(estimated, "getHeightPx")).isGreaterThan(0f)

        val paint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply { textSize = 16f }
        val wrapped = callStatic(
            "wrapTextForWidth",
            arrayOf(String::class.java, AndroidPaint::class.java, Float::class.javaPrimitiveType!!, Int::class.javaPrimitiveType!!),
            "alpha beta gamma delta epsilon",
            paint,
            60f,
            2,
        ) as List<*>
        assertThat(wrapped).isNotEmpty()

        val ellipsized = callStatic(
            "ellipsizeToWidth",
            arrayOf(String::class.java, AndroidPaint::class.java, Float::class.javaPrimitiveType!!),
            "very long line for ellipsis",
            paint,
            50f,
        ) as String
        assertThat(ellipsized.length).isAtMost("very long line for ellipsis".length)
    }

    private fun newHourlyForecast(hour: String, temp: Int): Any {
        return liveHourlyClass.getDeclaredConstructor(String::class.java, Int::class.javaPrimitiveType!!)
            .apply { isAccessible = true }
            .newInstance(hour, temp)
    }

    private fun newDailyForecast(day: String, max: Int, min: Int): Any {
        return liveDailyClass.getDeclaredConstructor(
            String::class.java,
            Int::class.javaPrimitiveType!!,
            Int::class.javaPrimitiveType!!,
        ).apply {
            isAccessible = true
        }.newInstance(day, max, min)
    }

    private fun callStatic(name: String, parameterTypes: Array<Class<*>>, vararg args: Any?): Any? {
        val method = runCatching {
            canvasEditLayerKtClass.getDeclaredMethod(name, *parameterTypes)
        }.getOrElse {
            canvasEditLayerKtClass.declaredMethods.firstOrNull { candidate ->
                candidate.name == name && candidate.parameterCount == args.size
            } ?: throw it
        }
        method.isAccessible = true
        return method.invoke(null, *args)
    }

    private fun getFloat(target: Any, getterName: String): Float {
        val getter = target.javaClass.getDeclaredMethod(getterName)
        getter.isAccessible = true
        return (getter.invoke(target) as Number).toFloat()
    }

    private fun getInt(target: Any, getterName: String): Int {
        val getter = target.javaClass.getDeclaredMethod(getterName)
        getter.isAccessible = true
        return (getter.invoke(target) as Number).toInt()
    }

    private companion object {
        private val canvasEditLayerKtClass = Class.forName(
            "com.darksok.canvaslauncher.feature.launcher.presentation.CanvasEditLayerKt",
        )
        private val liveHourlyClass = Class.forName(
            "com.darksok.canvaslauncher.feature.launcher.presentation.LiveHourlyForecastEntry",
        )
        private val liveDailyClass = Class.forName(
            "com.darksok.canvaslauncher.feature.launcher.presentation.LiveDailyForecastEntry",
        )
    }
}
