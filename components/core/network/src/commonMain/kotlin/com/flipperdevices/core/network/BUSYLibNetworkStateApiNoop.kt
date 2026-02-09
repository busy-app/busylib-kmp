package com.flipperdevices.core.network

import kotlinx.coroutines.flow.MutableStateFlow
import net.flipper.busylib.core.wrapper.wrap

class BUSYLibNetworkStateApiNoop(
    defaultState: Boolean = false
) : BUSYLibNetworkStateApi {
    override val isNetworkAvailableFlow = MutableStateFlow(defaultState).wrap()
}
