package com.flipperdevices.core.network

import kotlinx.coroutines.flow.MutableStateFlow

class BUSYLibNetworkStateApiNoop(
    defaultState: Boolean = false
) : BUSYLibNetworkStateApi {
    override val isNetworkAvailableFlow = MutableStateFlow(defaultState)
}