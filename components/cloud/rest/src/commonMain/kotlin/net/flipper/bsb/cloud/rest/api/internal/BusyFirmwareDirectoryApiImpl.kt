package net.flipper.bsb.cloud.rest.api.internal

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.CoroutineDispatcher
import me.tatarka.inject.annotations.Inject
import net.flipper.bsb.cloud.rest.api.BusyFirmwareDirectoryApi
import net.flipper.bsb.cloud.rest.model.BusyFirmwareDirectory
import net.flipper.busylib.core.di.BusyLibGraph
import net.flipper.core.busylib.ktx.common.runSuspendCatching
import net.flipper.core.ktor.di.qualifier.KtorNetworkClientQualifier
import net.flipper.core.ktor.di.qualifier.NetworkCoroutineDispatcher
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

@Inject
@SingleIn(BusyLibGraph::class)
@ContributesBinding(BusyLibGraph::class, BusyFirmwareDirectoryApi::class)
class BusyFirmwareDirectoryApiImpl(
    @KtorNetworkClientQualifier
    private val httpClient: HttpClient,
    @NetworkCoroutineDispatcher
    private val dispatcher: CoroutineDispatcher,
) : BusyFirmwareDirectoryApi {
    override suspend fun getFirmwareDirectory(): Result<BusyFirmwareDirectory> {
        return runSuspendCatching(dispatcher) {
            httpClient.get(HOST).body<BusyFirmwareDirectory>()
        }
    }

    companion object {
        internal const val HOST = "https://update.flipperzero.one/busybar-firmware/directory.json"
    }
}
