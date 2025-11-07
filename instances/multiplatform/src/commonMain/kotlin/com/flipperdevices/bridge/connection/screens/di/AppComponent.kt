package com.flipperdevices.bridge.connection.screens.di

import com.flipperdevices.bridge.connection.screens.ConnectionRootDecomposeComponent
import com.flipperdevices.busylib.core.di.BusyLibGraph
import dev.zacsweers.metro.ContributesTo

@ContributesTo(BusyLibGraph::class)
interface AppComponent {

    val rootComponentFactory: ConnectionRootDecomposeComponent.Factory
}