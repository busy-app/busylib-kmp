package net.flipper.bridge.connection.screens.search

import android.annotation.SuppressLint
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import net.flipper.bridge.api.scanner.DiscoveredBluetoothDevice
import net.flipper.bridge.api.scanner.FlipperScanner
import net.flipper.bridge.api.utils.Constants.UNKNOWN_NAME
import net.flipper.bridge.connection.config.api.FDevicePersistedStorage
import net.flipper.bridge.connection.config.api.model.BUSYBar
import net.flipper.busylib.core.wrapper.WrappedStateFlow
import net.flipper.busylib.core.wrapper.wrap

@SuppressLint("MissingPermission")
class SampleBLESearchViewModel(
    persistedStorage: FDevicePersistedStorage,
    flipperScanner: FlipperScanner
) : ConnectionSearchViewModel(persistedStorage) {
    private val mockDevice = ConnectionSearchItem(
        address = DiscoveredBluetoothDevice.MockDiscoveredBluetoothDevice.address,
        deviceModel = DiscoveredBluetoothDevice.MockDiscoveredBluetoothDevice.toFDeviceModel(),
        isAdded = false,
    )

    private val devicesFlow = MutableStateFlow<PersistentList<ConnectionSearchItem>>(
        persistentListOf(mockDevice)
    )

    init {
        combine(
            flipperScanner
                .findFlipperDevices(),
            persistedStorage.getAllDevices()
        ) { searchDevices, savedDevices ->
            val existedMacAddresses = savedDevices
                .flatMap { device ->
                    device.models
                        .filterIsInstance<BUSYBar.ConnectionWay.BLE>()
                        .map { it.address to device }
                }.toMap()
            searchDevices.map { bleDevice ->
                ConnectionSearchItem(
                    address = bleDevice.address,
                    deviceModel = existedMacAddresses[bleDevice.address]
                        ?: bleDevice.toFDeviceModel(),
                    isAdded = existedMacAddresses.containsKey(bleDevice.address)
                )
            }
        }.onEach {
            devicesFlow.emit(
                (listOf(mockDevice) + it).toPersistentList()
            )
        }.launchIn(viewModelScope)
    }

    override fun getDevicesFlow(): WrappedStateFlow<ImmutableList<ConnectionSearchItem>> =
        devicesFlow.asStateFlow().wrap()
}
private fun DiscoveredBluetoothDevice.toFDeviceModel(): BUSYBar {
    val id = address

    return when (this) {
        is DiscoveredBluetoothDevice.MockDiscoveredBluetoothDevice -> BUSYBar(
            uniqueId = id,
            humanReadableName = name ?: UNKNOWN_NAME,
            models = listOf(BUSYBar.ConnectionWay.Mock)
        )

        is DiscoveredBluetoothDevice.RealDiscoveredBluetoothDevice -> BUSYBar(
            uniqueId = id,
            humanReadableName = device.name ?: UNKNOWN_NAME,
            models = listOf(BUSYBar.ConnectionWay.BLE(address = device.address))
        )
    }
}
