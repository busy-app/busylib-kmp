package net.flipper.busylib.di

import android.content.Context
import com.flipperdevices.core.network.BUSYLibNetworkStateApi
import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.Settings
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import kotlinx.coroutines.CoroutineScope
import net.flipper.bsb.auth.principal.api.BUSYLibPrincipalApi
import net.flipper.bsb.cloud.api.BUSYLibHostApi
import net.flipper.busylib.BUSYLibAndroid
import net.flipper.busylib.core.di.BusyLibGraph

@DependencyGraph(BusyLibGraph::class)
internal interface BUSYLibGraphAndroid {
    val busyLib: BUSYLibAndroid

    @DependencyGraph.Factory
    fun interface Factory {
        @Suppress("LongParameterList")
        fun create(
            @Provides scope: CoroutineScope,
            @Provides principalApi: BUSYLibPrincipalApi,
            @Provides observableSettings: ObservableSettings,
            // Android-specific factory
            @Provides context: Context,
            @Provides hostApi: BUSYLibHostApi,
            @Provides networkStateApi: BUSYLibNetworkStateApi,
            @Provides settings: Settings
        ): BUSYLibGraphAndroid
    }
}
