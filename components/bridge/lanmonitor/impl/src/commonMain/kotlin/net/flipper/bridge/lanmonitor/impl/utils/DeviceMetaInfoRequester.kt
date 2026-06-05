package net.flipper.bridge.lanmonitor.impl.utils

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.serialization.SerialName
import net.flipper.bridge.lanmonitor.model.ConnectedDeviceMetaInfo
import net.flipper.core.ktor.di.qualifier.KtorNetworkClientQualifier

private data class DeviceStatus(
    @SerialName("serial_number")
    val hardwareId: String
)

class DeviceMetaInfoRequester(
    @KtorNetworkClientQualifier
    private val httpClient: HttpClient,
) {
    suspend fun getMetaInfo() = runCatching {
        val deviceStatus = httpClient.get("/api/status/device").body<DeviceStatus>()

        return@runCatching ConnectedDeviceMetaInfo(
            hardwareId = deviceStatus.hardwareId
        )
    }
}