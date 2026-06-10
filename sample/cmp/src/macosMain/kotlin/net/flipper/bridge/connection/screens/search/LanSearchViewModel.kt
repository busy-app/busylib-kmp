package net.flipper.bridge.connection.screens.search

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableStateFlow
import net.flipper.bridge.connection.config.api.FDevicePersistedStorage
import net.flipper.bridge.connection.service.api.FConnectionService
import net.flipper.busylib.core.wrapper.wrap
import net.flipper.core.busylib.log.LogTagProvider

class LanSearchViewModel(
    persistedStorage: FDevicePersistedStorage,
    deviceService: FConnectionService
) : ConnectionSearchViewModel(persistedStorage, deviceService), LogTagProvider {
    override val TAG = "LanSearchViewModel"

    override fun getDevicesFlow() =
        MutableStateFlow<ImmutableList<ConnectionSearchItem>>(persistentListOf()).wrap()
}
