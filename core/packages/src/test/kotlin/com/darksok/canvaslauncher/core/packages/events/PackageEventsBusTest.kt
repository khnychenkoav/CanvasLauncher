package com.darksok.canvaslauncher.core.packages.events

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Test

class PackageEventsBusTest {

    @Test
    fun `added event is emitted to collectors`() = runTest {
        val bus = InMemoryPackageEventsBus()
        val deferred = async { bus.events.first() }

        bus.publish(PackageEvent.Added("pkg.added"))

        assertThat(deferred.await()).isEqualTo(PackageEvent.Added("pkg.added"))
    }

    @Test
    fun `removed event is emitted to collectors`() = runTest {
        val bus = InMemoryPackageEventsBus()
        val deferred = async { bus.events.first() }

        bus.publish(PackageEvent.Removed("pkg.removed"))

        assertThat(deferred.await()).isEqualTo(PackageEvent.Removed("pkg.removed"))
    }

    @Test
    fun `changed event is emitted to collectors`() = runTest {
        val bus = InMemoryPackageEventsBus()
        val deferred = async { bus.events.first() }

        bus.publish(PackageEvent.Changed("pkg.changed"))

        assertThat(deferred.await()).isEqualTo(PackageEvent.Changed("pkg.changed"))
    }

    @Test
    fun `events preserve publish order`() = runTest {
        val bus = InMemoryPackageEventsBus()

        bus.publish(PackageEvent.Added("one"))
        bus.publish(PackageEvent.Removed("two"))
        bus.publish(PackageEvent.Changed("three"))

        val events = bus.events.take(3).toList()
        assertThat(events).containsExactly(
            PackageEvent.Added("one"),
            PackageEvent.Removed("two"),
            PackageEvent.Changed("three"),
        ).inOrder()
    }

    @Test
    fun `late subscriber receives replayed events`() = runTest {
        val bus = InMemoryPackageEventsBus()
        repeat(3) { index ->
            bus.publish(PackageEvent.Added("pkg.$index"))
        }

        val replayed = bus.events.take(3).toList()

        assertThat(replayed.map { it.packageName }).containsExactly("pkg.0", "pkg.1", "pkg.2").inOrder()
    }

    @Test
    fun `replay keeps only latest sixteen events`() = runTest {
        val bus = InMemoryPackageEventsBus()
        repeat(20) { index ->
            bus.publish(PackageEvent.Added("pkg.$index"))
        }

        val replayed = bus.events.take(16).toList()

        assertThat(replayed.map { it.packageName }).containsExactly(
            "pkg.4", "pkg.5", "pkg.6", "pkg.7", "pkg.8", "pkg.9", "pkg.10", "pkg.11",
            "pkg.12", "pkg.13", "pkg.14", "pkg.15", "pkg.16", "pkg.17", "pkg.18", "pkg.19",
        ).inOrder()
    }

    @Test
    fun `duplicate events are preserved`() = runTest {
        val bus = InMemoryPackageEventsBus()

        bus.publish(PackageEvent.Changed("dup"))
        bus.publish(PackageEvent.Changed("dup"))

        val replayed = bus.events.take(2).toList()
        assertThat(replayed).containsExactly(PackageEvent.Changed("dup"), PackageEvent.Changed("dup")).inOrder()
    }

    @Test
    fun `empty package name is preserved`() = runTest {
        val bus = InMemoryPackageEventsBus()

        bus.publish(PackageEvent.Removed(""))

        assertThat(bus.events.first().packageName).isEmpty()
    }

    @Test
    fun `multiple collectors observe same event`() = runTest {
        val bus = InMemoryPackageEventsBus()
        val first = async { bus.events.first() }
        val second = async { bus.events.first() }

        bus.publish(PackageEvent.Added("shared"))

        assertThat(first.await()).isEqualTo(PackageEvent.Added("shared"))
        assertThat(second.await()).isEqualTo(PackageEvent.Added("shared"))
    }

    @Test
    fun `publishing many events keeps most recent replay window stable`() = runTest {
        val bus = InMemoryPackageEventsBus()
        repeat(80) { index -> bus.publish(PackageEvent.Added("pkg.$index")) }

        val replayed = bus.events.take(16).toList()

        assertThat(replayed.first().packageName).isEqualTo("pkg.64")
        assertThat(replayed.last().packageName).isEqualTo("pkg.79")
    }
}
