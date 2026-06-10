package net.flipper.bridge.lanmonitor.impl.utils

import net.flipper.bridge.lanmonitor.model.ConnectedDeviceMetaInfo

/**
 * Requests meta information (currently the hardware id) from a BUSY Bar reachable over LAN.
 *
 * Never throws: a failed request is returned as [Result.failure].
 */
interface DeviceMetaInfoRequester {
    suspend fun getMetaInfo(): Result<ConnectedDeviceMetaInfo>
}
