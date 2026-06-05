package net.flipper.bridge.lanmonitor.api

import kotlinx.coroutines.flow.StateFlow
import net.flipper.bridge.lanmonitor.model.ConnectedDeviceMetaInfo

const val BB_PORT = 80
const val BB_HOST = "10.0.4.20"

interface LanMonitorApi {
    fun getConnectedDeviceFlow(): StateFlow<ConnectedDeviceMetaInfo?>
}
