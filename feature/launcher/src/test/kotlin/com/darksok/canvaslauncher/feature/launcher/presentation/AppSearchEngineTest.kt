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
}
