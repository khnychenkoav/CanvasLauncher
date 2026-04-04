package com.darksok.canvaslauncher.feature.launcher.presentation

import com.darksok.canvaslauncher.core.model.app.CanvasApp
import com.darksok.canvaslauncher.core.model.canvas.WorldPoint
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AppSearchEngineTest {

    private val apps = listOf(
        CanvasApp(packageName = "com.camera.pro", label = "Camera Pro", position = WorldPoint(0f, 0f)),
        CanvasApp(packageName = "com.chat.app", label = "Chat", position = WorldPoint(10f, 0f)),
        CanvasApp(packageName = "com.calendar.app", label = "Calendar", position = WorldPoint(20f, 0f)),
        CanvasApp(packageName = "org.toolbox.mailbox", label = "Mailbox", position = WorldPoint(30f, 0f)),
    )

    @Test
    fun `blank query returns empty result`() {
        val result = AppSearchEngine.rankByLabel(query = "   ", apps = apps)

        assertThat(result).isEmpty()
    }

    @Test
    fun `prefix match has higher priority than contains match`() {
        val result = AppSearchEngine.rankByLabel(query = "cal", apps = apps)

        assertThat(result.first().label).isEqualTo("Calendar")
    }

    @Test
    fun `exact label wins over package-only match`() {
        val result = AppSearchEngine.rankByLabel(query = "chat", apps = apps)

        assertThat(result.first().label).isEqualTo("Chat")
    }

    @Test
    fun `subsequence fallback still finds reasonable result`() {
        val result = AppSearchEngine.rankByLabel(query = "mbx", apps = apps)

        assertThat(result.firstOrNull()?.label).isEqualTo("Mailbox")
    }

    @Test
    fun `typo still finds expected app`() {
        val result = AppSearchEngine.rankByLabel(query = "calemdar", apps = apps)

        assertThat(result.firstOrNull()?.label).isEqualTo("Calendar")
    }

    @Test
    fun `indexed ranking matches direct ranking`() {
        val indexed = AppSearchEngine.rankByLabel(
            query = "calemdar",
            searchIndex = AppSearchEngine.buildIndex(apps),
        )
        val direct = AppSearchEngine.rankByLabel(query = "calemdar", apps = apps)

        assertThat(indexed).isEqualTo(direct)
    }

    @Test
    fun `search index keeps pre-sorted labels for blank query rendering`() {
        val index = AppSearchEngine.buildIndex(apps)

        val labels = index.entriesSortedByLabel.map { entry -> entry.label }

        assertThat(labels).containsExactly("Calendar", "Camera Pro", "Chat", "Mailbox").inOrder()
    }

    @Test
    fun `latin transliteration matches cyrillic label`() {
        val translitApps = listOf(
            CanvasApp(packageName = "com.android.dialer", label = "Звонок", position = WorldPoint(0f, 0f)),
            CanvasApp(packageName = "com.android.camera", label = "Камера", position = WorldPoint(10f, 0f)),
        )

        val result = AppSearchEngine.rankByLabel(query = "zvonok", apps = translitApps)

        assertThat(result.firstOrNull()?.label).isEqualTo("Звонок")
    }

    @Test
    fun `wrong keyboard layout still resolves query`() {
        val result = AppSearchEngine.rankByLabel(query = "сфьукф", apps = apps)

        assertThat(result.firstOrNull()?.label).isEqualTo("Camera Pro")
    }

    @Test
    fun `english call query matches russian phone app`() {
        val localizedApps = listOf(
            CanvasApp(packageName = "com.android.dialer", label = "Звонок", position = WorldPoint(0f, 0f)),
            CanvasApp(packageName = "com.android.gallery", label = "Галерея", position = WorldPoint(10f, 0f)),
        )

        val result = AppSearchEngine.rankByLabel(query = "call", apps = localizedApps)

        assertThat(result.firstOrNull()?.label).isEqualTo("Звонок")
    }

    @Test
    fun `russian call query matches english phone app`() {
        val localizedApps = listOf(
            CanvasApp(packageName = "com.android.dialer", label = "Call", position = WorldPoint(0f, 0f)),
            CanvasApp(packageName = "com.android.camera", label = "Camera", position = WorldPoint(10f, 0f)),
        )

        val result = AppSearchEngine.rankByLabel(query = "звонок", apps = localizedApps)

        assertThat(result.firstOrNull()?.label).isEqualTo("Call")
    }

}
