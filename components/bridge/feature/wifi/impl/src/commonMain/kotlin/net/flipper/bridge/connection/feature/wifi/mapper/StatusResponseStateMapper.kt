package net.flipper.bridge.connection.feature.wifi.mapper

import net.flipper.bridge.connection.feature.rpc.generated.model.StatusResponse
import net.flipper.bridge.connection.feature.wifi.api.model.BsbWifiStatusResponse

internal fun StatusResponse.State.toBsbWifiState(): BsbWifiStatusResponse.BsbWifiState {
    return when (this) {
        StatusResponse.State.UNKNOWN -> BsbWifiStatusResponse.BsbWifiState.UNKNOWN
        StatusResponse.State.DISCONNECTED -> BsbWifiStatusResponse.BsbWifiState.DISCONNECTED
        StatusResponse.State.CONNECTED -> BsbWifiStatusResponse.BsbWifiState.CONNECTED
        StatusResponse.State.CONNECTING -> BsbWifiStatusResponse.BsbWifiState.CONNECTING
        StatusResponse.State.DISCONNECTING -> BsbWifiStatusResponse.BsbWifiState.DISCONNECTING
        StatusResponse.State.RECONNECTING -> BsbWifiStatusResponse.BsbWifiState.RECONNECTING
    }
}
