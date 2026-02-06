package net.flipper.busylib

import com.flipperdevices.core.network.BUSYLibNetworkStateApi
import kotlinx.coroutines.flow.MutableStateFlow
import net.flipper.busylib.core.wrapper.WrappedStateFlow
import net.flipper.busylib.core.wrapper.wrap

class BUSYLibNetworkStateApiImpl : BUSYLibNetworkStateApi {
    private val isNetworkAvailableMutableFlow: MutableStateFlow<Boolean> = MutableStateFlow(false)

    suspend fun update(value: Boolean) {
        isNetworkAvailableMutableFlow.emit(value)
    }

    override val isNetworkAvailableFlow: WrappedStateFlow<Boolean>
        get() = isNetworkAvailableMutableFlow.wrap()
}
