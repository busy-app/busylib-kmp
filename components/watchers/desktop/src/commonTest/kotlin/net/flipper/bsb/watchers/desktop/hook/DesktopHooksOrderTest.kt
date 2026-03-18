package net.flipper.bsb.watchers.desktop.hook

import net.flipper.bridge.connection.config.api.PersistedStorageTransactionScope
import net.flipper.bridge.connection.config.api.TransactionHook
import net.flipper.bridge.connection.config.api.model.BUSYBar
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.uuid.Uuid

class DesktopHooksOrderTest {

    private val allHooks: List<TransactionHook> = listOf(
        DesktopEmptyFiller(),
        DesktopAlwaysLan(),
        DesktopActiveDevice(),
        DesktopAutoPurger()
    )

    private val cloudDevice = BUSYBar(
        humanReadableName = "Cloud Device",
        uniqueId = "cloud-1",
        cloud = BUSYBar.ConnectionWay.Cloud(Uuid.parse("00000000-0000-0000-0000-000000000001"))
    )

    private val cloudWithLanDevice = BUSYBar(
        humanReadableName = "Cloud+LAN Device",
        uniqueId = "cloud-lan-1",
        cloud = BUSYBar.ConnectionWay.Cloud(Uuid.parse("00000000-0000-0000-0000-000000000002")),
        lan = BUSYBar.ConnectionWay.Lan()
    )

    private val lanDevice1 = BUSYBar(
        humanReadableName = "LAN Device 1",
        uniqueId = "lan-1",
        lan = BUSYBar.ConnectionWay.Lan()
    )

    private val lanDevice2 = BUSYBar(
        humanReadableName = "LAN Device 2",
        uniqueId = "lan-2",
        lan = BUSYBar.ConnectionWay.Lan()
    )

    private val bareDevice1 = BUSYBar(
        humanReadableName = "Bare Device 1",
        uniqueId = "bare-1"
    )

    private val bareDevice2 = BUSYBar(
        humanReadableName = "Bare Device 2",
        uniqueId = "bare-2"
    )

    private val bleDevice = BUSYBar(
        humanReadableName = "BLE Device",
        uniqueId = "ble-1",
        ble = BUSYBar.ConnectionWay.BLE("AA:BB:CC:DD:EE:FF")
    )

    // ---- Empty storage ----

    @Test
    fun emptyStorage_allPermutations() {
        assertAllPermutationsProduceSameResult(
            devices = emptyList(),
            currentDevice = null
        )
    }

    // ---- Single cloud device (no LAN) ----

    @Test
    fun singleCloudDeviceNoLan_allPermutations() {
        assertAllPermutationsProduceSameResult(
            devices = listOf(cloudDevice),
            currentDevice = null
        )
    }

    @Test
    fun singleCloudDeviceNoLanSelected_allPermutations() {
        assertAllPermutationsProduceSameResult(
            devices = listOf(cloudDevice),
            currentDevice = cloudDevice
        )
    }

    // ---- Single LAN-only device ----

    @Test
    fun singleLanDevice_allPermutations() {
        assertAllPermutationsProduceSameResult(
            devices = listOf(lanDevice1),
            currentDevice = null
        )
    }

    @Test
    fun singleLanDeviceSelected_allPermutations() {
        assertAllPermutationsProduceSameResult(
            devices = listOf(lanDevice1),
            currentDevice = lanDevice1
        )
    }

    // ---- Cloud + LAN-only device ----

    @Test
    fun cloudAndLanDevice_noSelection_allPermutations() {
        assertAllPermutationsProduceSameResult(
            devices = listOf(cloudDevice, lanDevice1),
            currentDevice = null
        )
    }

    @Test
    fun cloudAndLanDevice_lanSelected_allPermutations() {
        assertAllPermutationsProduceSameResult(
            devices = listOf(cloudDevice, lanDevice1),
            currentDevice = lanDevice1
        )
    }

    @Test
    fun cloudAndLanDevice_cloudSelected_allPermutations() {
        assertAllPermutationsProduceSameResult(
            devices = listOf(cloudDevice, lanDevice1),
            currentDevice = cloudDevice
        )
    }

    // ---- Two LAN-only devices ----

    @Test
    fun twoLanDevices_noSelection_allPermutations() {
        assertAllPermutationsProduceSameResult(
            devices = listOf(lanDevice1, lanDevice2),
            currentDevice = null
        )
    }

    @Test
    fun twoLanDevices_firstSelected_allPermutations() {
        assertAllPermutationsProduceSameResult(
            devices = listOf(lanDevice1, lanDevice2),
            currentDevice = lanDevice1
        )
    }

    @Test
    fun twoLanDevices_secondSelected_allPermutations() {
        assertAllPermutationsProduceSameResult(
            devices = listOf(lanDevice1, lanDevice2),
            currentDevice = lanDevice2
        )
    }

    // ---- Cloud + two LAN-only devices ----

    @Test
    fun cloudAndTwoLanDevices_allPermutations() {
        assertAllPermutationsProduceSameResult(
            devices = listOf(cloudDevice, lanDevice1, lanDevice2),
            currentDevice = null
        )
    }

    @Test
    fun cloudAndTwoLanDevices_lanSelected_allPermutations() {
        assertAllPermutationsProduceSameResult(
            devices = listOf(cloudDevice, lanDevice1, lanDevice2),
            currentDevice = lanDevice1
        )
    }

    // ---- Bare device (no connections) ----

    @Test
    fun singleBareDevice_allPermutations() {
        assertAllPermutationsProduceSameResult(
            devices = listOf(bareDevice1),
            currentDevice = null
        )
    }

    @Test
    fun twoBareDevices_allPermutations() {
        assertAllPermutationsProduceSameResult(
            devices = listOf(bareDevice1, bareDevice2),
            currentDevice = null
        )
    }

    @Test
    fun cloudAndBareDevice_allPermutations() {
        assertAllPermutationsProduceSameResult(
            devices = listOf(cloudDevice, bareDevice1),
            currentDevice = null
        )
    }

    // ---- Cloud+LAN device already complete ----

    @Test
    fun cloudWithLanDevice_allPermutations() {
        assertAllPermutationsProduceSameResult(
            devices = listOf(cloudWithLanDevice),
            currentDevice = cloudWithLanDevice
        )
    }

    // ---- BLE device (gets LAN added) ----

    @Test
    fun bleDevice_allPermutations() {
        assertAllPermutationsProduceSameResult(
            devices = listOf(bleDevice),
            currentDevice = null
        )
    }

    @Test
    fun bleAndCloudDevice_allPermutations() {
        assertAllPermutationsProduceSameResult(
            devices = listOf(bleDevice, cloudDevice),
            currentDevice = null
        )
    }

    @Test
    fun bleAndLanDevice_allPermutations() {
        assertAllPermutationsProduceSameResult(
            devices = listOf(bleDevice, lanDevice1),
            currentDevice = null
        )
    }

    // ---- Mixed: cloud + BLE + LAN-only ----

    @Test
    fun cloudBleAndLanDevice_allPermutations() {
        assertAllPermutationsProduceSameResult(
            devices = listOf(cloudDevice, bleDevice, lanDevice1),
            currentDevice = null
        )
    }

    @Test
    fun cloudBleAndLanDevice_lanSelected_allPermutations() {
        assertAllPermutationsProduceSameResult(
            devices = listOf(cloudDevice, bleDevice, lanDevice1),
            currentDevice = lanDevice1
        )
    }

    // ---- Helpers ----

    private fun assertAllPermutationsProduceSameResult(
        devices: List<BUSYBar>,
        currentDevice: BUSYBar?
    ) {
        val permutations = allHooks.permutations()
        assertEquals(24, permutations.size, "Expected 24 permutations of 4 hooks")

        val results = permutations.map { orderedHooks ->
            val scope = FakeTransactionScope(
                devices = devices.toMutableList(),
                currentDeviceId = currentDevice?.uniqueId
            )
            orderedHooks.forEach { hook ->
                with(hook) { scope.postTransaction() }
            }
            scope.snapshot()
        }

        val first = results.first()
        results.forEachIndexed { index, result ->
            assertEquals(
                first,
                result,
                "Permutation #$index ${permutations[index].hookNames()} produced different result " +
                    "than permutation #0 ${permutations[0].hookNames()}.\n" +
                    "Expected: $first\n" +
                    "Actual:   $result"
            )
        }
    }

    private fun <T> List<T>.permutations(): List<List<T>> {
        if (size <= 1) return listOf(this)
        return flatMapIndexed { index, element ->
            val rest = toMutableList().apply { removeAt(index) }
            rest.permutations().map { listOf(element) + it }
        }
    }

    private fun List<TransactionHook>.hookNames(): String =
        joinToString(" -> ") { it::class.simpleName ?: "?" }

    /**
     * UUID-agnostic snapshot: compares device structure rather than exact IDs,
     * because [DesktopEmptyFiller] creates devices with random UUIDs.
     */
    private data class Snapshot(
        val deviceCount: Int,
        val deviceProperties: Set<DeviceProps>,
        val hasCurrentDevice: Boolean,
        val currentDeviceProps: DeviceProps?
    )

    private data class DeviceProps(
        val hasCloud: Boolean,
        val hasLan: Boolean,
        val hasBle: Boolean,
        val hasMock: Boolean
    )

    private fun BUSYBar.toProps() = DeviceProps(
        hasCloud = cloud != null,
        hasLan = lan != null,
        hasBle = ble != null,
        hasMock = mock != null
    )

    private fun FakeTransactionScope.snapshot(): Snapshot {
        val devices = getAllDevices()
        val current = getCurrentDevice()
        return Snapshot(
            deviceCount = devices.size,
            deviceProperties = devices.map { it.toProps() }.toSet(),
            hasCurrentDevice = current != null,
            currentDeviceProps = current?.toProps()
        )
    }

    /**
     * Mutable transaction scope matching the real [PersistedStorageTransactionScopeImpl] behavior:
     * - [addOrReplace] replaces by uniqueId
     * - [removeDevice] clears current selection when removing the current device
     * - [setCurrentDevice] auto-adds the device if not present
     */
    private class FakeTransactionScope(
        private val devices: MutableList<BUSYBar>,
        private var currentDeviceId: String?
    ) : PersistedStorageTransactionScope {

        override fun getCurrentDevice(): BUSYBar? =
            devices.find { it.uniqueId == currentDeviceId }

        override fun getAllDevices(): List<BUSYBar> = devices.toList()

        override fun setCurrentDevice(device: BUSYBar?) {
            if (device == null) {
                currentDeviceId = null
                return
            }
            if (devices.none { it.uniqueId == device.uniqueId }) {
                addOrReplace(device)
            }
            currentDeviceId = device.uniqueId
        }

        override fun addOrReplace(device: BUSYBar) {
            devices.removeAll { it.uniqueId == device.uniqueId }
            devices.add(device)
        }

        override fun removeDevice(id: String) {
            val removed = devices.removeAll { it.uniqueId == id }
            if (removed && id == currentDeviceId) {
                currentDeviceId = null
            }
        }
    }
}
