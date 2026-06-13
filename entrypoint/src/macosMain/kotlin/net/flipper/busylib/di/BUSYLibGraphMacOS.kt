package net.flipper.busylib.di

import com.flipperdevices.core.network.BUSYLibNetworkStateApi
import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.Settings
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import kotlinx.coroutines.CoroutineScope
import net.flipper.bsb.auth.principal.api.BUSYLibPrincipalApi
import net.flipper.bsb.cloud.api.BUSYLibHostApi
import net.flipper.busylib.BUSYLibMacOS
import net.flipper.busylib.core.di.BusyLibGraph

// internal: the DI graph is an implementation detail. Keeping it out of the public surface stops
// Metro's merged @ContributesIntoMap binding methods from leaking into the exported ObjC/Swift header.
@DependencyGraph(BusyLibGraph::class)
internal interface BUSYLibGraphMacOS {
    val busyLib: BUSYLibMacOS

    @DependencyGraph.Factory
    fun interface Factory {
        @Suppress("LongParameterList")
        fun create(
            @Provides scope: CoroutineScope,
            @Provides principalApi: BUSYLibPrincipalApi,
            @Provides observableSettings: ObservableSettings,
            @Provides hostApi: BUSYLibHostApi,
            @Provides networkStateApi: BUSYLibNetworkStateApi,
            @Provides settings: Settings
        ): BUSYLibGraphMacOS
    }
}
