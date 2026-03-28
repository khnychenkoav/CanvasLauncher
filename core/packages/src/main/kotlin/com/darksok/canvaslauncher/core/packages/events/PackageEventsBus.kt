package com.darksok.canvaslauncher.core.packages.events

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

sealed interface PackageEvent {
    val packageName: String

    data class Added(override val packageName: String) : PackageEvent
    data class Removed(override val packageName: String) : PackageEvent
    data class Changed(override val packageName: String) : PackageEvent
}

interface PackageEventsBus {
    val events: Flow<PackageEvent>
    fun publish(event: PackageEvent)
}

@Singleton
class InMemoryPackageEventsBus @Inject constructor() : PackageEventsBus {

    private val flow = MutableSharedFlow<PackageEvent>(
        replay = 16,
        extraBufferCapacity = 64,
    )

    override val events: Flow<PackageEvent> = flow.asSharedFlow()

    override fun publish(event: PackageEvent) {
        flow.tryEmit(event)
    }
}
