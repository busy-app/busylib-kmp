package com.flipperdevices.bridge.connection.di

import com.flipperdevices.bridge.connection.screens.ConnectionRootDecomposeComponent
import com.flipperdevices.core.di.AppGraph
import dev.zacsweers.metro.ContributesTo

@ContributesTo(AppGraph::class)
interface AppComponent {

    val rootComponentFactory: ConnectionRootDecomposeComponent.Factory
}
