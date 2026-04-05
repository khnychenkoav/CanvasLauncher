package com.darksok.canvaslauncher.feature.apps.di

import com.darksok.canvaslauncher.domain.layout.InitialLayoutStrategy
import com.darksok.canvaslauncher.domain.layout.MultiPatternLayoutStrategy
import com.darksok.canvaslauncher.domain.repository.CanvasAppsStore
import com.darksok.canvaslauncher.feature.apps.data.RoomCanvasAppsStore
import com.google.common.truth.Truth.assertThat
import java.lang.reflect.Modifier
import org.junit.Test

class AppsDataModuleTest {

    @Test
    fun `provider creates multi pattern layout strategy`() {
        assertThat(AppsDataProvidersModule.provideInitialLayoutStrategy()).isInstanceOf(MultiPatternLayoutStrategy::class.java)
    }

    @Test
    fun `provider returns fresh strategy instance each time`() {
        val first = AppsDataProvidersModule.provideInitialLayoutStrategy()
        val second = AppsDataProvidersModule.provideInitialLayoutStrategy()

        assertThat(first).isNotSameInstanceAs(second)
    }

    @Test
    fun `provider return type matches initial layout strategy contract`() {
        val method = AppsDataProvidersModule::class.java.getDeclaredMethod("provideInitialLayoutStrategy")

        assertThat(method.returnType).isEqualTo(InitialLayoutStrategy::class.java)
    }

    @Test
    fun `binding method returns canvas apps store contract`() {
        val method = AppsDataBindingsModule::class.java.declaredMethods.single { it.name == "bindCanvasAppsStore" }

        assertThat(method.returnType).isEqualTo(CanvasAppsStore::class.java)
    }

    @Test
    fun `binding method takes room canvas apps store implementation`() {
        val method = AppsDataBindingsModule::class.java.declaredMethods.single { it.name == "bindCanvasAppsStore" }

        assertThat(method.parameterTypes.toList()).containsExactly(RoomCanvasAppsStore::class.java)
    }

    @Test
    fun `binding method is abstract to let hilt generate implementation`() {
        val method = AppsDataBindingsModule::class.java.declaredMethods.single { it.name == "bindCanvasAppsStore" }

        assertThat(Modifier.isAbstract(method.modifiers)).isTrue()
    }
}
