package com.darksok.canvaslauncher.feature.launcher.presentation

import android.Manifest
import android.app.Application
import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.database.MatrixCursor
import android.graphics.Paint as AndroidPaint
import android.net.Uri
import android.provider.CalendarContract
import androidx.test.core.app.ApplicationProvider
import com.darksok.canvaslauncher.core.model.canvas.ScreenPoint
import com.darksok.canvaslauncher.feature.launcher.R
import com.google.common.truth.Truth.assertThat
import org.json.JSONArray
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowContentResolver

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

    @Test
    fun `notification ticker refresh delay preserves stationary and scroll cadence`() {
        val atCycleStart = callStatic(
            "notificationTickerRefreshDelayMs",
            arrayOf(Long::class.javaPrimitiveType!!),
            0L,
        ) as Long
        val nearStationaryEnd = callStatic(
            "notificationTickerRefreshDelayMs",
            arrayOf(Long::class.javaPrimitiveType!!),
            4_999L,
        ) as Long
        val atScrollStart = callStatic(
            "notificationTickerRefreshDelayMs",
            arrayOf(Long::class.javaPrimitiveType!!),
            5_000L,
        ) as Long
        val nearScrollEnd = callStatic(
            "notificationTickerRefreshDelayMs",
            arrayOf(Long::class.javaPrimitiveType!!),
            5_849L,
        ) as Long

        assertThat(atCycleStart).isEqualTo(5_000L)
        assertThat(nearStationaryEnd).isEqualTo(1L)
        assertThat(atScrollStart).isEqualTo(48L)
        assertThat(nearScrollEnd).isEqualTo(1L)
    }

    @Test
    fun `calendar event time formatting supports all day range and point events`() {
        val allDayEvent = newCalendarEvent(
            title = "All day event",
            startTimeMs = 1_700_000_000_000L,
            endTimeMs = 1_700_003_600_000L,
            allDay = true,
        )
        val allDayFormatted = callStatic(
            "formatCalendarEventTime",
            arrayOf(Context::class.java, calendarEventClass),
            appContext,
            allDayEvent,
        ) as String
        assertThat(allDayFormatted).isEqualTo(
            appContext.getString(R.string.widget_calendar_all_day),
        )

        val startMs = 1_700_000_000_000L
        val endMs = startMs + 3_600_000L
        val timedEvent = newCalendarEvent(
            title = "Timed event",
            startTimeMs = startMs,
            endTimeMs = endMs,
            allDay = false,
        )
        val timedFormatted = callStatic(
            "formatCalendarEventTime",
            arrayOf(Context::class.java, calendarEventClass),
            appContext,
            timedEvent,
        ) as String
        val timeFormatter = android.text.format.DateFormat.getTimeFormat(appContext)
        val expectedRange = appContext.getString(
            R.string.widget_calendar_time_range,
            timeFormatter.format(java.util.Date(startMs)),
            timeFormatter.format(java.util.Date(endMs)),
        )
        assertThat(timedFormatted).isEqualTo(expectedRange)

        val pointEvent = newCalendarEvent(
            title = "Point event",
            startTimeMs = startMs,
            endTimeMs = startMs,
            allDay = false,
        )
        val pointFormatted = callStatic(
            "formatCalendarEventTime",
            arrayOf(Context::class.java, calendarEventClass),
            appContext,
            pointEvent,
        ) as String
        assertThat(pointFormatted).isEqualTo(timeFormatter.format(java.util.Date(startMs)))
    }

    @Test
    fun `calendar query returns today's events with limit and field mapping`() {
        val startOfDay = java.time.LocalDate.now()
            .atStartOfDay(java.time.ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        FakeCalendarProvider.rows = (0 until 10).map { index ->
            val start = startOfDay + (index * 3_600_000L)
            FakeCalendarProvider.Row(
                title = "Event $index",
                begin = start,
                end = start + 1_800_000L,
                allDay = if (index == 0) 1 else 0,
            )
        }
        ShadowContentResolver.registerProviderInternal(
            CalendarContract.AUTHORITY,
            FakeCalendarProvider(),
        )
        shadowOf(appContext).grantPermissions(Manifest.permission.READ_CALENDAR)

        val events = callStatic(
            "queryTodayCalendarEvents",
            arrayOf(Context::class.java),
            appContext,
        ) as List<*>

        assertThat(events).hasSize(8)
        val first = events.first()!!
        assertThat(getString(first, "getTitle")).isEqualTo("Event 0")
        assertThat(getBoolean(first, "getAllDay")).isTrue()
        assertThat(getLong(first, "getStartTimeMs")).isEqualTo(startOfDay)
    }

    @Test
    fun `calendar permission helper tracks runtime grant`() {
        val beforeGrant = callStatic(
            "hasCalendarPermission",
            arrayOf(Context::class.java),
            appContext,
        ) as Boolean
        assertThat(beforeGrant).isFalse()

        shadowOf(appContext).grantPermissions(Manifest.permission.READ_CALENDAR)
        val afterGrant = callStatic(
            "hasCalendarPermission",
            arrayOf(Context::class.java),
            appContext,
        ) as Boolean
        assertThat(afterGrant).isTrue()
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

    private fun newCalendarEvent(
        title: String,
        startTimeMs: Long,
        endTimeMs: Long,
        allDay: Boolean,
    ): Any {
        return calendarEventClass.getDeclaredConstructor(
            String::class.java,
            Long::class.javaPrimitiveType!!,
            Long::class.javaPrimitiveType!!,
            Boolean::class.javaPrimitiveType!!,
        ).apply {
            isAccessible = true
        }.newInstance(title, startTimeMs, endTimeMs, allDay)
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

    private fun getLong(target: Any, getterName: String): Long {
        val getter = target.javaClass.getDeclaredMethod(getterName)
        getter.isAccessible = true
        return (getter.invoke(target) as Number).toLong()
    }

    private fun getBoolean(target: Any, getterName: String): Boolean {
        val getter = target.javaClass.getDeclaredMethod(getterName)
        getter.isAccessible = true
        return getter.invoke(target) as Boolean
    }

    private fun getString(target: Any, getterName: String): String {
        val getter = target.javaClass.getDeclaredMethod(getterName)
        getter.isAccessible = true
        return getter.invoke(target) as String
    }

    private class FakeCalendarProvider : ContentProvider() {
        data class Row(
            val title: String,
            val begin: Long,
            val end: Long,
            val allDay: Int,
        )

        override fun onCreate(): Boolean = true

        override fun query(
            uri: Uri,
            projection: Array<out String>?,
            selection: String?,
            selectionArgs: Array<out String>?,
            sortOrder: String?,
        ): Cursor {
            val columns = projection ?: arrayOf(
                CalendarContract.Instances.BEGIN,
                CalendarContract.Instances.END,
                CalendarContract.Instances.TITLE,
                CalendarContract.Instances.ALL_DAY,
            )
            val cursor = MatrixCursor(columns)
            rows.forEach { row ->
                cursor.addRow(
                    arrayOf(
                        row.begin,
                        row.end,
                        row.title,
                        row.allDay,
                    ),
                )
            }
            return cursor
        }

        override fun getType(uri: Uri): String? = null

        override fun insert(uri: Uri, values: ContentValues?): Uri? = null

        override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

        override fun update(
            uri: Uri,
            values: ContentValues?,
            selection: String?,
            selectionArgs: Array<out String>?,
        ): Int = 0

        companion object {
            var rows: List<Row> = emptyList()
        }
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
        private val calendarEventClass = Class.forName(
            "com.darksok.canvaslauncher.feature.launcher.presentation.CalendarEventEntry",
        )
    }
}
