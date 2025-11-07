package com.flipperdevices.bridge.connection.di

import com.flipperdevices.bridge.connection.screens.di.AppComponent
import com.flipperdevices.bridge.connection.service.api.FConnectionService
import com.flipperdevices.busylib.core.di.BusyLibGraph
import com.russhwolf.settings.ObservableSettings
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineScope

@DependencyGraph(BusyLibGraph::class)
@SingleIn(BusyLibGraph::class)
abstract class iOSAppComponent : AppComponent {
    @DependencyGraph.Factory
    fun interface Factory {
        fun create(
            @Provides observableSettings: ObservableSettings,
            @Provides scope: CoroutineScope,
        ): iOSAppComponent
    }
}
