package net.flipper.core.ktor.di

import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineDispatcher
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import net.flipper.busylib.core.di.BusyLibGraph
import net.flipper.core.busylib.ktx.common.FlipperDispatchers
import net.flipper.core.ktor.di.qualifier.KtorNetworkClientQualifier
import net.flipper.core.ktor.di.qualifier.NetworkCoroutineDispatcher
import net.flipper.core.ktor.getHttpClient

@ContributesTo(BusyLibGraph::class)
interface KtorModule {

    @Provides
    @SingleIn(BusyLibGraph::class)
    @KtorNetworkClientQualifier
    fun provideKtorNetworkHttpClient(): HttpClient {
        return getHttpClient()
    }

    @Provides
    @SingleIn(BusyLibGraph::class)
    @NetworkCoroutineDispatcher
    fun provideNetworkCoroutineDispatcher(): CoroutineDispatcher {
        return FlipperDispatchers.default
    }
}
