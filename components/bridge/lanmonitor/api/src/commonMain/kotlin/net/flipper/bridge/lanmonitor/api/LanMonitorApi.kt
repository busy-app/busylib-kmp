package net.flipper.bridge.lanmonitor.api

import kotlinx.coroutines.flow.StateFlow
import net.flipper.bridge.lanmonitor.model.ConnectedDeviceMetaInfo

interface LanMonitorApi {
    fun getConnectedDeviceFlow(): StateFlow<ConnectedDeviceMetaInfo?>
}
