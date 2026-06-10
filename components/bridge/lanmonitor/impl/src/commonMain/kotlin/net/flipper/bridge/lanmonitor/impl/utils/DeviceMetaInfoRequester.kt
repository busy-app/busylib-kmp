package net.flipper.bridge.lanmonitor.impl.utils

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import me.tatarka.inject.annotations.Inject
import net.flipper.bridge.lanmonitor.api.BB_HOST
import net.flipper.bridge.lanmonitor.api.BB_PORT
import net.flipper.bridge.lanmonitor.model.ConnectedDeviceMetaInfo
import net.flipper.core.ktor.di.qualifier.KtorNetworkClientQualifier

@Serializable
private data class DeviceStatus(
    @SerialName("serial_number")
    val hardwareId: String
)

@Inject
class DeviceMetaInfoRequester(
    @KtorNetworkClientQualifier
    private val httpClient: HttpClient,
) {
    suspend fun getMetaInfo() = runCatching {
        val deviceStatus = httpClient.get("http://$BB_HOST:$BB_PORT/api/status/device").body<DeviceStatus>()

        return@runCatching ConnectedDeviceMetaInfo(
            hardwareId = deviceStatus.hardwareId
        )
    }
}