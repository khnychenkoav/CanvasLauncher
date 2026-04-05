package com.darksok.canvaslauncher.core.packages.launch

import com.google.common.truth.Truth.assertThat
import java.io.File
import org.junit.Test

class PackageManagerAppLaunchServiceContractTest {

    @Test
    fun `service requests launch intent from package manager`() {
        assertThat(source()).contains("packageManager.getLaunchIntentForPackage(packageName)")
    }

    @Test
    fun `service returns launch unavailable when launch intent missing`() {
        assertThat(source()).contains("?: return AppResult.Failure(AppError.LaunchUnavailable)")
    }

    @Test
    fun `service starts activity with new task flag`() {
        assertThat(source()).contains("launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)")
    }

    @Test
    fun `service returns success after starting activity`() {
        assertThat(source()).contains("AppResult.Success(Unit)")
    }

    @Test
    fun `service catches activity not found exception`() {
        assertThat(source()).contains("catch (_: ActivityNotFoundException)")
    }

    @Test
    fun `service catches security exception`() {
        assertThat(source()).contains("catch (_: SecurityException)")
    }

    @Test
    fun `service maps known failures to launch unavailable`() {
        val source = source()

        assertThat(source).contains("AppResult.Failure(AppError.LaunchUnavailable)")
        assertThat(source.split("AppResult.Failure(AppError.LaunchUnavailable)").size).isAtLeast(3)
    }

    @Test
    fun `service catches generic throwable`() {
        assertThat(source()).contains("catch (throwable: Throwable)")
    }

    @Test
    fun `service wraps throwable message in unknown error`() {
        assertThat(source()).contains("AppError.Unknown(throwable.message)")
    }

    @Test
    fun `service delegates to application context startActivity`() {
        assertThat(source()).contains("context.startActivity(")
    }

    private fun source(): String = readSource("src/main/kotlin/com/darksok/canvaslauncher/core/packages/launch/PackageManagerAppLaunchService.kt")

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
