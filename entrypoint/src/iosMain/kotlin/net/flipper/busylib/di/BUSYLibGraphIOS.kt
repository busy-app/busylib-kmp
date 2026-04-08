package net.flipper.busylib.di

import com.flipperdevices.core.network.BUSYLibNetworkStateApi
import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.Settings
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import kotlinx.coroutines.CoroutineScope
import net.flipper.bsb.auth.principal.api.BUSYLibPrincipalApi
import net.flipper.bsb.cloud.api.BUSYLibHostApi
import net.flipper.busylib.BUSYLibIOS
import net.flipper.busylib.core.di.BusyLibGraph
import platform.CoreBluetooth.CBCentralManager

@Suppress("LongParameterList")
@DependencyGraph(scope = BusyLibGraph::class)
abstract class BUSYLibGraphIOS {
    abstract val busyLib: BUSYLibIOS

    @DependencyGraph.Factory
    interface Factory {
        fun create(
            @Provides scope: CoroutineScope,
            @Provides principalApi: BUSYLibPrincipalApi,
            @Provides observableSettings: ObservableSettings,
            @Provides manager: CBCentralManager,
            @Provides hostApi: BUSYLibHostApi,
            @Provides networkStateApi: BUSYLibNetworkStateApi,
            @Provides settings: Settings
        ): BUSYLibGraphIOS
    }
}
