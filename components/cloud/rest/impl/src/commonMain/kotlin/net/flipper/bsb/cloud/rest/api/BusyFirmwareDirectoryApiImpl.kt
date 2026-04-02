package net.flipper.bsb.cloud.rest.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.CoroutineDispatcher
import dev.zacsweers.metro.Inject
import net.flipper.bsb.cloud.rest.model.BusyFirmwareDirectory
import net.flipper.busylib.core.di.BusyLibGraph
import net.flipper.core.busylib.ktx.common.runSuspendCatching
import net.flipper.core.ktor.di.qualifier.KtorNetworkClientQualifier
import net.flipper.core.ktor.di.qualifier.NetworkCoroutineDispatcher
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.binding
import dev.zacsweers.metro.SingleIn

@Inject
@SingleIn(BusyLibGraph::class)
@ContributesBinding(BusyLibGraph::class, binding = binding<BusyFirmwareDirectoryApi>())
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
