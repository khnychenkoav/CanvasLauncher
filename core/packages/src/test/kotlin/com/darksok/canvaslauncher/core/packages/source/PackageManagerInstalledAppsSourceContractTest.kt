package com.darksok.canvaslauncher.core.packages.source

import com.google.common.truth.Truth.assertThat
import java.io.File
import org.junit.Test

class PackageManagerInstalledAppsSourceContractTest {

    @Test
    fun `source creates launcher intent from main action`() {
        assertThat(source()).contains("Intent(Intent.ACTION_MAIN)")
    }

    @Test
    fun `source adds launcher category`() {
        assertThat(source()).contains("addCategory(Intent.CATEGORY_LAUNCHER)")
    }

    @Test
    fun `source queries launcher activities from package manager`() {
        assertThat(source()).contains("packageManager.queryIntentActivities(launcherIntent, 0)")
    }

    @Test
    fun `source ignores entries without activity info`() {
        assertThat(source()).contains("val activityInfo = resolveInfo.activityInfo ?: return@mapNotNull null")
    }

    @Test
    fun `source ignores entries without package name`() {
        assertThat(source()).contains("val pkg = activityInfo.packageName ?: return@mapNotNull null")
    }

    @Test
    fun `source falls back to package name when label blank`() {
        assertThat(source()).contains("?.takeIf { it.isNotBlank() }\n                    ?: pkg")
    }

    @Test
    fun `source deduplicates apps by package name`() {
        assertThat(source()).contains("distinctBy { it.packageName }")
    }

    @Test
    fun `source sorts apps by lowercase label`() {
        assertThat(source()).contains("sortedBy { it.label.lowercase() }")
    }

    @Test
    fun `source resolves single app through launch intent`() {
        assertThat(source()).contains("val launchIntent = packageManager.getLaunchIntentForPackage(packageName) ?: return@withContext null")
    }

    @Test
    fun `source resolves single app through resolve activity`() {
        assertThat(source()).contains("val resolveInfo = packageManager.resolveActivity(launchIntent, 0) ?: return@withContext null")
    }

    @Test
    fun `source falls back to requested package name for missing label in single lookup`() {
        assertThat(source()).contains("?: packageName")
    }

    @Test
    fun `source returns installed app with requested package name`() {
        assertThat(source()).contains("InstalledApp(packageName = packageName, label = label)")
    }

    private fun source(): String = readSource("src/main/kotlin/com/darksok/canvaslauncher/core/packages/source/PackageManagerInstalledAppsSource.kt")

    private fun readSource(relativePath: String): String {
        val moduleFile = File(relativePath)
        val rootFile = File("core/packages/$relativePath")
        val file = when {
            moduleFile.exists() -> moduleFile
            rootFile.exists() -> rootFile
            else -> error("Missing source file for test: $relativePath")
        }
        return file.readText()
    }
}
