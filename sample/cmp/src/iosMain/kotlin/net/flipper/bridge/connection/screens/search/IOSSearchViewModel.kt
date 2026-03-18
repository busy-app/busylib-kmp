package net.flipper.bridge.connection.screens.search

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import net.flipper.bridge.connection.config.api.FDevicePersistedStorage
import net.flipper.bridge.connection.config.api.model.BUSYBar
import net.flipper.bridge.connection.config.api.model.BUSYBar.ConnectionWay
import net.flipper.bridge.connection.transport.ble.impl.ios.central.DiscoveredBluetoothDevice
import net.flipper.bridge.connection.transport.ble.impl.ios.central.FCentralManagerApi
import net.flipper.busylib.core.wrapper.wrap
import net.flipper.core.busylib.log.LogTagProvider

class IOSSearchViewModel(
    persistedStorage: FDevicePersistedStorage,
    private val fCentralManagerApi: FCentralManagerApi,
) : ConnectionSearchViewModel(persistedStorage), LogTagProvider {
    override val TAG = "iOSSearchViewModel"

    private val mockDevice = ConnectionSearchItem(
        address = "busy_bar_mock",
        deviceModel = BUSYBar(
            humanReadableName = "BUSY Bar Mock",
            mock = BUSYBar.ConnectionWay.Mock
        ),
        isAdded = false,
    )

    private val devicesFlow = MutableStateFlow<ImmutableList<ConnectionSearchItem>>(
        persistentListOf(mockDevice)
    )

    override fun getDevicesFlow() = devicesFlow.asStateFlow().wrap()

    init {
        viewModelScope.launch {
            fCentralManagerApi.startScan()
        }

        combine(
            fCentralManagerApi.discoveredStream,
            persistedStorage.getAllDevicesFlow()
        ) { searchDevices, savedDevices ->
            val existedMacAddresses = savedDevices
                .mapNotNull { device ->
                    device.ble?.let { it.address to device }
                }.toMap()

            searchDevices.map { bleDevice: DiscoveredBluetoothDevice ->
                ConnectionSearchItem(
                    address = bleDevice.address,
                    deviceModel = existedMacAddresses[bleDevice.address]
                        ?: bleDevice.toFDeviceModel(),
                    isAdded = existedMacAddresses.containsKey(bleDevice.address)
                )
            }
        }.onEach { items ->
            devicesFlow.emit(
                (listOf(mockDevice) + items).toPersistentList()
            )
        }.launchIn(viewModelScope)
    }

    override fun onDestroy() {
        viewModelScope.launch {
            fCentralManagerApi.stopScan()
        }
        super.onDestroy()
    }
}

private fun DiscoveredBluetoothDevice.toFDeviceModel(): BUSYBar {
    return BUSYBar(
        humanReadableName = this.name ?: "BUSY Bar",
        ble = ConnectionWay.BLE(
            address = this.address
        )
    )
}
