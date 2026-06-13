package net.flipper.bridge.lanmonitor.impl.utils

import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.flipper.bridge.lanmonitor.api.BB_HOST
import net.flipper.bridge.lanmonitor.api.BB_PORT
import net.flipper.bridge.lanmonitor.model.ConnectedDeviceMetaInfo
import net.flipper.busylib.core.di.BusyLibGraph
import net.flipper.core.busylib.ktx.common.runSuspendCatching
import net.flipper.core.ktor.di.qualifier.KtorNetworkClientQualifier

@Inject
@ContributesBinding(BusyLibGraph::class, binding = binding<DeviceMetaInfoRequester>())
class DeviceMetaInfoRequesterImpl(
    @KtorNetworkClientQualifier
    private val httpClient: HttpClient,
) : DeviceMetaInfoRequester {
    override suspend fun getMetaInfo(): Result<ConnectedDeviceMetaInfo> = runSuspendCatching {
        val deviceStatus = httpClient.get("http://$BB_HOST:$BB_PORT/api/status/device").body<DeviceStatus>()

        return@runSuspendCatching ConnectedDeviceMetaInfo(
            hardwareId = deviceStatus.hardwareId
        )
    }

    @Serializable
    private data class DeviceStatus(
        @SerialName("serial_number")
        val hardwareId: String
    )
}
