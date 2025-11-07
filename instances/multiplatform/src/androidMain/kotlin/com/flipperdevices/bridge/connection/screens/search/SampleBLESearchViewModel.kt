package com.flipperdevices.bridge.connection.screens.search

import android.annotation.SuppressLint
import com.flipperdevices.bridge.api.scanner.DiscoveredBluetoothDevice
import com.flipperdevices.bridge.api.scanner.FlipperScanner
import com.flipperdevices.bridge.api.utils.Constants.UNKNOWN_NAME
import com.flipperdevices.bridge.connection.config.api.FDevicePersistedStorage
import com.flipperdevices.bridge.connection.config.api.model.FDeviceBaseModel
import com.flipperdevices.core.di.AppGraph
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@Inject
@SuppressLint("MissingPermission")
@ContributesBinding(AppGraph::class, binding<ConnectionSearchViewModel>())
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
                .filterIsInstance<FDeviceBaseModel.FDeviceBSBModelBLE>()
                .associateBy { it.address }
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

    override fun getDevicesFlow() = devicesFlow.asStateFlow()
}
private fun DiscoveredBluetoothDevice.toFDeviceModel(): FDeviceBaseModel {
    val id = address

    return when (this) {
        is DiscoveredBluetoothDevice.MockDiscoveredBluetoothDevice -> FDeviceBaseModel.FDeviceBSBModelMock(
            uniqueId = id,
            humanReadableName = name ?: UNKNOWN_NAME
        )

        is DiscoveredBluetoothDevice.RealDiscoveredBluetoothDevice -> FDeviceBaseModel.FDeviceBSBModelBLE(
            address = device.address,
            uniqueId = id,
            humanReadableName = device.name ?: UNKNOWN_NAME
        )
    }
}
