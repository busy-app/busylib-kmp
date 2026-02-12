package net.flipper.bridge.connection.screens.search

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import net.flipper.bridge.connection.config.api.FDevicePersistedStorage
import net.flipper.bridge.connection.config.api.model.FDeviceCombined
import net.flipper.bridge.connection.utils.Secrets
import net.flipper.busylib.core.wrapper.WrappedStateFlow
import net.flipper.busylib.core.wrapper.wrap
import net.flipper.core.busylib.log.LogTagProvider

class LanSearchViewModel(
    persistedStorage: FDevicePersistedStorage
) : ConnectionSearchViewModel(persistedStorage), LogTagProvider {
    override val TAG = "LanSearchViewModel"

    private val searchItems = combine(
        persistedStorage.getAllDevices(),
        flowOf(
            listOf(
                FDeviceCombined.FDeviceBSBModelLan(),
                FDeviceCombined.FDeviceBSBModelCloud(

                    authToken = Secrets.DEVICE_TOKEN,
                    host = "proxy.dev.busy.app",
                    deviceId = Secrets.DEVICE_ID
                ),
                FDeviceCombined.FDeviceBSBModelCombined(
                    uniqueId = "BUSY Bar Combined",
                    humanReadableName = "BUSY Bar Combined",
                    models = listOf(
                        FDeviceCombined.FDeviceBSBModelLan(),
                        FDeviceCombined.FDeviceBSBModelCloud(
                            authToken = Secrets.DEVICE_TOKEN,
                            host = "proxy.dev.busy.app",
                            deviceId = Secrets.DEVICE_ID
                        )
                    ),
                )
            )
        )
    ) { savedDevices, foundDevices ->
        foundDevices.map { device ->
            ConnectionSearchItem(
                address = device.humanReadableName,
                deviceModel = device,
                isAdded = savedDevices.find { it.uniqueId == device.uniqueId } != null
            )
        }.toPersistentList()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, persistentListOf())

    override fun getDevicesFlow(): WrappedStateFlow<ImmutableList<ConnectionSearchItem>> {
        return searchItems.wrap()
    }
}
