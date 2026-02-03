package com.flipperdevices.core.network

import kotlinx.coroutines.flow.StateFlow

interface BUSYLibNetworkStateApi {
    val isNetworkAvailableFlow: StateFlow<Boolean>
}
