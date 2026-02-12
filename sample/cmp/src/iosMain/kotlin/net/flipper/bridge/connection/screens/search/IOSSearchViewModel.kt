package net.flipper.bridge.connection.screens.search

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.flipper.bridge.connection.config.api.FDevicePersistedStorage
import net.flipper.bridge.connection.config.api.model.FDeviceCombined
import net.flipper.bridge.connection.service.api.FConnectionService
import net.flipper.busylib.core.wrapper.wrap
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.info
import net.flipper.core.busylib.log.warn
import platform.AccessorySetupKit.ASAccessory
import platform.AccessorySetupKit.ASAccessoryEvent
import platform.AccessorySetupKit.ASAccessorySession
import platform.AccessorySetupKit.ASDiscoveryDescriptor
import platform.AccessorySetupKit.ASPickerDisplayItem
import platform.CoreBluetooth.CBUUID
import platform.Foundation.NSUUID
import platform.UIKit.UIImage
import platform.darwin.dispatch_get_main_queue
import kotlin.collections.toMutableMap

class IOSSearchViewModel(
    persistedStorage: FDevicePersistedStorage,
    private val connectionService: FConnectionService,
) : ConnectionSearchViewModel(persistedStorage), LogTagProvider {
    override val TAG = "iOSSearchViewModel"

    private val mockDevice = ConnectionSearchItem(
        address = "busy_bar_mock",
        deviceModel = FDeviceCombined(
            humanReadableName = "BUSY Bar Mock",
            models = listOf(FDeviceCombined.DeviceModel.FDeviceBSBModelMock)
        ),
        isAdded = false,
    )

    // Map to store ConnectionSearchItem with ASAccessory by UUID
    private val searchItems = MutableStateFlow<ImmutableMap<String, ASAccessory>>(persistentMapOf())

    private val devicesFlow = MutableStateFlow<ImmutableList<ConnectionSearchItem>>(
        persistentListOf(mockDevice)
    )

    override fun getDevicesFlow() = devicesFlow.asStateFlow().wrap()

    private val session = ASAccessorySession()

    @OptIn(ExperimentalForeignApi::class)
    override fun onDeviceClick(searchItem: ConnectionSearchItem) {
        viewModelScope.launch {
            if (searchItem.isAdded) {
                // Find accessory by UUID from searchItems map and remove it
                val accessory = searchItems.value[searchItem.address]

                if (accessory != null) {
                    session.removeAccessory(accessory) {}
                } else {
                    warn { "Accessory with UUID ${searchItem.address} not found in map" }
                }

                persistedStorage.removeDevice(searchItem.deviceModel.uniqueId)
            } else {
                persistedStorage.addDevice(searchItem.deviceModel)
            }
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun supportedPickerDisplayItem(): ASPickerDisplayItem {
        val descriptor = ASDiscoveryDescriptor()
        descriptor.bluetoothServiceUUID = CBUUID.UUIDWithString("308A") as objcnames.classes.CBUUID
        descriptor.bluetoothCompanyIdentifier = 3625u // 0E29
        descriptor.supportedOptions = 2uL // .bluetoothPairingLE

        val productImage = UIImage.imageNamed("BusyBarDevice") ?: run {
            warn { "Failed to load system image, using empty UIImage" }
            UIImage()
        }

        val item = ASPickerDisplayItem(
            name = "BUSY Bar",
            productImage = productImage,
            descriptor = descriptor
        )
        return item
    }

    @OptIn(ExperimentalForeignApi::class)
    private val supportedPickerDisplayItems: List<Any> = listOf(supportedPickerDisplayItem())

    init {
        combine(
            searchItems,
            persistedStorage.getAllDevices()
        ) { accessoriesMap, savedDevices ->
            val existedUuids = savedDevices
                .filter { device -> device.models.any { it is FDeviceCombined.DeviceModel.FDeviceBSBModelBLEiOS } }
                .associateBy { it.uniqueId }

            accessoriesMap.map { (uuid, accessory) ->
                ConnectionSearchItem(
                    address = uuid,
                    deviceModel = existedUuids[uuid] ?: FDeviceCombined(
                        uniqueId = uuid,
                        humanReadableName = accessory.displayName,
                        models = listOf(FDeviceCombined.DeviceModel.FDeviceBSBModelBLEiOS(address = uuid))
                    ),
                    isAdded = existedUuids.containsKey(uuid)
                )
            }
        }.onEach { items ->
            devicesFlow.emit(
                (listOf(mockDevice) + items).toImmutableList()
            )
        }.launchIn(viewModelScope)

        session.activateWithQueue(dispatch_get_main_queue()) { event: platform.AccessorySetupKit.ASAccessoryEvent? ->
            handleSessionEvent(event)
        }

        session.showPickerForDisplayItems(supportedPickerDisplayItems) { error: platform.Foundation.NSError? ->
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
            }
            // .accessoryAdded
            30L -> {
                info { "Accessory added: ${event.accessory}" }
                saveAccessory(event.accessory)
            }
            // accessoryRemoved
            31L -> {
                info { "Accessory removed: ${event.accessory}" }
                removeAccessory(event.accessory?.bluetoothIdentifier)
            }
            // .accessoryChanged
            32L -> {
                info { "Accessory changed: ${event.accessory}" }
                // saveAccessory(event.accessory)
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

        val id = accessory.bluetoothIdentifier ?: run {
            warn { "Accessory ${accessory.displayName} has null bluetoothIdentifier" }
            return
        }

        val uuidString = id.UUIDString()

        // Store ASAccessory object in the map
        val updatedMap = searchItems.value.toMutableMap()
        updatedMap[uuidString] = accessory

        viewModelScope.launch {
            searchItems.emit(updatedMap.toImmutableMap())
        }
    }

    private fun removeAccessory(id: NSUUID?) {
        if (id == null) {
            warn { "Received null accessory id from ASAccessorySession" }
            return
        }

        val uuidString = id.UUIDString()
        info { "Removing accessory with UUID: $uuidString" }

        // Remove from map
        val updatedMap = searchItems.value.toMutableMap()
        info { "All devices before $uuidString removal: ${updatedMap.keys}" }
        updatedMap.remove(uuidString)
        info { "All devices after $uuidString removal: ${updatedMap.keys}" }

        runBlocking {
            info { "Emitting updated map without $uuidString" }
            searchItems.emit(updatedMap.toImmutableMap())
            info { "Emitted updated map without $uuidString" }
            val currentDevice = persistedStorage.getCurrentDevice().first()
            val isCurrentDevice = currentDevice?.uniqueId == uuidString
            val existingDevices = persistedStorage.getAllDevices().first()
            val deviceExists = existingDevices.any { it.uniqueId == uuidString }

            if (isCurrentDevice) {
                info { "Accessory is current device, stopping connection attempts" }
                connectionService.forgetCurrentDevice()
            } else if (deviceExists) {
                info { "Device found in storage, removing..." }
                persistedStorage.removeDevice(uuidString)
            } else {
                info { "Device not in storage, skipping removal" }
            }
        }
    }
}
