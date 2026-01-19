package net.flipper.bridge.connection.transport.combined.impl.di

import me.tatarka.inject.annotations.IntoMap
import me.tatarka.inject.annotations.Provides
import net.flipper.bridge.connection.transport.combined.CombinedConnectionApi
import net.flipper.bridge.connection.transport.combined.FCombinedConnectionConfig
import net.flipper.bridge.connection.transport.common.api.DeviceConnectionApiHolder
import net.flipper.busylib.core.di.BusyLibGraph
import software.amazon.lastmile.kotlin.inject.anvil.ContributesTo
import kotlin.reflect.KClass

@ContributesTo(BusyLibGraph::class)
interface CombinedConnectionModule {
    @IntoMap
    @Provides
    fun getCombinedConnection(
        combinedConnectionApi: CombinedConnectionApi
    ): Pair<KClass<*>, DeviceConnectionApiHolder> {
        return FCombinedConnectionConfig::class to DeviceConnectionApiHolder(
            combinedConnectionApi
        )
    }
}
