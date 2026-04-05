package com.darksok.canvaslauncher.feature.launcher.presentation

import android.app.Notification
import android.content.Context
import android.os.Process
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class CanvasNotificationFeedTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun setUp() {
        CanvasNotificationFeed.clear()
    }

    @After
    fun tearDown() {
        CanvasNotificationFeed.clear()
    }

    @Test
    fun `replaceAll keeps only valid notifications sorted by post time`() {
        val oldest = sbn(
            key = "old",
            postTime = 1_000L,
            title = "Mail",
            text = "First line",
        )
        val newest = sbn(
            key = "new",
            postTime = 5_000L,
            title = "Chat",
            text = "Hi",
        )
        val ongoing = sbn(
            key = "ongoing",
            postTime = 9_000L,
            title = "Music",
            text = "Playing",
            flags = Notification.FLAG_ONGOING_EVENT,
        )
        val blank = sbn(
            key = "blank",
            postTime = 8_000L,
            title = " ",
            text = "\n",
        )

        CanvasNotificationFeed.replaceAll(arrayOf(oldest, newest, ongoing, blank))

        val entries = CanvasNotificationFeed.entries.value
        assertThat(entries).hasSize(2)
        assertThat(entries[0].postedAt).isGreaterThan(entries[1].postedAt)
        assertThat(entries[0].text).contains("Chat")
        assertThat(entries[0].text).contains("Hi")
    }

    @Test
    fun `upsert updates existing entry and remove handles blank keys safely`() {
        CanvasNotificationFeed.replaceAll(
            arrayOf(
                sbn(
                    key = "k1",
                    postTime = 2_000L,
                    title = "Title",
                    text = "Body",
                ),
            ),
        )

        CanvasNotificationFeed.upsert(
            sbn(
                key = "k1",
                postTime = 6_000L,
                title = "Title",
                text = "Line1\nLine2",
            ),
        )
        CanvasNotificationFeed.remove("")
        CanvasNotificationFeed.remove("missing")

        assertThat(CanvasNotificationFeed.entries.value).hasSize(1)
        assertThat(CanvasNotificationFeed.entries.value.single().text).isEqualTo("Title — Line1 Line2")
        assertThat(CanvasNotificationFeed.entries.value.single().postedAt).isEqualTo(6_000L)

        val actualKey = CanvasNotificationFeed.entries.value.single().key
        CanvasNotificationFeed.remove(actualKey)
        assertThat(CanvasNotificationFeed.entries.value).isEmpty()
    }

    @Test
    fun `clear and nullable inputs are no-op safe`() {
        CanvasNotificationFeed.upsert(null)
        CanvasNotificationFeed.remove(null)
        CanvasNotificationFeed.replaceAll(null)
        assertThat(CanvasNotificationFeed.entries.value).isEmpty()

        CanvasNotificationFeed.replaceAll(
            arrayOf(
                sbn(
                    key = "k2",
                    postTime = 1_500L,
                    title = "Alert",
                    text = "Body",
                    flags = Notification.FLAG_GROUP_SUMMARY,
                ),
            ),
        )
        assertThat(CanvasNotificationFeed.entries.value).isEmpty()

        CanvasNotificationFeed.clear()
        assertThat(CanvasNotificationFeed.entries.value).isEmpty()
    }

    private fun sbn(
        key: String,
        postTime: Long,
        title: String,
        text: String,
        flags: Int = 0,
    ): android.service.notification.StatusBarNotification {
        val notification = Notification.Builder(context, "test-channel")
            .setContentTitle(title)
            .setContentText(text)
            .build()
            .apply { this.flags = flags }

        return android.service.notification.StatusBarNotification(
            "com.example.source",
            "com.example.source",
            key.hashCode(),
            key,
            0,
            0,
            0,
            notification,
            Process.myUserHandle(),
            postTime,
        )
    }
}
