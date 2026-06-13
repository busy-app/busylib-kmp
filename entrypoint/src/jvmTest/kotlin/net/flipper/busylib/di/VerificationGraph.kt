package net.flipper.busylib.di

import com.flipperdevices.core.network.BUSYLibNetworkStateApi
import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.Settings
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import kotlinx.coroutines.CoroutineScope
import net.flipper.bridge.connection.device.bsb.api.FBSBDeviceApi
import net.flipper.bridge.connection.feature.common.api.FDeviceFeature
import net.flipper.bridge.connection.feature.common.api.FDeviceFeatureApi
import net.flipper.bridge.connection.transport.common.api.DeviceConnectionApiHolder
import net.flipper.bsb.auth.principal.api.BUSYLibPrincipalApi
import net.flipper.bsb.cloud.api.BUSYLibHostApi
import net.flipper.bsb.watchers.api.InternalBUSYLibStartupListener
import net.flipper.busylib.core.di.BusyLibGraph
import net.flipper.eventbus.api.EventBusApi
import kotlin.reflect.KClass

/**
 * Test-only Metro graph scoped to [BusyLibGraph]. It aggregates the exact same contributions as the
 * production platform graphs but exposes the DI multibindings and a known singleton directly, so the
 * runtime contracts can be asserted against compiled binaries (see MetroGraphVerificationTest).
 */
@DependencyGraph(BusyLibGraph::class)
interface VerificationGraph {
    val featureFactories: Map<FDeviceFeature, FDeviceFeatureApi.Factory>
    val startupListeners: Set<InternalBUSYLibStartupListener>
    val connectionHolders: Map<KClass<*>, DeviceConnectionApiHolder>
    val bsbDeviceFactory: FBSBDeviceApi.Factory
    val eventBus: EventBusApi
    val eventBusProvider: () -> EventBusApi

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
        ): VerificationGraph
    }
}
