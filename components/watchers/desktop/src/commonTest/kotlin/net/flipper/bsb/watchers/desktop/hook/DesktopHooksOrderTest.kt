package net.flipper.bsb.watchers.desktop.hook

import net.flipper.bridge.connection.config.api.model.BUSYBar
import net.flipper.bridge.connection.config.api.model.addTransport
import net.flipper.bridge.connection.config.internal.InternalStorageTransactionScope
import net.flipper.bridge.connection.config.internal.TransactionHook
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.uuid.Uuid

class DesktopHooksOrderTest {

    private val allHooks: List<TransactionHook> = listOf(
        DesktopEmptyFiller(),
        DesktopAlwaysLan(),
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
        cloud = BUSYBar.ConnectionWay.Cloud(Uuid.parse("00000000-0000-0000-0000-000000000002"))
    ).addTransport(lan = BUSYBar.ConnectionWay.Lan())

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

    private val bleDevice = BUSYBar(
        humanReadableName = "BLE Device",
        uniqueId = "ble-1",
        ble = BUSYBar.ConnectionWay.BLE("AA:BB:CC:DD:EE:FF")
    )

    // ---- Empty storage ----

    @Test
    fun GIVEN_empty_storage_WHEN_all_hook_permutations_run_THEN_results_are_identical() {
        assertAllPermutationsProduceSameResult(
            devices = emptyList(),
            currentDevice = null
        )
    }

    // ---- Single cloud device (no LAN) ----

    @Test
    fun GIVEN_single_cloud_device_without_LAN_WHEN_all_hook_permutations_run_THEN_results_are_identical() {
        assertAllPermutationsProduceSameResult(
            devices = listOf(cloudDevice),
            currentDevice = null
        )
    }

    @Test
    fun GIVEN_single_cloud_device_without_LAN_selected_WHEN_all_hook_permutations_run_THEN_results_are_identical() {
        assertAllPermutationsProduceSameResult(
            devices = listOf(cloudDevice),
            currentDevice = cloudDevice
        )
    }

    // ---- Single LAN-only device ----

    @Test
    fun GIVEN_single_LAN_device_WHEN_all_hook_permutations_run_THEN_results_are_identical() {
        assertAllPermutationsProduceSameResult(
            devices = listOf(lanDevice1),
            currentDevice = null
        )
    }

    @Test
    fun GIVEN_single_LAN_device_selected_WHEN_all_hook_permutations_run_THEN_results_are_identical() {
        assertAllPermutationsProduceSameResult(
            devices = listOf(lanDevice1),
            currentDevice = lanDevice1
        )
    }

    // ---- Cloud + LAN-only device ----

    @Test
    fun GIVEN_cloud_and_LAN_devices_with_no_selection_WHEN_all_hook_permutations_run_THEN_results_are_identical() {
        assertAllPermutationsProduceSameResult(
            devices = listOf(cloudDevice, lanDevice1),
            currentDevice = null
        )
    }

    @Test
    fun GIVEN_cloud_and_LAN_devices_with_LAN_selected_WHEN_all_hook_permutations_run_THEN_results_are_identical() {
        assertAllPermutationsProduceSameResult(
            devices = listOf(cloudDevice, lanDevice1),
            currentDevice = lanDevice1
        )
    }

    @Test
    fun GIVEN_cloud_and_LAN_devices_with_cloud_selected_WHEN_all_hook_permutations_run_THEN_results_are_identical() {
        assertAllPermutationsProduceSameResult(
            devices = listOf(cloudDevice, lanDevice1),
            currentDevice = cloudDevice
        )
    }

    // ---- Two LAN-only devices ----

    @Test
    fun GIVEN_two_LAN_devices_with_no_selection_WHEN_all_hook_permutations_run_THEN_results_are_identical() {
        assertAllPermutationsProduceSameResult(
            devices = listOf(lanDevice1, lanDevice2),
            currentDevice = null
        )
    }

    @Test
    fun GIVEN_two_LAN_devices_with_first_selected_WHEN_all_hook_permutations_run_THEN_results_are_identical() {
        assertAllPermutationsProduceSameResult(
            devices = listOf(lanDevice1, lanDevice2),
            currentDevice = lanDevice1
        )
    }

    @Test
    fun GIVEN_two_LAN_devices_with_second_selected_WHEN_all_hook_permutations_run_THEN_results_are_identical() {
        assertAllPermutationsProduceSameResult(
            devices = listOf(lanDevice1, lanDevice2),
            currentDevice = lanDevice2
        )
    }

    // ---- Cloud + two LAN-only devices ----

    @Test
    fun GIVEN_cloud_and_two_LAN_devices_WHEN_all_hook_permutations_run_THEN_results_are_identical() {
        assertAllPermutationsProduceSameResult(
            devices = listOf(cloudDevice, lanDevice1, lanDevice2),
            currentDevice = null
        )
    }

    @Test
    fun GIVEN_cloud_and_two_LAN_devices_with_LAN_selected_WHEN_all_hook_permutations_run_THEN_results_are_identical() {
        assertAllPermutationsProduceSameResult(
            devices = listOf(cloudDevice, lanDevice1, lanDevice2),
            currentDevice = lanDevice1
        )
    }

    // ---- Cloud+LAN device already complete ----

    @Test
    fun GIVEN_cloud_with_LAN_device_selected_WHEN_all_hook_permutations_run_THEN_results_are_identical() {
        assertAllPermutationsProduceSameResult(
            devices = listOf(cloudWithLanDevice),
            currentDevice = cloudWithLanDevice
        )
    }

    // ---- BLE device (gets LAN added) ----

    @Test
    fun GIVEN_BLE_device_WHEN_all_hook_permutations_run_THEN_results_are_identical() {
        assertAllPermutationsProduceSameResult(
            devices = listOf(bleDevice),
            currentDevice = null
        )
    }

    @Test
    fun GIVEN_BLE_and_cloud_devices_WHEN_all_hook_permutations_run_THEN_results_are_identical() {
        assertAllPermutationsProduceSameResult(
            devices = listOf(bleDevice, cloudDevice),
            currentDevice = null
        )
    }

    @Test
    fun GIVEN_BLE_and_LAN_devices_WHEN_all_hook_permutations_run_THEN_results_are_identical() {
        assertAllPermutationsProduceSameResult(
            devices = listOf(bleDevice, lanDevice1),
            currentDevice = null
        )
    }

    // ---- Mixed: cloud + BLE + LAN-only ----

    @Test
    fun GIVEN_cloud_BLE_and_LAN_devices_WHEN_all_hook_permutations_run_THEN_results_are_identical() {
        assertAllPermutationsProduceSameResult(
            devices = listOf(cloudDevice, bleDevice, lanDevice1),
            currentDevice = null
        )
    }

    @Test
    fun GIVEN_cloud_BLE_and_LAN_devices_with_LAN_selected_WHEN_all_hook_permutations_run_THEN_results_are_identical() {
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
        assertEquals(6, permutations.size, "Expected 6 permutations of 3 hooks")

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
    ) : InternalStorageTransactionScope {

        override fun getCurrentDevice(): BUSYBar? =
            devices.find { it.uniqueId == currentDeviceId }

        override fun getAllDevices(): List<BUSYBar> = devices.toList()

        override fun setCurrentDevice(device: BUSYBar) {
            if (devices.none { it.uniqueId == device.uniqueId }) {
                addOrReplace(device)
            }
            currentDeviceId = device.uniqueId
        }

        override fun setCurrentDeviceNullable(device: BUSYBar?) {
            if (device == null) {
                currentDeviceId = null
                return
            }
            setCurrentDevice(device)
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
