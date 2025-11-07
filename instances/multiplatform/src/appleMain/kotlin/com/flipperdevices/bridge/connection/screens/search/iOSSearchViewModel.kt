package com.flipperdevices.bridge.connection.screens.search

import com.flipperdevices.bridge.connection.config.api.FDevicePersistedStorage
import com.flipperdevices.bridge.connection.config.api.model.FDeviceBaseModel
import com.flipperdevices.busylib.core.di.BusyLibGraph
import com.flipperdevices.core.busylib.log.LogTagProvider
import com.flipperdevices.core.busylib.log.warn
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import platform.AccessorySetupKit.ASAccessory
import platform.AccessorySetupKit.ASAccessoryEvent
import platform.AccessorySetupKit.ASAccessorySession
import platform.Foundation.NSUUID
import platform.darwin.dispatch_get_main_queue

@Inject
@ContributesBinding(BusyLibGraph::class, binding<ConnectionSearchViewModel>())
class iOSSearchViewModel(
    private val persistedStorage: FDevicePersistedStorage
) : ConnectionSearchViewModel(persistedStorage), LogTagProvider {
    override val TAG = "iOSSearchViewModel"

    private val searchItems =
        MutableStateFlow<ImmutableList<ConnectionSearchItem>>(persistentListOf())

    override fun getDevicesFlow() = searchItems.asStateFlow()

    private val session = ASAccessorySession()

    init {
        session.activateWithQueue(dispatch_get_main_queue()) { event ->
            handleSessionEvent(event)
        }
    }

    private fun handleSessionEvent(event: ASAccessoryEvent?) {
        if (event == null) {
            warn { "Received null event from ASAccessorySession" }
            return
        }

        when (event.eventType) {
            // .activated
            10L -> {
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
