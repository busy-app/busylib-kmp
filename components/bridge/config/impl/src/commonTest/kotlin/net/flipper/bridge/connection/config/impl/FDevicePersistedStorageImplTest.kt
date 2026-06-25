package net.flipper.bridge.connection.config.impl

import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.ExperimentalSettingsImplementation
import com.russhwolf.settings.MapSettings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import net.flipper.bridge.connection.config.api.model.BUSYBar
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalSettingsApi::class, ExperimentalSettingsImplementation::class)
class FDevicePersistedStorageImplTest {

    private fun bleDevice(name: String, id: String) = BUSYBar(
        humanReadableName = name,
        uniqueId = id,
        ble = BUSYBar.ConnectionWay.BLE(address = id)
    )

    private fun createStorage() = FDevicePersistedStorageImpl(MapSettings())

    @Test
    fun returnsDevicesSortedAlphabeticallyRegardlessOfInsertionOrder() = runTest {
        val storage = createStorage()
        storage.transaction {
            addOrReplace(bleDevice(name = "Charlie", id = "id-c"))
            addOrReplace(bleDevice(name = "Alice", id = "id-a"))
            addOrReplace(bleDevice(name = "Bob", id = "id-b"))
        }

        val names = storage.getAllDevicesFlow().first().map { it.humanReadableName }

        assertEquals(listOf("Alice", "Bob", "Charlie"), names)
    }

    @Test
    fun sortsCaseInsensitively() = runTest {
        val storage = createStorage()
        storage.transaction {
            addOrReplace(bleDevice(name = "banana", id = "id-1"))
            addOrReplace(bleDevice(name = "Apple", id = "id-2"))
            addOrReplace(bleDevice(name = "cherry", id = "id-3"))
        }

        val names = storage.getAllDevicesFlow().first().map { it.humanReadableName }

        assertEquals(listOf("Apple", "banana", "cherry"), names)
    }

    @Test
    fun reSortsAfterAdditionalDeviceIsAddedInSeparateTransaction() = runTest {
        val storage = createStorage()
        storage.transaction {
            addOrReplace(bleDevice(name = "Alice", id = "id-a"))
            addOrReplace(bleDevice(name = "Charlie", id = "id-c"))
        }

        storage.transaction {
            addOrReplace(bleDevice(name = "Bob", id = "id-b"))
        }

        val names = storage.getAllDevicesFlow().first().map { it.humanReadableName }

        assertEquals(listOf("Alice", "Bob", "Charlie"), names)
    }
}
