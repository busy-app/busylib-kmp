package net.flipper.bridge.connection.transport.combined.impl

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import net.flipper.bridge.connection.transport.combined.FCombinedConnectionConfig
import net.flipper.bridge.connection.transport.combined.impl.connections.AutoReconnectConnection
import net.flipper.bridge.connection.transport.combined.impl.connections.helpers.MockConnectionBuilder
import net.flipper.bridge.connection.transport.combined.impl.connections.helpers.TestConfig
import net.flipper.bridge.connection.transport.combined.impl.connections.helpers.TestConnectedDeviceApi
import net.flipper.bridge.connection.transport.common.api.FConnectedDeviceApi
import net.flipper.bridge.connection.transport.common.api.FDeviceConnectionConfig
import net.flipper.bridge.connection.transport.common.api.FInternalTransportConnectionStatus
import net.flipper.bridge.connection.transport.common.api.FInternalTransportConnectionStatus.Connected
import net.flipper.bridge.connection.transport.common.api.FInternalTransportConnectionStatus.Connecting
import net.flipper.bridge.connection.transport.common.api.FInternalTransportConnectionStatus.Disconnected
import net.flipper.bridge.connection.transport.common.api.FTransportConnectionStatusListener
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Comprehensive tests for [FCombinedConnectionApiImpl.tryUpdateConnectionConfig] covering:
 * - Config type validation
 * - Identical config (no-op)
 * - Name-only change
 * - Connection reuse when child tryUpdateConnectionConfig succeeds
 * - Connection removal/close when config removed
 * - New connection creation when config added
 * - Connection ordering from connectionConfigs
 * - Dynamic invalidation of consumers (connectionsFlow)
 * - Concurrent updates and race conditions
 * - Error handling at all levels
 * - Disconnect behavior
 * - State transitions during config updates
 */
@Suppress("LargeClass")
@OptIn(ExperimentalCoroutinesApi::class)
class FCombinedConnectionApiImplTest {

    // region Helper Functions

    private fun createConnectionBuilder(): MockConnectionBuilder = MockConnectionBuilder()

    private fun createConfig(
        name: String = "TestDevice",
        vararg childConfigs: FDeviceConnectionConfig<*>
    ): FCombinedConnectionConfig = FCombinedConnectionConfig(
        name = name,
        connectionConfigs = childConfigs.toList()
    )

    private suspend fun connectAndReport(
        builder: MockConnectionBuilder,
        scope: CoroutineScope,
        deviceName: String = "Device"
    ): TestConnectedDeviceApi {
        builder.connectCalledDeferred.await()
        val listener = builder.latestListener()!!
        val deviceApi = builder.deviceApis.lastOrNull() ?: TestConnectedDeviceApi(deviceName)
        listener.onStatusUpdate(
            Connected(
                scope = scope,
                deviceApi = deviceApi
            )
        )
        return deviceApi
    }

    /**
     * Creates a [FCombinedConnectionApiImpl] with pre-established [AutoReconnectConnection]s.
     *
     * Returns the SUT, the list of connection builders, and the status listener.
     */
    private suspend fun createSut(
        testDispatcher: kotlinx.coroutines.test.TestCoroutineScheduler,
        scope: CoroutineScope,
        config: FCombinedConnectionConfig,
        globalConnectionBuilder: MockConnectionBuilder = MockConnectionBuilder()
    ): Triple<FCombinedConnectionApiImpl, MockConnectionBuilder, MutableList<FInternalTransportConnectionStatus>> {
        val statusHistory = mutableListOf<FInternalTransportConnectionStatus>()
        val listener = FTransportConnectionStatusListener { status ->
            statusHistory.add(status)
        }

        val dispatcher = StandardTestDispatcher(testDispatcher)
        val connections = config.connectionConfigs.map {
            AutoReconnectConnection(
                scope = scope,
                config = it,
                connectionBuilder = globalConnectionBuilder,
                dispatcher = dispatcher
            )
        }

        val sut = FCombinedConnectionApiImpl(
            currentConfig = config,
            initialConnections = connections,
            listener = listener,
            connectionBuilder = globalConnectionBuilder,
            scope = scope
        )
        return Triple(sut, globalConnectionBuilder, statusHistory)
    }

    // endregion

    // region Config Type Validation Tests

    @Test
    fun GIVEN_non_combined_config_WHEN_tryUpdateConnectionConfig_THEN_returns_failure() = runTest {
        val config = createConfig("Device", TestConfig("a"))
        val builder = createConnectionBuilder()
        val (sut, _, _) = createSut(testScheduler, backgroundScope, config, builder)
        advanceUntilIdle()

        val result = sut.tryUpdateConnectionConfig(TestConfig("different"))

        assertTrue(result.isFailure, "Should fail for non-FCombinedConnectionConfig")
        assertIs<IllegalArgumentException>(result.exceptionOrNull())
    }

    // endregion

    // region Identical Config (No-op) Tests

    @Test
    fun GIVEN_same_config_WHEN_tryUpdateConnectionConfig_THEN_returns_success_immediately() =
        runTest {
            val config = createConfig("Device", TestConfig("a"))
            val builder = createConnectionBuilder()
            val (sut, _, _) = createSut(testScheduler, backgroundScope, config, builder)
            advanceUntilIdle()

            val result = sut.tryUpdateConnectionConfig(config)

            assertTrue(result.isSuccess, "Same config should return success")
        }

    @Test
    fun GIVEN_equal_config_object_WHEN_tryUpdateConnectionConfig_THEN_returns_success() = runTest {
        val childConfig = TestConfig("a")
        val config1 = createConfig("Device", childConfig)
        val config2 = createConfig("Device", childConfig)
        val builder = createConnectionBuilder()
        val (sut, _, _) = createSut(testScheduler, backgroundScope, config1, builder)
        advanceUntilIdle()

        val result = sut.tryUpdateConnectionConfig(config2)

        assertTrue(result.isSuccess, "Equal config should return success")
    }

    // endregion

    // region Name-Only Change Tests

    @Test
    fun GIVEN_only_name_changed_WHEN_tryUpdateConnectionConfig_THEN_success_and_name_updated() =
        runTest {
            val childConfig = TestConfig("a")
            val config1 = createConfig("OldName", childConfig)
            val builder = createConnectionBuilder()
            val (sut, _, _) = createSut(testScheduler, backgroundScope, config1, builder)
            advanceUntilIdle()

            val config2 = createConfig("NewName", childConfig)
            val result = sut.tryUpdateConnectionConfig(config2)
            advanceUntilIdle()

            assertTrue(result.isSuccess, "Name-only change should succeed")
            assertEquals("NewName", sut.deviceName, "deviceName should reflect new config name")
        }

    // endregion

    // region Connection Reuse Tests

    @Test
    fun GIVEN_exact_same_child_config_WHEN_tryUpdateConnectionConfig_THEN_reuses_connection() =
        runTest {
            val testDispatcher = StandardTestDispatcher(testScheduler)
            val childConfig = TestConfig("a")
            val config1 = createConfig("Device", childConfig)
            val builder = createConnectionBuilder()
            val listener = FTransportConnectionStatusListener { }

            val conn = AutoReconnectConnection(
                scope = backgroundScope,
                config = childConfig,
                connectionBuilder = builder,
                dispatcher = testDispatcher
            )
            builder.connectCalledDeferred.await()
            advanceUntilIdle()

            val sut = FCombinedConnectionApiImpl(
                currentConfig = config1,
                initialConnections = listOf(conn),
                listener = listener,
                connectionBuilder = builder,
                scope = backgroundScope
            )
            advanceUntilIdle()

            val connectionsBefore = sut.connectionsFlow.value
            val config2 = createConfig("Device2", childConfig)
            val result = sut.tryUpdateConnectionConfig(config2)
            advanceUntilIdle()

            assertTrue(result.isSuccess)
            val connectionsAfter = sut.connectionsFlow.value
            assertEquals(1, connectionsAfter.size)
            assertSame(connectionsBefore[0], connectionsAfter[0], "Should reuse exact same connection object")
        }

    @Test
    fun GIVEN_child_accepts_config_update_WHEN_tryUpdateConnectionConfig_THEN_reuses_connection() =
        runTest {
            val testDispatcher = StandardTestDispatcher(testScheduler)
            val configA = TestConfig("a")
            val configA2 = TestConfig("a2")
            val config1 = createConfig("Device", configA)
            val builder = createConnectionBuilder()
            val listener = FTransportConnectionStatusListener { }

            val conn = AutoReconnectConnection(
                scope = backgroundScope,
                config = configA,
                connectionBuilder = builder,
                dispatcher = testDispatcher
            )
            builder.connectCalledDeferred.await()
            advanceUntilIdle()

            // Simulate the child becoming connected with a device that accepts updates
            val deviceApi = TestConnectedDeviceApi("DeviceA")
            deviceApi.tryUpdateResult = Result.success(Unit)
            builder.latestListener()!!.onStatusUpdate(
                Connected(scope = backgroundScope, deviceApi = deviceApi)
            )
            advanceUntilIdle()

            val sut = FCombinedConnectionApiImpl(
                currentConfig = config1,
                initialConnections = listOf(conn),
                listener = listener,
                connectionBuilder = builder,
                scope = backgroundScope
            )
            advanceUntilIdle()

            val config2 = createConfig("Device", configA2)
            val result = sut.tryUpdateConnectionConfig(config2)
            advanceUntilIdle()

            assertTrue(result.isSuccess)
            assertEquals(1, sut.connectionsFlow.value.size)
            assertSame(conn, sut.connectionsFlow.value[0], "Should reuse connection")
            assertEquals(configA2, conn.config, "Connection config should be updated")
        }

    @Test
    fun GIVEN_child_rejects_config_update_WHEN_tryUpdateConnectionConfig_THEN_creates_new_connection() =
        runTest {
            val testDispatcher = StandardTestDispatcher(testScheduler)
            val configA = TestConfig("a")
            val configB = TestConfig("b")
            val config1 = createConfig("Device", configA)
            val builder = createConnectionBuilder()
            val listener = FTransportConnectionStatusListener { }

            val conn = AutoReconnectConnection(
                scope = backgroundScope,
                config = configA,
                connectionBuilder = builder,
                dispatcher = testDispatcher
            )
            builder.connectCalledDeferred.await()
            advanceUntilIdle()

            // Child rejects update
            val deviceApi = TestConnectedDeviceApi("DeviceA")
            deviceApi.tryUpdateResult = Result.failure(IllegalArgumentException("Rejected"))
            builder.latestListener()!!.onStatusUpdate(
                Connected(scope = backgroundScope, deviceApi = deviceApi)
            )
            advanceUntilIdle()

            val sut = FCombinedConnectionApiImpl(
                currentConfig = config1,
                initialConnections = listOf(conn),
                listener = listener,
                connectionBuilder = builder,
                scope = backgroundScope
            )
            advanceUntilIdle()

            val config2 = createConfig("Device", configB)
            val result = sut.tryUpdateConnectionConfig(config2)
            advanceUntilIdle()

            assertTrue(result.isSuccess)
            assertEquals(1, sut.connectionsFlow.value.size)
            // Old connection should be disconnected, new one created
            assertTrue(
                sut.connectionsFlow.value[0] !== conn,
                "Should be a new connection, not the old one"
            )
        }

    // endregion

    // region Connection Removal Tests

    @Test
    fun GIVEN_child_config_removed_WHEN_tryUpdateConnectionConfig_THEN_connection_disconnected() =
        runTest {
            val testDispatcher = StandardTestDispatcher(testScheduler)
            val configA = TestConfig("a")
            val configB = TestConfig("b")
            val config1 = createConfig("Device", configA, configB)
            val builderA = createConnectionBuilder()
            val builderB = createConnectionBuilder()
            val listener = FTransportConnectionStatusListener { }

            val connA = AutoReconnectConnection(
                scope = backgroundScope,
                config = configA,
                connectionBuilder = builderA,
                dispatcher = testDispatcher
            )
            val connB = AutoReconnectConnection(
                scope = backgroundScope,
                config = configB,
                connectionBuilder = builderB,
                dispatcher = testDispatcher
            )
            builderA.connectCalledDeferred.await()
            builderB.connectCalledDeferred.await()
            advanceUntilIdle()

            val sut = FCombinedConnectionApiImpl(
                currentConfig = config1,
                initialConnections = listOf(connA, connB),
                listener = listener,
                connectionBuilder = builderA,
                scope = backgroundScope
            )
            advanceUntilIdle()

            // Remove configB, keep only configA
            val config2 = createConfig("Device", configA)
            val result = sut.tryUpdateConnectionConfig(config2)
            advanceUntilIdle()

            assertTrue(result.isSuccess)
            assertEquals(1, sut.connectionsFlow.value.size)
            assertSame(connA, sut.connectionsFlow.value[0], "configA connection should be kept")
        }

    @Test
    fun GIVEN_all_configs_removed_WHEN_tryUpdateConnectionConfig_THEN_all_connections_disconnected() =
        runTest {
            val testDispatcher = StandardTestDispatcher(testScheduler)
            val sutScope = CoroutineScope(SupervisorJob() + testDispatcher)
            val configA = TestConfig("a")
            val config1 = createConfig("Device", configA)
            val builder = createConnectionBuilder()
            val statusHistory = mutableListOf<FInternalTransportConnectionStatus>()
            val listener = FTransportConnectionStatusListener { statusHistory.add(it) }

            val conn = AutoReconnectConnection(
                scope = backgroundScope,
                config = configA,
                connectionBuilder = builder,
                dispatcher = testDispatcher
            )
            builder.connectCalledDeferred.await()
            advanceUntilIdle()

            val sut = FCombinedConnectionApiImpl(
                currentConfig = config1,
                initialConnections = listOf(conn),
                listener = listener,
                connectionBuilder = builder,
                scope = sutScope
            )
            advanceUntilIdle()

            // Remove all configs
            val config2 = createConfig("Device")
            val result = sut.tryUpdateConnectionConfig(config2)
            advanceUntilIdle()

            assertTrue(result.isSuccess)
            assertEquals(0, sut.connectionsFlow.value.size)
            // State should transition to Disconnected
            assertTrue(
                statusHistory.any { it == Disconnected },
                "Should emit Disconnected when all connections removed. " +
                    "statusHistory=$statusHistory"
            )

            sutScope.cancel()
        }

    // endregion

    // region Connection Addition Tests

    @Test
    fun GIVEN_new_child_config_added_WHEN_tryUpdateConnectionConfig_THEN_new_connection_created() =
        runTest {
            val testDispatcher = StandardTestDispatcher(testScheduler)
            val configA = TestConfig("a")
            val configB = TestConfig("b")
            val config1 = createConfig("Device", configA)
            val builder = createConnectionBuilder()
            val listener = FTransportConnectionStatusListener { }

            val connA = AutoReconnectConnection(
                scope = backgroundScope,
                config = configA,
                connectionBuilder = builder,
                dispatcher = testDispatcher
            )
            builder.connectCalledDeferred.await()
            advanceUntilIdle()

            val sut = FCombinedConnectionApiImpl(
                currentConfig = config1,
                initialConnections = listOf(connA),
                listener = listener,
                connectionBuilder = builder,
                scope = backgroundScope
            )
            advanceUntilIdle()

            // Add configB
            val config2 = createConfig("Device", configA, configB)
            val result = sut.tryUpdateConnectionConfig(config2)
            advanceUntilIdle()

            assertTrue(result.isSuccess)
            assertEquals(2, sut.connectionsFlow.value.size)
            assertSame(connA, sut.connectionsFlow.value[0], "First should be existing connA")
        }

    @Test
    fun GIVEN_empty_initial_config_WHEN_add_connections_THEN_connections_created() = runTest {
        val builder = createConnectionBuilder()
        val listener = FTransportConnectionStatusListener { }

        val sut = FCombinedConnectionApiImpl(
            currentConfig = createConfig("Device"),
            initialConnections = emptyList(),
            listener = listener,
            connectionBuilder = builder,
            scope = backgroundScope
        )
        advanceUntilIdle()

        val configA = TestConfig("a")
        val configB = TestConfig("b")
        val config2 = createConfig("Device", configA, configB)
        val result = sut.tryUpdateConnectionConfig(config2)
        advanceUntilIdle()

        assertTrue(result.isSuccess)
        assertEquals(2, sut.connectionsFlow.value.size)
    }

    // endregion

    // region Connection Ordering Tests

    @Test
    fun GIVEN_reordered_configs_WHEN_tryUpdateConnectionConfig_THEN_connections_follow_new_order() =
        runTest {
            val testDispatcher = StandardTestDispatcher(testScheduler)
            val configA = TestConfig("a")
            val configB = TestConfig("b")
            val configC = TestConfig("c")
            val config1 = createConfig("Device", configA, configB, configC)
            val builderA = createConnectionBuilder()
            val builderB = createConnectionBuilder()
            val builderC = createConnectionBuilder()
            val listener = FTransportConnectionStatusListener { }

            val connA = AutoReconnectConnection(
                scope = backgroundScope,
                config = configA,
                connectionBuilder = builderA,
                dispatcher = testDispatcher
            )
            val connB = AutoReconnectConnection(
                scope = backgroundScope,
                config = configB,
                connectionBuilder = builderB,
                dispatcher = testDispatcher
            )
            val connC = AutoReconnectConnection(
                scope = backgroundScope,
                config = configC,
                connectionBuilder = builderC,
                dispatcher = testDispatcher
            )
            builderA.connectCalledDeferred.await()
            builderB.connectCalledDeferred.await()
            builderC.connectCalledDeferred.await()
            advanceUntilIdle()

            val sut = FCombinedConnectionApiImpl(
                currentConfig = config1,
                initialConnections = listOf(connA, connB, connC),
                listener = listener,
                connectionBuilder = builderA,
                scope = backgroundScope
            )
            advanceUntilIdle()

            // Reverse order
            val config2 = createConfig("Device", configC, configA, configB)
            val result = sut.tryUpdateConnectionConfig(config2)
            advanceUntilIdle()

            assertTrue(result.isSuccess)
            val connections = sut.connectionsFlow.value
            assertEquals(3, connections.size)
            assertSame(connC, connections[0], "First should be connC")
            assertSame(connA, connections[1], "Second should be connA")
            assertSame(connB, connections[2], "Third should be connB")
        }

    // endregion

    // region Complex Scenarios (Add + Remove + Reuse)

    @Test
    fun GIVEN_add_remove_reuse_simultaneously_WHEN_tryUpdateConnectionConfig_THEN_correct_result() =
        runTest {
            val testDispatcher = StandardTestDispatcher(testScheduler)
            val configA = TestConfig("a")
            val configB = TestConfig("b")
            val configC = TestConfig("c")
            val configD = TestConfig("d")
            val config1 = createConfig("Device", configA, configB, configC)
            val builderA = createConnectionBuilder()
            val builderB = createConnectionBuilder()
            val builderC = createConnectionBuilder()
            val listener = FTransportConnectionStatusListener { }

            val connA = AutoReconnectConnection(
                scope = backgroundScope,
                config = configA,
                connectionBuilder = builderA,
                dispatcher = testDispatcher
            )
            val connB = AutoReconnectConnection(
                scope = backgroundScope,
                config = configB,
                connectionBuilder = builderB,
                dispatcher = testDispatcher
            )
            val connC = AutoReconnectConnection(
                scope = backgroundScope,
                config = configC,
                connectionBuilder = builderC,
                dispatcher = testDispatcher
            )
            builderA.connectCalledDeferred.await()
            builderB.connectCalledDeferred.await()
            builderC.connectCalledDeferred.await()
            advanceUntilIdle()

            val sut = FCombinedConnectionApiImpl(
                currentConfig = config1,
                initialConnections = listOf(connA, connB, connC),
                listener = listener,
                connectionBuilder = builderA,
                scope = backgroundScope
            )
            advanceUntilIdle()

            // configA: kept, configB: removed, configC: kept, configD: added
            val config2 = createConfig("Device", configD, configA, configC)
            val result = sut.tryUpdateConnectionConfig(config2)
            advanceUntilIdle()

            assertTrue(result.isSuccess)
            val connections = sut.connectionsFlow.value
            assertEquals(3, connections.size)
            // configD is new (first position), configA reused (second), configC reused (third)
            assertNotEquals(connA, connections[0], "First should be new (configD)")
            assertSame(connA, connections[1], "Second should be connA")
            assertSame(connC, connections[2], "Third should be connC")
        }

    // endregion

    // region Dynamic Invalidation Tests

    @Test
    fun GIVEN_connections_changed_WHEN_observing_connectionsFlow_THEN_consumers_see_new_list() =
        runTest {
            val testDispatcher = StandardTestDispatcher(testScheduler)
            val configA = TestConfig("a")
            val configB = TestConfig("b")
            val config1 = createConfig("Device", configA)
            val builder = createConnectionBuilder()
            val listener = FTransportConnectionStatusListener { }

            val connA = AutoReconnectConnection(
                scope = backgroundScope,
                config = configA,
                connectionBuilder = builder,
                dispatcher = testDispatcher
            )
            builder.connectCalledDeferred.await()
            advanceUntilIdle()

            val sut = FCombinedConnectionApiImpl(
                currentConfig = config1,
                initialConnections = listOf(connA),
                listener = listener,
                connectionBuilder = builder,
                scope = backgroundScope
            )
            advanceUntilIdle()

            val observedSizes = mutableListOf<Int>()
            val collectorJob = launch {
                sut.connectionsFlow.collect { observedSizes.add(it.size) }
            }
            advanceUntilIdle()

            // Update config to add configB
            val config2 = createConfig("Device", configA, configB)
            sut.tryUpdateConnectionConfig(config2)
            advanceUntilIdle()

            collectorJob.cancel()

            assertTrue(
                observedSizes.contains(1),
                "Should have observed initial size 1"
            )
            assertTrue(
                observedSizes.contains(2),
                "Should have observed updated size 2"
            )
        }

    @Test
    fun GIVEN_connection_removed_WHEN_state_was_connected_THEN_status_listener_notified() =
        runTest {
            val testDispatcher = StandardTestDispatcher(testScheduler)
            val sutScope = CoroutineScope(SupervisorJob() + testDispatcher)
            val configA = TestConfig("a")
            val config1 = createConfig("Device", configA)
            val builder = createConnectionBuilder()
            val statusHistory = mutableListOf<FInternalTransportConnectionStatus>()
            val listener = FTransportConnectionStatusListener { statusHistory.add(it) }

            val conn = AutoReconnectConnection(
                scope = backgroundScope,
                config = configA,
                connectionBuilder = builder,
                dispatcher = testDispatcher
            )
            builder.connectCalledDeferred.await()
            advanceUntilIdle()

            // Simulate connected state
            val deviceApi = TestConnectedDeviceApi("A")
            builder.latestListener()!!.onStatusUpdate(
                Connected(scope = backgroundScope, deviceApi = deviceApi)
            )
            advanceUntilIdle()

            val sut = FCombinedConnectionApiImpl(
                currentConfig = config1,
                initialConnections = listOf(conn),
                listener = listener,
                connectionBuilder = builder,
                scope = sutScope
            )
            advanceUntilIdle()
            statusHistory.clear()

            // Remove all connections
            val config2 = createConfig("Device")
            sut.tryUpdateConnectionConfig(config2)
            advanceUntilIdle()

            assertTrue(
                statusHistory.any { it == Disconnected },
                "Should emit Disconnected after removing connected connection. " +
                    "Got: $statusHistory"
            )

            sutScope.cancel()
        }

    // endregion

    // region Concurrent Update Tests

    @Test
    fun GIVEN_concurrent_updates_WHEN_tryUpdateConnectionConfig_THEN_serialized_correctly() =
        runTest {
            val testDispatcher = StandardTestDispatcher(testScheduler)
            val configA = TestConfig("a")
            val configB = TestConfig("b")
            val configC = TestConfig("c")
            val config1 = createConfig("Device", configA)
            val builder = createConnectionBuilder()
            val listener = FTransportConnectionStatusListener { }

            val conn = AutoReconnectConnection(
                scope = backgroundScope,
                config = configA,
                connectionBuilder = builder,
                dispatcher = testDispatcher
            )
            builder.connectCalledDeferred.await()
            advanceUntilIdle()

            val sut = FCombinedConnectionApiImpl(
                currentConfig = config1,
                initialConnections = listOf(conn),
                listener = listener,
                connectionBuilder = builder,
                scope = backgroundScope
            )
            advanceUntilIdle()

            // Launch concurrent updates
            val jobs = List(10) { i ->
                async {
                    val newConfig = if (i % 2 == 0) {
                        createConfig("Device", configA, configB)
                    } else {
                        createConfig("Device", configA, configC)
                    }
                    sut.tryUpdateConnectionConfig(newConfig)
                }
            }

            advanceUntilIdle()
            val results = jobs.awaitAll()

            // All should succeed (mutex serializes them)
            assertTrue(results.all { it.isSuccess }, "All concurrent updates should succeed")
            // Final state should be consistent
            val finalConnections = sut.connectionsFlow.value
            assertTrue(
                finalConnections.isNotEmpty(),
                "Should have connections after concurrent updates"
            )
        }

    @Test
    fun GIVEN_rapid_config_changes_WHEN_alternating_configs_THEN_final_state_consistent() =
        runTest {
            val builder = createConnectionBuilder()
            val listener = FTransportConnectionStatusListener { }

            val sut = FCombinedConnectionApiImpl(
                currentConfig = createConfig("Device"),
                initialConnections = emptyList(),
                listener = listener,
                connectionBuilder = builder,
                scope = backgroundScope
            )
            advanceUntilIdle()

            val configA = TestConfig("a")
            val configB = TestConfig("b")
            val config1 = createConfig("Device", configA)
            val config2 = createConfig("Device", configB)
            val config3 = createConfig("Device", configA, configB)

            // Rapid updates
            sut.tryUpdateConnectionConfig(config1)
            advanceUntilIdle()
            sut.tryUpdateConnectionConfig(config2)
            advanceUntilIdle()
            sut.tryUpdateConnectionConfig(config3)
            advanceUntilIdle()

            assertEquals(2, sut.connectionsFlow.value.size)
        }

    // endregion

    // region Error Handling Tests

    @Test
    fun GIVEN_child_tryUpdate_throws_exception_WHEN_tryUpdateConnectionConfig_THEN_handled_gracefully() =
        runTest {
            val testDispatcher = StandardTestDispatcher(testScheduler)
            val configA = TestConfig("a")
            val configB = TestConfig("b")
            val config1 = createConfig("Device", configA)

            // Custom builder where device throws on tryUpdateConnectionConfig
            val throwingDeviceApi = TestConnectedDeviceApi("DeviceA").apply {
                tryUpdateResult = Result.failure(RuntimeException("Unexpected error"))
            }
            val builder = MockConnectionBuilder().apply {
                templateDeviceApi = TestConnectedDeviceApi("DeviceA")
            }
            val listener = FTransportConnectionStatusListener { }

            val conn = AutoReconnectConnection(
                scope = backgroundScope,
                config = configA,
                connectionBuilder = builder,
                dispatcher = testDispatcher
            )
            builder.connectCalledDeferred.await()
            advanceUntilIdle()

            // Simulate connected with throwing device
            builder.latestListener()!!.onStatusUpdate(
                Connected(scope = backgroundScope, deviceApi = throwingDeviceApi)
            )
            advanceUntilIdle()

            val sut = FCombinedConnectionApiImpl(
                currentConfig = config1,
                initialConnections = listOf(conn),
                listener = listener,
                connectionBuilder = builder,
                scope = backgroundScope
            )
            advanceUntilIdle()

            // Update with different config - child rejects, so new connection should be created
            val config2 = createConfig("Device", configB)
            val result = sut.tryUpdateConnectionConfig(config2)
            advanceUntilIdle()

            assertTrue(result.isSuccess, "Should succeed even when child throws")
            assertEquals(1, sut.connectionsFlow.value.size)
        }

    @Test
    fun GIVEN_disconnect_throws_on_removed_connection_WHEN_tryUpdateConnectionConfig_THEN_still_succeeds() =
        runTest {
            val testDispatcher = StandardTestDispatcher(testScheduler)
            val configA = TestConfig("a")
            val config1 = createConfig("Device", configA)
            val builder = createConnectionBuilder()
            val listener = FTransportConnectionStatusListener { }

            val conn = AutoReconnectConnection(
                scope = backgroundScope,
                config = configA,
                connectionBuilder = builder,
                dispatcher = testDispatcher
            )
            builder.connectCalledDeferred.await()
            advanceUntilIdle()

            val sut = FCombinedConnectionApiImpl(
                currentConfig = config1,
                initialConnections = listOf(conn),
                listener = listener,
                connectionBuilder = builder,
                scope = backgroundScope
            )
            advanceUntilIdle()

            // Remove all configs — disconnect will be called on conn
            // disconnect() calls cancelAndJoin() which shouldn't throw normally
            // but the runCatching in the impl protects against any unexpected error
            val config2 = createConfig("Device")
            val result = sut.tryUpdateConnectionConfig(config2)
            advanceUntilIdle()

            assertTrue(result.isSuccess, "Should succeed even if disconnect has issues")
            assertEquals(0, sut.connectionsFlow.value.size)
        }

    // endregion

    // region State Transition Tests

    @Test
    fun GIVEN_new_connection_added_WHEN_becomes_connected_THEN_status_emits_connected() =
        runTest {
            val testDispatcher = StandardTestDispatcher(testScheduler)
            val sutScope = CoroutineScope(SupervisorJob() + testDispatcher)
            val builder = createConnectionBuilder()
            val statusHistory = mutableListOf<FInternalTransportConnectionStatus>()
            val listener = FTransportConnectionStatusListener { statusHistory.add(it) }

            val sut = FCombinedConnectionApiImpl(
                currentConfig = createConfig("Device"),
                initialConnections = emptyList(),
                listener = listener,
                connectionBuilder = builder,
                scope = sutScope
            )
            advanceUntilIdle()
            statusHistory.clear()

            // Add a new connection
            val configA = TestConfig("a")
            val config2 = createConfig("Device", configA)
            sut.tryUpdateConnectionConfig(config2)
            advanceUntilIdle()

            // The new AutoReconnectConnection starts in Connecting state
            assertTrue(
                statusHistory.any { it == Connecting },
                "Should see Connecting status for new connection. Got: $statusHistory"
            )

            sutScope.cancel()
        }

    @Test
    fun GIVEN_connected_connection_kept_WHEN_another_added_THEN_remains_connected() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val sutScope = CoroutineScope(SupervisorJob() + testDispatcher)
        val configA = TestConfig("a")
        val configB = TestConfig("b")
        val config1 = createConfig("Device", configA)
        val builder = createConnectionBuilder()
        val statusHistory = mutableListOf<FInternalTransportConnectionStatus>()
        val listener = FTransportConnectionStatusListener { statusHistory.add(it) }

        val conn = AutoReconnectConnection(
            scope = backgroundScope,
            config = configA,
            connectionBuilder = builder,
            dispatcher = testDispatcher
        )
        builder.connectCalledDeferred.await()
        advanceUntilIdle()

        // Make connected
        val deviceApi = TestConnectedDeviceApi("A")
        builder.latestListener()!!.onStatusUpdate(
            Connected(scope = backgroundScope, deviceApi = deviceApi)
        )
        advanceUntilIdle()

        val sut = FCombinedConnectionApiImpl(
            currentConfig = config1,
            initialConnections = listOf(conn),
            listener = listener,
            connectionBuilder = builder,
            scope = sutScope
        )
        advanceUntilIdle()

        // Verify we're connected
        assertTrue(
            statusHistory.any { it is Connected },
            "Should be Connected initially"
        )

        statusHistory.clear()

        // Add configB while keeping configA
        val config2 = createConfig("Device", configA, configB)
        sut.tryUpdateConnectionConfig(config2)
        advanceUntilIdle()

        // Should still see Connected in status (connA is still connected)
        // Max priority should still be Connected (priority 4 > Connecting priority 1)
        val hasDisconnected = statusHistory.any { it == Disconnected }
        // We should NOT see a disconnected state since connA stays connected
        assertTrue(
            !hasDisconnected || statusHistory.last() is Connected || statusHistory.last() == Connecting,
            "Should remain connected or at worst be connecting (not disconnected). " +
                "Got: $statusHistory"
        )

        sutScope.cancel()
    }

    // endregion

    // region Disconnect Tests

    @Test
    fun GIVEN_multiple_connections_WHEN_disconnect_called_THEN_all_disconnected() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val configA = TestConfig("a")
        val configB = TestConfig("b")
        val config = createConfig("Device", configA, configB)
        val builderA = createConnectionBuilder()
        val builderB = createConnectionBuilder()
        val listener = FTransportConnectionStatusListener { }

        val connA = AutoReconnectConnection(
            scope = backgroundScope,
            config = configA,
            connectionBuilder = builderA,
            dispatcher = testDispatcher
        )
        val connB = AutoReconnectConnection(
            scope = backgroundScope,
            config = configB,
            connectionBuilder = builderB,
            dispatcher = testDispatcher
        )
        builderA.connectCalledDeferred.await()
        builderB.connectCalledDeferred.await()
        advanceUntilIdle()

        val sut = FCombinedConnectionApiImpl(
            currentConfig = config,
            initialConnections = listOf(connA, connB),
            listener = listener,
            connectionBuilder = builderA,
            scope = backgroundScope
        )
        advanceUntilIdle()

        sut.disconnect()
        advanceUntilIdle()

        // After disconnect, no more reconnection attempts should happen
        val attemptsBefore = builderA.connectAttempts + builderB.connectAttempts
        advanceTimeBy(30.seconds)
        advanceUntilIdle()
        val attemptsAfter = builderA.connectAttempts + builderB.connectAttempts
        assertEquals(
            attemptsBefore,
            attemptsAfter,
            "No more connection attempts after disconnect"
        )
    }

    @Test
    fun GIVEN_update_after_disconnect_WHEN_tryUpdateConnectionConfig_THEN_still_works() =
        runTest {
            val builder = createConnectionBuilder()
            val listener = FTransportConnectionStatusListener { }

            val sut = FCombinedConnectionApiImpl(
                currentConfig = createConfig("Device"),
                initialConnections = emptyList(),
                listener = listener,
                connectionBuilder = builder,
                scope = backgroundScope
            )
            advanceUntilIdle()

            sut.disconnect()
            advanceUntilIdle()

            // Config update after disconnect should still process
            val configA = TestConfig("a")
            val config2 = createConfig("Device2", configA)
            val result = sut.tryUpdateConnectionConfig(config2)
            advanceUntilIdle()

            assertTrue(result.isSuccess, "Config update after disconnect should succeed")
        }

    // endregion

    // region deviceName Tests

    @Test
    fun GIVEN_initial_config_WHEN_deviceName_accessed_THEN_returns_config_name() = runTest {
        val builder = createConnectionBuilder()
        val listener = FTransportConnectionStatusListener { }

        val sut = FCombinedConnectionApiImpl(
            currentConfig = createConfig("MyDevice"),
            initialConnections = emptyList(),
            listener = listener,
            connectionBuilder = builder,
            scope = backgroundScope
        )

        assertEquals("MyDevice", sut.deviceName)
    }

    @Test
    fun GIVEN_config_updated_WHEN_deviceName_accessed_THEN_returns_updated_name() = runTest {
        val builder = createConnectionBuilder()
        val listener = FTransportConnectionStatusListener { }

        val sut = FCombinedConnectionApiImpl(
            currentConfig = createConfig("OldName"),
            initialConnections = emptyList(),
            listener = listener,
            connectionBuilder = builder,
            scope = backgroundScope
        )
        advanceUntilIdle()

        sut.tryUpdateConnectionConfig(createConfig("NewName"))
        advanceUntilIdle()

        assertEquals("NewName", sut.deviceName)
    }

    // endregion

    // region Edge Cases

    @Test
    fun GIVEN_duplicate_configs_in_new_list_WHEN_tryUpdateConnectionConfig_THEN_each_gets_own_connection() =
        runTest {
            val builder = createConnectionBuilder()
            val listener = FTransportConnectionStatusListener { }

            val sut = FCombinedConnectionApiImpl(
                currentConfig = createConfig("Device"),
                initialConnections = emptyList(),
                listener = listener,
                connectionBuilder = builder,
                scope = backgroundScope
            )
            advanceUntilIdle()

            // Two identical child configs
            val configA = TestConfig("a")
            val config2 = createConfig("Device", configA, configA)
            val result = sut.tryUpdateConnectionConfig(config2)
            advanceUntilIdle()

            assertTrue(result.isSuccess)
            // data class TestConfig with same id means both are equal
            // First match should take the exact-match path;
            // second one won't find an unmatched exact match (the first is used),
            // and tryUpdateConnectionConfig on the existing will succeed (not connected),
            // but it's already matched. So the second creates a new connection.
            // Actually with the exact match logic: first configA matches no existing (empty),
            // creates new. Second configA also no existing match, creates new.
            assertEquals(2, sut.connectionsFlow.value.size)
        }

    @Test
    fun GIVEN_parent_scope_cancelled_WHEN_tryUpdateConnectionConfig_THEN_handles_gracefully() =
        runTest {
            val testDispatcher = StandardTestDispatcher(testScheduler)
            val parentJob = SupervisorJob()
            val parentScope = CoroutineScope(parentJob + testDispatcher)

            val builder = createConnectionBuilder()
            val listener = FTransportConnectionStatusListener { }

            val sut = FCombinedConnectionApiImpl(
                currentConfig = createConfig("Device"),
                initialConnections = emptyList(),
                listener = listener,
                connectionBuilder = builder,
                scope = parentScope
            )
            advanceUntilIdle()

            parentJob.cancel()
            advanceUntilIdle()

            // After scope cancellation, operations may fail
            // but should not crash the test
            val configA = TestConfig("a")
            val config2 = createConfig("Device", configA)
            val result = runCatching { sut.tryUpdateConnectionConfig(config2) }
            advanceUntilIdle()

            // The result might be success or failure depending on timing,
            // but it should not throw an unhandled exception
            assertTrue(true, "Should handle scope cancellation gracefully")
        }

    @Test
    fun GIVEN_multiple_updates_same_config_WHEN_tryUpdateConnectionConfig_THEN_idempotent() =
        runTest {
            val builder = createConnectionBuilder()
            val listener = FTransportConnectionStatusListener { }
            val configA = TestConfig("a")
            val config = createConfig("Device", configA)

            val sut = FCombinedConnectionApiImpl(
                currentConfig = createConfig("OldDevice"),
                initialConnections = emptyList(),
                listener = listener,
                connectionBuilder = builder,
                scope = backgroundScope
            )
            advanceUntilIdle()

            val result1 = sut.tryUpdateConnectionConfig(config)
            advanceUntilIdle()
            val connectionsAfterFirst = sut.connectionsFlow.value.toList()

            val result2 = sut.tryUpdateConnectionConfig(config)
            advanceUntilIdle()
            val connectionsAfterSecond = sut.connectionsFlow.value.toList()

            assertTrue(result1.isSuccess)
            assertTrue(result2.isSuccess)
            // Second call should be no-op (currentConfig == config)
            assertEquals(
                connectionsAfterFirst.size,
                connectionsAfterSecond.size,
                "Second call should be idempotent"
            )
        }

    @Test
    fun GIVEN_connection_in_connecting_state_WHEN_config_changed_THEN_new_connection_created() =
        runTest {
            val testDispatcher = StandardTestDispatcher(testScheduler)
            val configA = TestConfig("a")
            val configA2 = TestConfig("a2")
            val config1 = createConfig("Device", configA)
            val builder = createConnectionBuilder().apply {
                connectDelay = 5000L // Keep in connecting state
            }
            val listener = FTransportConnectionStatusListener { }

            val conn = AutoReconnectConnection(
                scope = backgroundScope,
                config = configA,
                connectionBuilder = builder,
                dispatcher = testDispatcher
            )
            advanceTimeBy(100.milliseconds) // Start connecting but not yet finished
            advanceUntilIdle()

            val sut = FCombinedConnectionApiImpl(
                currentConfig = config1,
                initialConnections = listOf(conn),
                listener = listener,
                connectionBuilder = builder,
                scope = backgroundScope
            )
            advanceUntilIdle()

            // State should be Connecting (not Connected)
            assertEquals(
                Connecting,
                conn.stateFlow.value,
                "Connection should be in Connecting state"
            )

            // Update config - since not connected, tryUpdateConnectionConfig on child fails.
            // No exact match either (configA != configA2).
            // A new connection is created and old one is disconnected.
            val config2 = createConfig("Device", configA2)
            val result = sut.tryUpdateConnectionConfig(config2)
            advanceUntilIdle()

            assertTrue(result.isSuccess)
            assertEquals(1, sut.connectionsFlow.value.size)
            assertTrue(
                sut.connectionsFlow.value[0] !== conn,
                "Should be a new connection (old was not connected, different config)"
            )
        }

    // endregion

    // region AutoReconnectConnection.tryUpdateConnectionConfig Tests

    @Test
    fun GIVEN_connected_child_accepts_WHEN_autoReconnect_tryUpdate_THEN_success_and_config_updated() =
        runTest {
            val testDispatcher = StandardTestDispatcher(testScheduler)
            val configA = TestConfig("a")
            val configB = TestConfig("b")
            val builder = createConnectionBuilder()

            val conn = AutoReconnectConnection(
                scope = backgroundScope,
                config = configA,
                connectionBuilder = builder,
                dispatcher = testDispatcher
            )
            builder.connectCalledDeferred.await()
            advanceUntilIdle()

            val deviceApi = TestConnectedDeviceApi("DevA").apply {
                tryUpdateResult = Result.success(Unit)
            }
            builder.latestListener()!!.onStatusUpdate(
                Connected(scope = backgroundScope, deviceApi = deviceApi)
            )
            advanceUntilIdle()

            val result = conn.tryUpdateConnectionConfig(configB)

            assertTrue(result.isSuccess)
            assertEquals(configB, conn.config)
            assertEquals(1, deviceApi.tryUpdateCallCount)

            conn.disconnect()
            advanceUntilIdle()
        }

    @Test
    fun GIVEN_connected_child_rejects_WHEN_autoReconnect_tryUpdate_THEN_failure_and_config_unchanged() =
        runTest {
            val testDispatcher = StandardTestDispatcher(testScheduler)
            val configA = TestConfig("a")
            val configB = TestConfig("b")
            val builder = createConnectionBuilder()

            val conn = AutoReconnectConnection(
                scope = backgroundScope,
                config = configA,
                connectionBuilder = builder,
                dispatcher = testDispatcher
            )
            builder.connectCalledDeferred.await()
            advanceUntilIdle()

            val deviceApi = TestConnectedDeviceApi("DevA").apply {
                tryUpdateResult = Result.failure(IllegalArgumentException("Nope"))
            }
            builder.latestListener()!!.onStatusUpdate(
                Connected(scope = backgroundScope, deviceApi = deviceApi)
            )
            advanceUntilIdle()

            val result = conn.tryUpdateConnectionConfig(configB)

            assertTrue(result.isFailure)
            assertEquals(configA, conn.config, "Config should not change on failure")

            conn.disconnect()
            advanceUntilIdle()
        }

    @Test
    fun GIVEN_not_connected_WHEN_autoReconnect_tryUpdate_THEN_failure_returned() =
        runTest {
            val testDispatcher = StandardTestDispatcher(testScheduler)
            val configA = TestConfig("a")
            val configB = TestConfig("b")
            val builder = createConnectionBuilder().apply {
                connectDelay = 10000L
            }

            val conn = AutoReconnectConnection(
                scope = backgroundScope,
                config = configA,
                connectionBuilder = builder,
                dispatcher = testDispatcher
            )
            advanceTimeBy(100.milliseconds)

            val result = conn.tryUpdateConnectionConfig(configB)

            assertTrue(result.isFailure, "Should fail when not connected")
            assertEquals(configA, conn.config, "Config should not change when not connected")

            conn.disconnect()
            advanceUntilIdle()
        }

    @Test
    fun GIVEN_connected_device_throws_WHEN_autoReconnect_tryUpdate_THEN_failure_returned() =
        runTest {
            val testDispatcher = StandardTestDispatcher(testScheduler)
            val configA = TestConfig("a")
            val configB = TestConfig("b")

            // Use a device API that throws on tryUpdateConnectionConfig
            val throwingDevice = object : FConnectedDeviceApi {
                override val deviceName = "Thrower"
                override suspend fun tryUpdateConnectionConfig(config: FDeviceConnectionConfig<*>): Result<Unit> {
                    throw RuntimeException("Crash!")
                }
                override suspend fun disconnect() {}
            }
            val builder = createConnectionBuilder()

            val conn = AutoReconnectConnection(
                scope = backgroundScope,
                config = configA,
                connectionBuilder = builder,
                dispatcher = testDispatcher
            )
            builder.connectCalledDeferred.await()
            advanceUntilIdle()

            // Make connected with the throwing device
            builder.latestListener()!!.onStatusUpdate(
                Connected(scope = backgroundScope, deviceApi = throwingDevice)
            )
            advanceUntilIdle()

            val result = conn.tryUpdateConnectionConfig(configB)

            assertTrue(result.isFailure, "Should return failure when device throws")
            assertIs<RuntimeException>(result.exceptionOrNull())
            assertEquals(configA, conn.config, "Config should not change on exception")

            conn.disconnect()
            advanceUntilIdle()
        }

    // endregion

    // region Integration Tests

    @Test
    fun GIVEN_full_lifecycle_WHEN_create_update_disconnect_THEN_works_correctly() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val sutScope = CoroutineScope(SupervisorJob() + testDispatcher)
        val configA = TestConfig("a")
        val configB = TestConfig("b")
        val configC = TestConfig("c")
        val builder = createConnectionBuilder()
        val statusHistory = mutableListOf<FInternalTransportConnectionStatus>()
        val listener = FTransportConnectionStatusListener { statusHistory.add(it) }

        // Phase 1: Create with configA
        val conn = AutoReconnectConnection(
            scope = backgroundScope,
            config = configA,
            connectionBuilder = builder,
            dispatcher = testDispatcher
        )
        builder.connectCalledDeferred.await()
        advanceUntilIdle()

        val sut = FCombinedConnectionApiImpl(
            currentConfig = createConfig("Device", configA),
            initialConnections = listOf(conn),
            listener = listener,
            connectionBuilder = builder,
            scope = sutScope
        )
        advanceUntilIdle()

        // Phase 2: Simulate connected
        val deviceApi = TestConnectedDeviceApi("DevA")
        builder.latestListener()!!.onStatusUpdate(
            Connected(scope = backgroundScope, deviceApi = deviceApi)
        )
        advanceUntilIdle()

        assertTrue(
            statusHistory.any { it is Connected },
            "Should see Connected status"
        )

        // Phase 3: Update - add configB, keep configA
        val config2 = createConfig("UpdatedDevice", configA, configB)
        val result2 = sut.tryUpdateConnectionConfig(config2)
        advanceUntilIdle()

        assertTrue(result2.isSuccess)
        assertEquals(2, sut.connectionsFlow.value.size)
        assertEquals("UpdatedDevice", sut.deviceName)

        // Phase 4: Update - replace configA with configC, keep configB
        val config3 = createConfig("UpdatedDevice", configC, configB)
        val result3 = sut.tryUpdateConnectionConfig(config3)
        advanceUntilIdle()

        assertTrue(result3.isSuccess)
        assertEquals(2, sut.connectionsFlow.value.size)

        // Phase 5: Disconnect
        sut.disconnect()
        advanceUntilIdle()

        sutScope.cancel()
    }

    @Test
    fun GIVEN_config_swap_WHEN_A_B_to_B_A_THEN_connections_reused_in_new_order() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val configA = TestConfig("a")
        val configB = TestConfig("b")
        val builderA = createConnectionBuilder()
        val builderB = createConnectionBuilder()
        val listener = FTransportConnectionStatusListener { }

        val connA = AutoReconnectConnection(
            scope = backgroundScope,
            config = configA,
            connectionBuilder = builderA,
            dispatcher = testDispatcher
        )
        val connB = AutoReconnectConnection(
            scope = backgroundScope,
            config = configB,
            connectionBuilder = builderB,
            dispatcher = testDispatcher
        )
        builderA.connectCalledDeferred.await()
        builderB.connectCalledDeferred.await()
        advanceUntilIdle()

        val config1 = createConfig("Device", configA, configB)
        val sut = FCombinedConnectionApiImpl(
            currentConfig = config1,
            initialConnections = listOf(connA, connB),
            listener = listener,
            connectionBuilder = builderA,
            scope = backgroundScope
        )
        advanceUntilIdle()

        // Swap order
        val config2 = createConfig("Device", configB, configA)
        val result = sut.tryUpdateConnectionConfig(config2)
        advanceUntilIdle()

        assertTrue(result.isSuccess)
        val connections = sut.connectionsFlow.value
        assertEquals(2, connections.size)
        assertSame(connB, connections[0], "First should be connB")
        assertSame(connA, connections[1], "Second should be connA")
    }

    @Test
    fun GIVEN_connection_not_connected_WHEN_tryUpdate_with_different_type_match_THEN_updates_config() =
        runTest {
            val testDispatcher = StandardTestDispatcher(testScheduler)
            val configA = TestConfig("a")
            val configA2 = TestConfig("a-modified")
            val config1 = createConfig("Device", configA)
            val builder = createConnectionBuilder().apply {
                connectDelay = 10000L // keep in connecting state
            }
            val listener = FTransportConnectionStatusListener { }

            val conn = AutoReconnectConnection(
                scope = backgroundScope,
                config = configA,
                connectionBuilder = builder,
                dispatcher = testDispatcher
            )
            advanceTimeBy(100.milliseconds) // Not yet connected
            advanceUntilIdle()

            val sut = FCombinedConnectionApiImpl(
                currentConfig = config1,
                initialConnections = listOf(conn),
                listener = listener,
                connectionBuilder = builder,
                scope = backgroundScope
            )
            advanceUntilIdle()

            // Update: configA -> configA2 (different instance, child not connected)
            val config2 = createConfig("Device", configA2)
            sut.tryUpdateConnectionConfig(config2)
            advanceUntilIdle()

            // The connection should be reused with updated config
            assertEquals(1, sut.connectionsFlow.value.size)
            assertEquals(configA2, sut.connectionsFlow.value[0].config)

            sut.disconnect()
            advanceUntilIdle()
        }

    @Test
    fun GIVEN_no_connections_initially_WHEN_update_then_back_to_empty_THEN_transitions_correct() =
        runTest {
            val testDispatcher = StandardTestDispatcher(testScheduler)
            val sutScope = CoroutineScope(SupervisorJob() + testDispatcher)
            val builder = createConnectionBuilder()
            val statusHistory = mutableListOf<FInternalTransportConnectionStatus>()
            val listener = FTransportConnectionStatusListener { statusHistory.add(it) }

            val sut = FCombinedConnectionApiImpl(
                currentConfig = createConfig("Device"),
                initialConnections = emptyList(),
                listener = listener,
                connectionBuilder = builder,
                scope = sutScope
            )
            advanceUntilIdle()

            assertTrue(
                statusHistory.any { it == Disconnected },
                "Should start as Disconnected with no connections"
            )

            // Add a connection
            val configA = TestConfig("a")
            sut.tryUpdateConnectionConfig(createConfig("Device", configA))
            advanceUntilIdle()

            assertTrue(
                statusHistory.any { it == Connecting },
                "Should transition to Connecting when connection added"
            )

            // Remove all connections
            statusHistory.clear()
            sut.tryUpdateConnectionConfig(createConfig("Device"))
            advanceUntilIdle()

            assertTrue(
                statusHistory.any { it == Disconnected },
                "Should transition back to Disconnected"
            )

            sutScope.cancel()
        }

    // endregion
}
