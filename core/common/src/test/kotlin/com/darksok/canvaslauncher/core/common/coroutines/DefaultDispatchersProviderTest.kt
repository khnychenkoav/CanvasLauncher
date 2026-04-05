package com.darksok.canvaslauncher.core.common.coroutines

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultDispatchersProviderTest {

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `io dispatcher points to global io`() {
        val provider = DefaultDispatchersProvider()
        assertThat(provider.io).isSameInstanceAs(Dispatchers.IO)
    }

    @Test
    fun `default dispatcher points to global default`() {
        val provider = DefaultDispatchersProvider()
        assertThat(provider.default).isSameInstanceAs(Dispatchers.Default)
    }

    @Test
    fun `main dispatcher points to current main dispatcher`() {
        val dispatcher = StandardTestDispatcher()
        Dispatchers.setMain(dispatcher)
        val provider = DefaultDispatchersProvider()
        assertThat(provider.main).isSameInstanceAs(Dispatchers.Main)
    }

    @Test
    fun `provider main remains aligned with current main after update`() {
        val first = StandardTestDispatcher()
        val second = newSingleThreadContext("provider-main")
        Dispatchers.setMain(first)
        val provider = DefaultDispatchersProvider()
        Dispatchers.setMain(second)
        assertThat(provider.main).isSameInstanceAs(Dispatchers.Main)
        second.close()
    }

    @Test
    fun `provider main stays usable across multiple main swaps`() {
        val first = StandardTestDispatcher()
        val second = StandardTestDispatcher()
        Dispatchers.setMain(first)
        val provider = DefaultDispatchersProvider()
        Dispatchers.setMain(second)
        Dispatchers.setMain(first)
        assertThat(provider.main).isSameInstanceAs(Dispatchers.Main)
    }

    @Test
    fun `io and default dispatchers stay distinct`() {
        val provider = DefaultDispatchersProvider()
        assertThat(provider.io).isNotSameInstanceAs(provider.default)
    }

    @Test
    fun `main dispatcher can differ from default dispatcher`() {
        val dispatcher = StandardTestDispatcher()
        Dispatchers.setMain(dispatcher)
        val provider = DefaultDispatchersProvider()
        assertThat(provider.main).isNotSameInstanceAs(provider.default)
    }

    @Test
    fun `main dispatcher can differ from io dispatcher`() {
        val dispatcher = StandardTestDispatcher()
        Dispatchers.setMain(dispatcher)
        val provider = DefaultDispatchersProvider()
        assertThat(provider.main).isNotSameInstanceAs(provider.io)
    }

    @Test
    fun `provider created twice shares io dispatcher`() {
        val first = DefaultDispatchersProvider()
        val second = DefaultDispatchersProvider()
        assertThat(first.io).isSameInstanceAs(second.io)
    }

    @Test
    fun `provider created twice shares default dispatcher`() {
        val first = DefaultDispatchersProvider()
        val second = DefaultDispatchersProvider()
        assertThat(first.default).isSameInstanceAs(second.default)
    }

    @Test
    fun `provider created after resetting main uses global main`() {
        val dispatcher = StandardTestDispatcher()
        Dispatchers.setMain(dispatcher)
        Dispatchers.resetMain()
        val provider = DefaultDispatchersProvider()
        assertThat(provider.main).isSameInstanceAs(Dispatchers.Main)
    }

    @Test
    fun `custom provider implementation can expose explicit dispatchers`() {
        val io = StandardTestDispatcher()
        val default = StandardTestDispatcher()
        val main = StandardTestDispatcher()
        val provider = object : DispatchersProvider {
            override val io = io
            override val default = default
            override val main = main
        }
        assertThat(provider.io).isSameInstanceAs(io)
        assertThat(provider.default).isSameInstanceAs(default)
        assertThat(provider.main).isSameInstanceAs(main)
    }

    @Test
    fun `custom provider implementation may reuse same dispatcher for all roles`() {
        val dispatcher = StandardTestDispatcher()
        val provider = object : DispatchersProvider {
            override val io = dispatcher
            override val default = dispatcher
            override val main = dispatcher
        }
        assertThat(provider.io).isSameInstanceAs(dispatcher)
        assertThat(provider.default).isSameInstanceAs(dispatcher)
        assertThat(provider.main).isSameInstanceAs(dispatcher)
    }

    @Test
    fun `dispatchers provider contract exposes three dispatcher properties`() {
        val provider: DispatchersProvider = DefaultDispatchersProvider()
        assertThat(listOf(provider.io, provider.default, provider.main)).hasSize(3)
    }

    @Test
    fun `default provider main is stable across repeated reads`() {
        val dispatcher = StandardTestDispatcher()
        Dispatchers.setMain(dispatcher)
        val provider = DefaultDispatchersProvider()
        assertThat(provider.main).isSameInstanceAs(provider.main)
    }

    @Test
    fun `default provider io is stable across repeated reads`() {
        val provider = DefaultDispatchersProvider()
        assertThat(provider.io).isSameInstanceAs(provider.io)
    }

    @Test
    fun `default provider default is stable across repeated reads`() {
        val provider = DefaultDispatchersProvider()
        assertThat(provider.default).isSameInstanceAs(provider.default)
    }
}
