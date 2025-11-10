package com.flipperdevices.bridge.connection.screens.search

import com.flipperdevices.bridge.connection.config.api.FDevicePersistedStorage
import com.flipperdevices.bridge.connection.config.api.model.FDeviceBaseModel
import com.flipperdevices.core.busylib.log.LogTagProvider
import com.flipperdevices.core.busylib.log.info
import com.flipperdevices.core.busylib.log.warn
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import platform.AccessorySetupKit.*
import platform.CoreBluetooth.CBUUID
import platform.Foundation.NSUUID
import platform.UIKit.UIImage
import platform.darwin.dispatch_get_main_queue

class IOSSearchViewModel(
    private val persistedStorage: FDevicePersistedStorage
) : ConnectionSearchViewModel(persistedStorage), LogTagProvider {
    override val TAG = "iOSSearchViewModel"

    private val mockDevice = ConnectionSearchItem(
        address = "busy_bar_mock",
        deviceModel = FDeviceBaseModel.FDeviceBSBModelMock(humanReadableName = "BUSY Bar Mock"),
        isAdded = false,
    )

    private val searchItems =
        MutableStateFlow<ImmutableList<ConnectionSearchItem>>(persistentListOf(mockDevice))

    override fun getDevicesFlow() = searchItems.asStateFlow()

    private val session = ASAccessorySession()

    @OptIn(ExperimentalForeignApi::class)
    private fun supportedPickerDisplayItem(): Any {
        val descriptor = ASDiscoveryDescriptor()

        descriptor.bluetoothServiceUUID = CBUUID.UUIDWithString("0000308A-0000-1000-8000-00805F9B34FB") as objcnames.classes.CBUUID
        descriptor.bluetoothCompanyIdentifier = 0x0E29u

        // Create a default system image as placeholder
        val productImage = UIImage()

        // Use ASPickerDisplayItem.init(name:productImage:descriptor:)
        val item = platform.AccessorySetupKit.ASPickerDisplayItem.init(
            name = "BUSY Bar",
            productImage = productImage,
            descriptor = descriptor
        )

        item.setupOptions = 1u // .rename in Swift
        info { "Created ASPickerDisplayItem: name='BUSY Bar', descriptor=${descriptor}" }
        return item
    }

    @OptIn(ExperimentalForeignApi::class)
    private val supportedPickerDisplayItems: List<Any> = listOf(supportedPickerDisplayItem())

    init {
        session.activateWithQueue(dispatch_get_main_queue()) { event ->
            handleSessionEvent(event)
        }

        // Show picker with the busy item
        session.showPickerForDisplayItems(supportedPickerDisplayItems) { error ->
            warn { "Error showing picker: $error" }
        }
    }


    private fun handleSessionEvent(event: ASAccessoryEvent?) {
        if (event == null) {
            warn { "Received null event from ASAccessorySession" }
            return
        }

        info { "Event $event" }
        when (event.eventType) {
            // .activated
            10L -> {
                info { "Acccessory ${session.accessories}" }
                for (accessory in session.accessories) {
                    saveAccessory(accessory as ASAccessory?)
                }
            }
            // .accessoryAdded
            30L -> {
                saveAccessory(event.accessory)
            }
            // accessoryRemoved
            31L -> {
                removeAccessory(event.accessory?.bluetoothIdentifier)
            }
            // .accessoryChanged
            32L -> {
                saveAccessory(event.accessory)
            }
            else -> {
                warn { "Received unknown event type ${event.eventType}" }
            }
        }
    }

    private fun saveAccessory(accessory: ASAccessory?) {
        if (accessory == null) {
            warn { "Received null accessory from ASAccessorySession" }
            return
        }

        val name = accessory.displayName
        val id = accessory.bluetoothIdentifier ?: run {
            warn { "Accessory ${accessory.displayName} has null bluetoothIdentifier" }
            return
        }

        val list = searchItems.value.toMutableList()
        val isAdded = list.any { it.deviceModel.uniqueId == id.UUIDString() }

        val newItem = ConnectionSearchItem(
            address = id.UUIDString(), // No address for iOS accessories
            deviceModel = FDeviceBaseModel.FDeviceBSBModelBLEiOS(
                uuid = id.UUIDString(),
                humanReadableName = name
            ),
            isAdded = isAdded
        )
        list.add(newItem)

        viewModelScope.launch {
            searchItems.emit(list.toImmutableList())
        }
    }

    private fun removeAccessory(id: NSUUID?) {
        if (id == null) {
            warn { "Received null accessory id from ASAccessorySession" }
            return
        }

        val list = searchItems.value.toMutableList()
        val iterator = list.iterator()
        while (iterator.hasNext()) {
            val item = iterator.next()
            if (item.deviceModel.uniqueId == id.UUIDString()) {
                iterator.remove()
            }
        }

        viewModelScope.launch {
            searchItems.emit(list.toImmutableList())
        }
    }
}
