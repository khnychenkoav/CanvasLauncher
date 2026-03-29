package com.darksok.canvaslauncher.domain.layout

import com.darksok.canvaslauncher.core.model.app.InstalledApp
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SmartLayoutGroupingTest {

    @Test
    fun `groups known apps into semantic buckets`() {
        val apps = listOf(
            InstalledApp("org.telegram.messenger", "Telegram"),
            InstalledApp("com.whatsapp", "WhatsApp"),
            InstalledApp("com.instagram.android", "Instagram"),
            InstalledApp("com.google.android.youtube", "YouTube"),
            InstalledApp("com.google.android.calendar", "Calendar"),
            InstalledApp("com.microsoft.office.excel", "Excel"),
            InstalledApp("com.amazon.mShop.android.shopping", "Amazon"),
            InstalledApp("com.android.settings", "Settings"),
        )

        val grouped = SmartLayoutGrouping.group(apps)
        val ids = grouped.map { group -> group.id }

        assertThat(ids).contains("communication")
        assertThat(ids).contains("social")
        assertThat(ids).contains("work")
        assertThat(ids).contains("shopping")
        assertThat(ids).contains("system")
    }

    @Test
    fun `uses vendor fallback for unknown apps from same vendor`() {
        val apps = listOf(
            InstalledApp("com.mycorp.alpha", "Alpha"),
            InstalledApp("com.mycorp.beta", "Beta"),
            InstalledApp("org.single.vendor", "Single"),
        )

        val grouped = SmartLayoutGrouping.group(apps)
        val vendorGroup = grouped.firstOrNull { group -> group.id == "vendor-mycorp" }

        assertThat(vendorGroup).isNotNull()
        assertThat(vendorGroup!!.apps).hasSize(2)
        assertThat(grouped.any { group -> group.id == "utilities" }).isTrue()
    }
}
