package com.flipperdevices.bridge.connection.screens.search

import com.flipperdevices.bridge.connection.config.api.FDevicePersistedStorage
import com.flipperdevices.core.busylib.log.LogTagProvider
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class USBSearchViewModel(
    private val persistedStorage: FDevicePersistedStorage
) : ConnectionSearchViewModel(persistedStorage), LogTagProvider {
    override val TAG = "USBSearchViewModel"

    private val searchItems =
        MutableStateFlow<ImmutableList<ConnectionSearchItem>>(persistentListOf())

    override fun getDevicesFlow() = searchItems.asStateFlow()
}
