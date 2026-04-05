package com.darksok.canvaslauncher.domain.layout

import com.darksok.canvaslauncher.core.model.app.InstalledApp
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SmartLayoutGroupingTest {

    @Test
    fun `empty apps produce no groups`() {
        assertThat(SmartLayoutGrouping.group(emptyList())).isEmpty()
    }

    @Test
    fun `groups known apps into semantic buckets`() {
        val grouped = SmartLayoutGrouping.group(
            listOf(
                InstalledApp("org.telegram.messenger", "Telegram"),
                InstalledApp("com.instagram.android", "Instagram"),
                InstalledApp("com.google.android.youtube", "YouTube"),
                InstalledApp("com.microsoft.office.excel", "Excel"),
                InstalledApp("com.amazon.mShop.android.shopping", "Amazon"),
                InstalledApp("com.android.settings", "Settings"),
            ),
        )
        val ids = grouped.map { it.id }
        assertThat(ids).containsAtLeast("communication", "social", "system")
    }

    @Test
    fun `deduplicates apps by package name`() {
        val grouped = SmartLayoutGrouping.group(
            listOf(
                InstalledApp("pkg.same", "Alpha"),
                InstalledApp("pkg.same", "Beta"),
            ),
        )
        assertThat(grouped.sumOf { it.apps.size }).isEqualTo(1)
    }

    @Test
    fun `uses vendor fallback for unknown apps from same vendor`() {
        val grouped = SmartLayoutGrouping.group(
            listOf(
                InstalledApp("com.mycorp.alpha", "Alpha"),
                InstalledApp("com.mycorp.beta", "Beta"),
            ),
        )
        val vendorGroup = grouped.single { it.id == "vendor-mycorp" }
        assertThat(vendorGroup.apps).hasSize(2)
    }

    @Test
    fun `generic vendor keys fall back to utilities`() {
        val grouped = SmartLayoutGrouping.group(
            listOf(
                InstalledApp("com.app.alpha", "Alpha"),
                InstalledApp("com.app.beta", "Beta"),
            ),
        )
        assertThat(grouped.map { it.id }).contains("utilities")
        assertThat(grouped.map { it.id }).doesNotContain("vendor-app")
    }

    @Test
    fun `vendor titles are humanized`() {
        val grouped = SmartLayoutGrouping.group(
            listOf(
                InstalledApp("com.cool_vendor.alpha", "Alpha"),
                InstalledApp("com.cool_vendor.beta", "Beta"),
            ),
        )
        val vendorGroup = grouped.single { it.id == "vendor-cool_vendor" }
        assertThat(vendorGroup.title).isEqualTo("Cool Vendor")
    }

    @Test
    fun `system packages fall into system group when no stronger semantic matches`() {
        val grouped = SmartLayoutGrouping.group(listOf(InstalledApp("com.android.permissioncontroller", "Permission Controller")))
        assertThat(grouped.single().id).isEqualTo("system")
    }

    @Test
    fun `apps inside group are sorted alphabetically by label`() {
        val grouped = SmartLayoutGrouping.group(
            listOf(
                InstalledApp("org.telegram.beta", "Zulu"),
                InstalledApp("org.telegram.alpha", "Alpha"),
            ),
        )
        assertThat(grouped.single().apps.map { it.label }).containsExactly("Alpha", "Zulu").inOrder()
    }

    @Test
    fun `groups are sorted by priority before utilities`() {
        val grouped = SmartLayoutGrouping.group(
            listOf(
                InstalledApp("com.android.settings", "Settings"),
                InstalledApp("org.telegram.messenger", "Telegram"),
                InstalledApp("pkg.misc.tool", "Tool"),
            ),
        )
        val ids = grouped.map { it.id }
        assertThat(ids.indexOf("communication")).isLessThan(ids.indexOf("utilities"))
    }

    @Test
    fun `keyword matching is case insensitive`() {
        val grouped = SmartLayoutGrouping.group(listOf(InstalledApp("pkg.mail", "GMAIL")))
        assertThat(grouped.single().id).isEqualTo("communication")
    }
}
