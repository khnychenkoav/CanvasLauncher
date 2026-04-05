package com.darksok.canvaslauncher.core.packages.di

import com.google.common.truth.Truth.assertThat
import java.io.File
import org.junit.Test

class PackagesModuleContractTest {

    @Test
    fun `providers module exposes package manager provider`() {
        assertThat(providersSource()).contains("fun providePackageManager(")
    }

    @Test
    fun `providers module returns context package manager`() {
        assertThat(providersSource()).contains("PackageManager = context.packageManager")
    }

    @Test
    fun `providers module exposes dispatchers provider`() {
        assertThat(providersSource()).contains("fun provideDispatchersProvider(): DispatchersProvider = DefaultDispatchersProvider()")
    }

    @Test
    fun `bindings module binds installed apps source`() {
        assertThat(bindingsSource()).contains("abstract fun bindInstalledAppsSource(")
    }

    @Test
    fun `bindings module binds app launch service`() {
        assertThat(bindingsSource()).contains("abstract fun bindAppLaunchService(")
    }

    @Test
    fun `bindings module binds icon cache gateway`() {
        assertThat(bindingsSource()).contains("abstract fun bindIconCacheGateway(")
    }

    @Test
    fun `bindings module binds icon bitmap store`() {
        assertThat(bindingsSource()).contains("abstract fun bindIconBitmapStore(")
    }

    @Test
    fun `bindings module binds package events bus`() {
        assertThat(bindingsSource()).contains("abstract fun bindPackageEventsBus(")
    }

    private fun providersSource(): String = readSource("src/main/kotlin/com/darksok/canvaslauncher/core/packages/di/PackagesModule.kt")

    private fun bindingsSource(): String = providersSource()

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
