package com.flipperdevices.core.network

import net.flipper.busylib.core.wrapper.WrappedStateFlow

interface BUSYLibNetworkStateApi {
    val isNetworkAvailableFlow: WrappedStateFlow<Boolean>
}
