package net.flipper.bridge.connection.utils

import com.flipperdevices.core.network.BUSYLibNetworkStateApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.flow.MutableStateFlow
import net.flipper.busylib.core.wrapper.WrappedStateFlow
import net.flipper.busylib.core.wrapper.wrap
import platform.Network.nw_path_get_status
import platform.Network.nw_path_monitor_create
import platform.Network.nw_path_monitor_set_queue
import platform.Network.nw_path_monitor_set_update_handler
import platform.Network.nw_path_monitor_start
import platform.Network.nw_path_status_satisfied
import platform.darwin.dispatch_queue_create

@OptIn(ExperimentalForeignApi::class)
class BUSYLibNetworkStateApple : BUSYLibNetworkStateApi {
    private val isNetworkAvailableMutableFlow = MutableStateFlow(false)

    override val isNetworkAvailableFlow: WrappedStateFlow<Boolean> = isNetworkAvailableMutableFlow.wrap()

    init {
        val monitor = nw_path_monitor_create()
        val queue = dispatch_queue_create("net.flipper.network.monitor", null)

        nw_path_monitor_set_update_handler(monitor) { path ->
            val satisfied = nw_path_get_status(path) == nw_path_status_satisfied
            isNetworkAvailableMutableFlow.value = satisfied
        }

        nw_path_monitor_set_queue(monitor, queue)
        nw_path_monitor_start(monitor)
    }
}
