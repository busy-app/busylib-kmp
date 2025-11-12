package net.flipper.bridge.connection.screens.search

import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.launch
import net.flipper.bridge.connection.config.api.FDevicePersistedStorage
import net.flipper.bridge.connection.screens.decompose.DecomposeViewModel
import net.flipper.busylib.core.wrapper.WrappedStateFlow

abstract class ConnectionSearchViewModel(
    val persistedStorage: FDevicePersistedStorage
) : DecomposeViewModel() {
    abstract fun getDevicesFlow(): WrappedStateFlow<ImmutableList<ConnectionSearchItem>>
    open fun onDeviceClick(searchItem: ConnectionSearchItem) {
        viewModelScope.launch {
            if (searchItem.isAdded) {
                persistedStorage.removeDevice(searchItem.deviceModel.uniqueId)
            } else {
                persistedStorage.addDevice(searchItem.deviceModel)
            }
        }
    }
}
