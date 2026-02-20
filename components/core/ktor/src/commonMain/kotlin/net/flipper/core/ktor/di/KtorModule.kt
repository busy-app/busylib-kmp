package net.flipper.core.ktor.di

import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineDispatcher
import me.tatarka.inject.annotations.Provides
import net.flipper.busylib.core.di.BusyLibGraph
import net.flipper.core.busylib.ktx.common.FlipperDispatchers
import net.flipper.core.ktor.di.qualifier.KtorNetworkClientQualifier
import net.flipper.core.ktor.di.qualifier.NetworkCoroutineDispatcher
import net.flipper.core.ktor.getHttpClient
import software.amazon.lastmile.kotlin.inject.anvil.ContributesTo
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

@ContributesTo(BusyLibGraph::class)
interface KtorModule {

    @Provides
    @SingleIn(BusyLibGraph::class)
    fun provideKtorNetworkHttpClient(): @KtorNetworkClientQualifier HttpClient {
        return getHttpClient()
    }

    @Provides
    @SingleIn(BusyLibGraph::class)
    fun provideNetworkCoroutineDispatcher(): @NetworkCoroutineDispatcher CoroutineDispatcher {
        return FlipperDispatchers.default
    }
}
