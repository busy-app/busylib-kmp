package net.flipper.bridge.lanmonitor.impl

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.job
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import net.flipper.bridge.connection.config.api.PersistedStorageTransactionScope
import net.flipper.bridge.connection.config.api.model.BUSYBar
import net.flipper.bridge.connection.config.internal.FInternalDevicePersistedStorage
import net.flipper.bridge.connection.config.internal.InternalStorageTransactionScope
import net.flipper.bridge.connection.config.internal.TransactionHook
import net.flipper.bridge.lanmonitor.impl.platform.LanAvailablePlatformListener
import net.flipper.bridge.lanmonitor.impl.utils.DeviceMetaInfoRequester
import net.flipper.bridge.lanmonitor.model.ConnectedDeviceMetaInfo
import net.flipper.busylib.core.wrapper.WrappedFlow
import net.flipper.eventbus.internal.BusyLibEventPublisher
import net.flipper.eventbus.model.BusyLibEvent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.uuid.Uuid

/**
 * Behavioural tests for [LanMonitorApiImpl]: it observes LAN availability, resolves the reachable
 * device's hardware id, and reconciles persisted storage by auto-switching the active device,
 * publishing an [BusyLibEvent.ActiveDeviceAutoSwitched] when (and only when) the active device changes.
 */
class LanMonitorApiImplTest {

    @Test
    fun GIVEN_unknown_hardware_id_WHEN_lan_available_THEN_adds_new_lan_device_and_publishes_event() = runTest {
        val storage = FakeStorage()
        val listener = FakeLanAvailableListener()
        val requester = FakeMetaInfoRequester(listOf(success("hw-new")))
        val publisher = RecordingEventPublisher(storage)
        val api = lanMonitor(listener, requester, storage, publisher)

        api.onLaunch()
        listener.flow.value = true
        advanceUntilIdle()

        val device = storage.snapshotDevices().single()
        assertEquals("hw-new", device.hardwareId)
        assertNotNull(device.lan, "New device must be reachable over LAN")
        assertEquals(device.uniqueId, storage.snapshotCurrentId(), "New device must become the active device")

        val event = assertIs<BusyLibEvent.ActiveDeviceAutoSwitched>(publisher.events.single())
        assertEquals("hw-new", event.newDevice.hardwareId)
    }

    @Test
    fun GIVEN_matching_device_not_current_WHEN_plugged_in_THEN_adds_lan_switches_and_event_carries_lan_device() =
        runTest {
            val target = cloudDevice(uniqueId = "dev-1", hardwareId = "hw-1")
            val other = cloudDevice(uniqueId = "dev-2", hardwareId = "hw-2")
            val storage = FakeStorage(initialDevices = listOf(target, other), initialCurrentId = "dev-2")
            val listener = FakeLanAvailableListener()
            val requester = FakeMetaInfoRequester(listOf(success("hw-1")))
            val publisher = RecordingEventPublisher(storage)
            val api = lanMonitor(listener, requester, storage, publisher)

            api.onLaunch()
            listener.flow.value = true
            advanceUntilIdle()

            val stored = storage.snapshotDevices().single { it.uniqueId == "dev-1" }
            assertNotNull(stored.lan, "LAN transport must be added to the matched device")
            assertNotNull(stored.cloud, "Existing transports must be preserved")
            assertEquals("dev-1", storage.snapshotCurrentId(), "Active device must switch to the matched device")

            // Regression: the event must carry the LAN-augmented device, not the pre-update one.
            val event = assertIs<BusyLibEvent.ActiveDeviceAutoSwitched>(publisher.events.single())
            assertEquals("dev-1", event.newDevice.uniqueId)
            assertNotNull(event.newDevice.lan, "Published device must include the freshly added LAN transport")
        }

    @Test
    fun GIVEN_matching_device_already_current_WHEN_plugged_in_THEN_ensures_lan_but_publishes_no_event() = runTest {
        val target = cloudDevice(uniqueId = "dev-1", hardwareId = "hw-1")
        val storage = FakeStorage(initialDevices = listOf(target), initialCurrentId = "dev-1")
        val listener = FakeLanAvailableListener()
        val requester = FakeMetaInfoRequester(listOf(success("hw-1")))
        val publisher = RecordingEventPublisher(storage)
        val api = lanMonitor(listener, requester, storage, publisher)

        api.onLaunch()
        listener.flow.value = true
        advanceUntilIdle()

        val stored = storage.snapshotDevices().single()
        assertNotNull(stored.lan, "LAN transport must still be ensured for the already-current device")
        assertEquals("dev-1", storage.snapshotCurrentId())
        assertTrue(publisher.events.isEmpty(), "No switch happened, so no event must be published")
    }

    @Test
    fun GIVEN_event_published_WHEN_auto_switch_happens_THEN_publish_runs_outside_storage_transaction() = runTest {
        val storage = FakeStorage()
        val listener = FakeLanAvailableListener()
        val requester = FakeMetaInfoRequester(listOf(success("hw-new")))
        val publisher = RecordingEventPublisher(storage)
        val api = lanMonitor(listener, requester, storage, publisher)

        api.onLaunch()
        listener.flow.value = true
        advanceUntilIdle()

        assertEquals(1, publisher.events.size)
        assertFalse(
            publisher.publishedDuringTransaction,
            "Event must be published after the storage transaction commits, never while holding the storage lock"
        )
    }

    @Test
    fun GIVEN_lan_unavailable_WHEN_launched_THEN_no_request_no_storage_change_and_flow_is_null() = runTest {
        val storage = FakeStorage()
        val listener = FakeLanAvailableListener()
        val requester = FakeMetaInfoRequester(listOf(success("hw-x")))
        val publisher = RecordingEventPublisher(storage)
        val api = lanMonitor(listener, requester, storage, publisher)

        api.onLaunch()
        advanceUntilIdle()

        assertEquals(0, requester.callCount, "Hardware id must not be requested while LAN is unavailable")
        assertTrue(storage.snapshotDevices().isEmpty())
        assertTrue(publisher.events.isEmpty())
        assertNull(api.getConnectedDeviceFlow().value)
    }

    @Test
    fun GIVEN_lan_toggles_WHEN_observing_flow_THEN_reflects_meta_info_then_null() = runTest {
        val storage = FakeStorage()
        val listener = FakeLanAvailableListener()
        val requester = FakeMetaInfoRequester(listOf(success("hw-9")))
        val publisher = RecordingEventPublisher(storage)
        val api = lanMonitor(listener, requester, storage, publisher)

        listener.flow.value = true
        advanceUntilIdle()
        assertEquals(ConnectedDeviceMetaInfo("hw-9"), api.getConnectedDeviceFlow().value)

        listener.flow.value = false
        advanceUntilIdle()
        assertNull(api.getConnectedDeviceFlow().value, "Losing LAN availability must clear the connected device")
    }

    @Test
    fun GIVEN_meta_info_fails_twice_WHEN_lan_available_THEN_retries_until_success() = runTest {
        val storage = FakeStorage()
        val listener = FakeLanAvailableListener()
        val requester = FakeMetaInfoRequester(
            listOf(
                failure(),
                failure(),
                success("hw-retry"),
            )
        )
        val publisher = RecordingEventPublisher(storage)
        val api = lanMonitor(listener, requester, storage, publisher)

        listener.flow.value = true
        advanceUntilIdle()

        assertEquals(ConnectedDeviceMetaInfo("hw-retry"), api.getConnectedDeviceFlow().value)
        assertEquals(3, requester.callCount, "Must keep retrying the request until it succeeds")
    }

    // ---- Helpers ----

    private fun TestScope.lanMonitor(
        listener: LanAvailablePlatformListener,
        requester: DeviceMetaInfoRequester,
        storage: FInternalDevicePersistedStorage,
        publisher: BusyLibEventPublisher,
    ): LanMonitorApiImpl = LanMonitorApiImpl(
        lanAvailableListener = listener,
        globalScope = CoroutineScope(
            backgroundScope.coroutineContext.job + UnconfinedTestDispatcher(testScheduler)
        ),
        infoRequester = requester,
        storageApi = storage,
        eventApi = publisher,
    )

    private fun success(hardwareId: String): Result<ConnectedDeviceMetaInfo> =
        Result.success(ConnectedDeviceMetaInfo(hardwareId))

    private fun failure(): Result<ConnectedDeviceMetaInfo> =
        Result.failure(IllegalStateException("request failed"))

    private fun cloudDevice(uniqueId: String, hardwareId: String): BUSYBar = BUSYBar(
        humanReadableName = "Device $uniqueId",
        hardwareId = hardwareId,
        uniqueId = uniqueId,
        cloud = BUSYBar.ConnectionWay.Cloud(Uuid.random())
    )

    // ---- Fakes ----

    private class FakeLanAvailableListener(
        val flow: MutableStateFlow<Boolean> = MutableStateFlow(false)
    ) : LanAvailablePlatformListener {
        override fun getLanAvailableFlow(): Flow<Boolean> = flow
    }

    private class FakeMetaInfoRequester(
        private val responses: List<Result<ConnectedDeviceMetaInfo>>
    ) : DeviceMetaInfoRequester {
        var callCount: Int = 0
            private set

        override suspend fun getMetaInfo(): Result<ConnectedDeviceMetaInfo> {
            val index = callCount.coerceAtMost(responses.lastIndex)
            callCount++
            return responses[index]
        }
    }

    private class RecordingEventPublisher(
        private val storage: FakeStorage
    ) : BusyLibEventPublisher {
        val events: MutableList<BusyLibEvent> = mutableListOf()
        var publishedDuringTransaction: Boolean = false
            private set

        override suspend fun publish(event: BusyLibEvent) {
            if (storage.transactionActive) {
                publishedDuringTransaction = true
            }
            events.add(event)
        }
    }

    /**
     * In-memory [FInternalDevicePersistedStorage] mirroring the real transaction semantics
     * (copy-on-commit under a single lock) so tests can assert the resulting device state and
     * detect work that wrongly runs while the storage lock is held.
     */
    private class FakeStorage(
        initialDevices: List<BUSYBar> = emptyList(),
        initialCurrentId: String? = null,
    ) : FInternalDevicePersistedStorage {
        private val mutex = Mutex()
        private var devices: List<BUSYBar> = initialDevices.toList()
        private var currentId: String? = initialCurrentId

        var transactionActive: Boolean = false
            private set

        fun snapshotDevices(): List<BUSYBar> = devices
        fun snapshotCurrentId(): String? = currentId

        override suspend fun <T> transactionInternal(
            block: suspend InternalStorageTransactionScope.() -> T
        ): T = mutex.withLock {
            transactionActive = true
            try {
                val scope = FakeTransactionScope(devices, currentId)
                val result = block(scope)
                devices = scope.snapshotDevices()
                currentId = scope.snapshotCurrentId()
                result
            } finally {
                transactionActive = false
            }
        }

        override suspend fun <T> transaction(
            block: suspend PersistedStorageTransactionScope.() -> T
        ): T = transactionInternal { block() }

        override suspend fun addHook(vararg hook: TransactionHook) = Unit
        override fun getCurrentDeviceFlow(): WrappedFlow<BUSYBar?> = error("not used in test")
        override fun getAllDevicesFlow(): WrappedFlow<List<BUSYBar>> = error("not used in test")
    }

    private class FakeTransactionScope(
        devices: List<BUSYBar>,
        currentId: String?,
    ) : InternalStorageTransactionScope {
        private val devices: MutableList<BUSYBar> = devices.toMutableList()
        private var currentId: String? = currentId

        fun snapshotDevices(): List<BUSYBar> = devices.toList()
        fun snapshotCurrentId(): String? = currentId

        override fun getCurrentDevice(): BUSYBar? = devices.find { it.uniqueId == currentId }
        override fun getAllDevices(): List<BUSYBar> = devices.toList()

        override fun setCurrentDevice(device: BUSYBar) = setCurrentDeviceNullable(device)

        override fun setCurrentDeviceNullable(device: BUSYBar?) {
            if (device == null) {
                currentId = null
                return
            }
            if (devices.none { it.uniqueId == device.uniqueId }) {
                addOrReplace(device)
            }
            currentId = device.uniqueId
        }

        override fun addOrReplace(device: BUSYBar) {
            devices.removeAll { it.uniqueId == device.uniqueId }
            devices.add(device)
        }

        override fun removeDevice(id: String) {
            val removed = devices.removeAll { it.uniqueId == id }
            if (removed && id == currentId) {
                currentId = null
            }
        }
    }
}
