package net.flipper.bridge.connection.config.impl.hooks

import net.flipper.bridge.connection.config.api.PersistedStorageTransactionScope
import net.flipper.bridge.connection.config.api.model.BUSYBar
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.uuid.Uuid

class AlwaysActiveHookTest {

    private val cloudDevice1 = BUSYBar(
        humanReadableName = "Cloud Device 1",
        uniqueId = "cloud-1",
        cloud = BUSYBar.ConnectionWay.Cloud(Uuid.random())
    )

    private val cloudDevice2 = BUSYBar(
        humanReadableName = "Cloud Device 2",
        uniqueId = "cloud-2",
        cloud = BUSYBar.ConnectionWay.Cloud(Uuid.random())
    )

    private val bleDevice1 = BUSYBar(
        humanReadableName = "BLE Device 1",
        uniqueId = "ble-1",
        ble = BUSYBar.ConnectionWay.BLE("AA:BB:CC:DD:EE:FF")
    )

    private val bleDevice2 = BUSYBar(
        humanReadableName = "BLE Device 2",
        uniqueId = "ble-2",
        ble = BUSYBar.ConnectionWay.BLE("11:22:33:44:55:66")
    )

    private val lanDevice = BUSYBar(
        humanReadableName = "LAN Device",
        uniqueId = "lan-1",
        lan = BUSYBar.ConnectionWay.Lan()
    )

    private val hook = AlwaysActiveHook()

    @Test
    fun doesNothingWhenCurrentDeviceExists() {
        val scope = FakeTransactionScope(
            selectedDevice = bleDevice1,
            devices = listOf(bleDevice1, bleDevice2, cloudDevice1, cloudDevice2, lanDevice)
        )
        with(hook) { scope.postTransaction() }
        assertEquals(bleDevice1, scope.getSelectedDevice())
    }

    @Test
    fun selectsFirstCloudDeviceWhenCurrentIsNull() {
        val scope = FakeTransactionScope(
            selectedDevice = null,
            devices = listOf(bleDevice1, bleDevice2, cloudDevice1, lanDevice, cloudDevice2)
        )
        with(hook) { scope.postTransaction() }
        assertEquals(cloudDevice1, scope.getSelectedDevice())
    }

    @Test
    fun selectsFirstNonCloudDeviceWhenNoCloudAvailable() {
        val scope = FakeTransactionScope(
            selectedDevice = null,
            devices = listOf(lanDevice, bleDevice1, bleDevice2)
        )
        with(hook) { scope.postTransaction() }
        assertEquals(lanDevice, scope.getSelectedDevice())
    }

    @Test
    fun doesNothingWhenNoDevicesAvailable() {
        val scope = FakeTransactionScope(
            selectedDevice = null,
            devices = emptyList()
        )
        with(hook) { scope.postTransaction() }
        assertNull(scope.getSelectedDevice())
    }

    @Test
    fun prefersCloudOverBleAndLan() {
        val scope = FakeTransactionScope(
            selectedDevice = null,
            devices = listOf(bleDevice1, lanDevice, bleDevice2, cloudDevice2, cloudDevice1)
        )
        with(hook) { scope.postTransaction() }
        assertEquals(cloudDevice2, scope.getSelectedDevice())
    }

    private class FakeTransactionScope(
        private var selectedDevice: BUSYBar?,
        private val devices: List<BUSYBar>
    ) : PersistedStorageTransactionScope {
        fun getSelectedDevice(): BUSYBar? = selectedDevice
        override fun getCurrentDevice(): BUSYBar? = selectedDevice
        override fun getAllDevices(): List<BUSYBar> = devices
        override fun setCurrentDevice(device: BUSYBar?) {
            selectedDevice = device
        }
        override fun addOrReplace(device: BUSYBar) = Unit
        override fun removeDevice(id: String) = Unit
    }
}
