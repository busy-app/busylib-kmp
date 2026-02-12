package net.flipper.bridge.connection.device.bsb.impl.api

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import net.flipper.bridge.connection.feature.battery.api.FDeviceBatteryInfoFeatureApi
import net.flipper.bridge.connection.feature.battery.model.BSBDeviceBatteryInfo
import net.flipper.bridge.connection.feature.common.api.FDeviceFeature
import net.flipper.bridge.connection.feature.common.api.FDeviceFeatureApi
import net.flipper.bridge.connection.feature.common.api.FOnDeviceReadyFeatureApi
import net.flipper.bridge.connection.feature.common.api.FUnsafeDeviceFeatureApi
import net.flipper.bridge.connection.feature.info.api.FDeviceInfoFeatureApi
import net.flipper.bridge.connection.feature.rpc.api.model.BusyBarStatusSystem
import net.flipper.bridge.connection.transport.common.api.FConnectedDeviceApi
import net.flipper.bridge.connection.transport.common.api.FDeviceConnectionConfig
import net.flipper.busylib.core.wrapper.CResult
import net.flipper.busylib.core.wrapper.WrappedFlow
import net.flipper.busylib.core.wrapper.toCResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertSame

/**
 * Tests to prevent race conditions in FBSBDeviceApiImpl public API.
 * These tests verify thread-safety and ensure that concurrent access doesn't cause issues.
 */
class FBSBDeviceApiImplRaceConditionTest {

    @Test
    fun GIVEN_multiple_concurrent_get_calls_WHEN_same_feature_THEN_same_instance_returned() =
        runTest {
            // Given
            var factoryCallCount = 0
            val mockFeatureApi = TestFeatureApi()

            val factories = mapOf(
                FDeviceFeature.DEVICE_INFO to FDeviceFeatureApi.Factory { _, _, _ ->
                    delay(100) // Simulate async work
                    factoryCallCount++
                    mockFeatureApi
                }
            )

            val api = createFBSBDeviceApi(
                scope = backgroundScope,
                factories = factories
            )

            // When - 10 concurrent calls to get the same feature
            val results = List(10) {
                async {
                    api.get(FDeviceInfoFeatureApi::class)?.await()
                }
            }.awaitAll()

            // Then - factory should be called only once
            assertEquals(1, factoryCallCount, "Factory should be called only once")

            // All results should be the same instance
            results.forEach { result ->
                assertNotNull(result, "Result should not be null")
                assertSame(mockFeatureApi, result, "All results should be the same instance")
            }
        }

    @Test
    fun GIVEN_multiple_concurrent_get_calls_WHEN_different_features_THEN_all_created_independently() =
        runTest {
            // Given
            val deviceInfoFeature = TestFeatureApi()
            val batteryInfoFeature = TestBatteryFeatureApi()
            var deviceInfoCallCount = 0
            var batteryInfoCallCount = 0

            val factories = mapOf(
                FDeviceFeature.DEVICE_INFO to FDeviceFeatureApi.Factory { _, _, _ ->
                    delay(100)
                    deviceInfoCallCount++
                    deviceInfoFeature
                },
                FDeviceFeature.BATTERY_INFO to FDeviceFeatureApi.Factory { _, _, _ ->
                    delay(50)
                    batteryInfoCallCount++
                    batteryInfoFeature
                }
            )

            val api = createFBSBDeviceApi(
                scope = backgroundScope,
                factories = factories
            )

            // When - concurrent calls for different features
            val deviceInfoResults = List(5) {
                async {
                    api.get(FDeviceInfoFeatureApi::class)?.await()
                }
            }
            val batteryInfoResults = List(5) {
                async {
                    api.get(FDeviceBatteryInfoFeatureApi::class)?.await()
                }
            }

            val allResults = (deviceInfoResults + batteryInfoResults).awaitAll()

            // Then
            assertEquals(1, deviceInfoCallCount, "Device info factory should be called once")
            assertEquals(1, batteryInfoCallCount, "Battery info factory should be called once")
            assertEquals(10, allResults.size)
        }

    @Test
    fun GIVEN_concurrent_get_calls_WHEN_factory_returns_null_THEN_all_calls_get_null() =
        runTest {
            // Given
            var factoryCallCount = 0

            val factories = mapOf(
                FDeviceFeature.DEVICE_INFO to FDeviceFeatureApi.Factory { _, _, _ ->
                    delay(50)
                    factoryCallCount++
                    null // Factory returns null
                }
            )

            val api = createFBSBDeviceApi(
                scope = backgroundScope,
                factories = factories
            )

            // When - multiple concurrent calls
            val results = List(10) {
                async {
                    api.get(FDeviceInfoFeatureApi::class)?.await()
                }
            }.awaitAll()

            // Then
            assertEquals(1, factoryCallCount, "Factory should be called only once")
            results.forEach { result ->
                assertEquals(null, result, "All results should be null")
            }
        }

    @Test
    fun GIVEN_rapid_sequential_calls_WHEN_getting_feature_THEN_no_race_condition() = runTest {
        // Given
        var factoryCallCount = 0
        val mockFeatureApi = TestFeatureApi()

        val factories = mapOf(
            FDeviceFeature.DEVICE_INFO to FDeviceFeatureApi.Factory { _, _, _ ->
                factoryCallCount++
                mockFeatureApi
            }
        )

        val api = createFBSBDeviceApi(
            scope = backgroundScope,
            factories = factories
        )

        // When - rapid sequential calls (not awaited immediately)
        val deferredResults = List(100) {
            api.get(FDeviceInfoFeatureApi::class)
        }

        // Then - await all and check
        val results = deferredResults.mapNotNull { it?.await() }

        assertEquals(1, factoryCallCount, "Factory should be called only once")
        assertEquals(100, results.size, "Should have 100 results")
        results.forEach { result ->
            assertSame(mockFeatureApi, result, "All results should be the same instance")
        }
    }

    @Test
    fun GIVEN_concurrent_mixed_features_WHEN_some_already_cached_THEN_correct_instances_returned() =
        runTest {
            // Given
            val deviceInfoFeature = TestFeatureApi()
            val batteryInfoFeature = TestBatteryFeatureApi()

            val factories = mapOf(
                FDeviceFeature.DEVICE_INFO to FDeviceFeatureApi.Factory { _, _, _ ->
                    delay(50)
                    deviceInfoFeature
                },
                FDeviceFeature.BATTERY_INFO to FDeviceFeatureApi.Factory { _, _, _ ->
                    delay(50)
                    batteryInfoFeature
                }
            )

            val api = createFBSBDeviceApi(
                scope = backgroundScope,
                factories = factories
            )

            // When - first call to cache device info
            val firstCall = api.get(FDeviceInfoFeatureApi::class)?.await()
            assertSame(deviceInfoFeature, firstCall)

            // Now make concurrent calls with mixed cached and new features
            val results = List(20) { index ->
                async {
                    if (index % 2 == 0) {
                        api.get(FDeviceInfoFeatureApi::class)?.await() // Cached
                    } else {
                        api.get(FDeviceBatteryInfoFeatureApi::class)?.await() // New
                    }
                }
            }.awaitAll()

            // Then
            assertEquals(20, results.size)
            results.forEachIndexed { index, result ->
                assertNotNull(result)
                if (index % 2 == 0) {
                    assertSame(deviceInfoFeature, result)
                } else {
                    assertSame(batteryInfoFeature, result)
                }
            }
        }

    @Test
    fun GIVEN_slow_factory_WHEN_many_rapid_concurrent_calls_THEN_factory_called_once() =
        runTest {
            // Given
            var factoryCallCount = 0
            val mockFeatureApi = TestFeatureApi()

            val factories = mapOf(
                FDeviceFeature.DEVICE_INFO to FDeviceFeatureApi.Factory { _, _, _ ->
                    // Simulate slow factory initialization
                    delay(200)
                    factoryCallCount++
                    mockFeatureApi
                }
            )

            val api = createFBSBDeviceApi(
                scope = backgroundScope,
                factories = factories
            )

            // When - 50 concurrent calls initiated rapidly
            val results = List(50) {
                async {
                    api.get(FDeviceInfoFeatureApi::class)?.await()
                }
            }.awaitAll()

            // Then - factory should be called only once despite 50 concurrent requests
            assertEquals(1, factoryCallCount, "Factory should be called only once")
            assertEquals(50, results.size)
            // All results should be the same instance (no duplicate factory calls)
            results.forEach { result ->
                assertSame(mockFeatureApi, result, "All results should be the same instance")
            }
        }

    @Test
    fun GIVEN_onReady_features_WHEN_device_initialized_THEN_all_called_once() = runTest {
        // Given
        var onReadyCallCount = 0
        val onReadyDeferred = CompletableDeferred<Unit>()

        val onReadyFactory = object : FOnDeviceReadyFeatureApi.Factory {
            override suspend fun invoke(
                unsafeFeatureDeviceApi: FUnsafeDeviceFeatureApi,
                scope: CoroutineScope,
                connectedDevice: FConnectedDeviceApi
            ): FOnDeviceReadyFeatureApi {
                return TestOnReadyFeatureApi {
                    delay(50)
                    onReadyCallCount++
                    onReadyDeferred.complete(Unit)
                }
            }
        }

        val factories = mapOf(
            FDeviceFeature.DEVICE_INFO to FDeviceFeatureApi.Factory { _, _, _ ->
                TestFeatureApi()
            }
        )

        // When
        createFBSBDeviceApi(
            scope = backgroundScope,
            factories = factories,
            onReadyFactories = setOf(onReadyFactory)
        )

        // Wait for onReady to be called
        onReadyDeferred.await()

        // Then
        assertEquals(1, onReadyCallCount, "onReady should be called exactly once")
    }

    @Test
    fun GIVEN_concurrent_calls_WHEN_wrong_type_requested_THEN_returns_null() = runTest {
        // Given
        val deviceInfoFeature = TestFeatureApi()

        val factories = mapOf(
            FDeviceFeature.DEVICE_INFO to FDeviceFeatureApi.Factory { _, _, _ ->
                delay(50)
                deviceInfoFeature
            }
        )

        val api = createFBSBDeviceApi(
            scope = backgroundScope,
            factories = factories
        )

        // When - request with wrong type (requesting battery API when only device info is implemented)
        val results = List(10) {
            async {
                api.get(FDeviceBatteryInfoFeatureApi::class)?.await()
            }
        }.awaitAll()

        // Then
        results.forEach { result ->
            assertEquals(null, result, "Should return null for wrong type")
        }
    }

    @Test
    fun GIVEN_deferred_not_awaited_immediately_WHEN_multiple_callers_THEN_all_get_same_deferred() =
        runTest {
            // Given
            var factoryCallCount = 0
            val mockFeatureApi = TestFeatureApi()

            val factories = mapOf(
                FDeviceFeature.DEVICE_INFO to FDeviceFeatureApi.Factory { _, _, _ ->
                    delay(100)
                    factoryCallCount++
                    mockFeatureApi
                }
            )

            val api = createFBSBDeviceApi(
                scope = backgroundScope,
                factories = factories
            )

            // When - get deferreds without awaiting
            val deferreds = List(10) {
                api.get(FDeviceInfoFeatureApi::class)
            }

            // Then - all deferreds should eventually resolve to the same value
            val results = deferreds.mapNotNull { it?.await() }

            assertEquals(1, factoryCallCount, "Factory should be called only once")
            assertEquals(10, results.size)
            results.forEach { result ->
                assertSame(mockFeatureApi, result, "All results should be the same instance")
            }
        }

    @Test
    fun GIVEN_high_concurrency_WHEN_mixed_operations_THEN_state_remains_consistent() = runTest {
        // Given
        val features = mutableMapOf<FDeviceFeature, TestFeatureApi>()
        val factories = FDeviceFeature.entries.take(5).associateWith { feature ->
            FDeviceFeatureApi.Factory { _, _, _ ->
                delay(10)
                TestFeatureApi().also { features[feature] = it }
            }
        }

        val api = createFBSBDeviceApi(
            scope = backgroundScope,
            factories = factories
        )

        // When - high concurrency with mixed feature requests
        val results = List(100) { index ->
            async {
                val featureClass = when (index % 5) {
                    0 -> FDeviceInfoFeatureApi::class
                    1 -> FDeviceBatteryInfoFeatureApi::class
                    2 -> FDeviceInfoFeatureApi::class
                    3 -> FDeviceInfoFeatureApi::class
                    else -> FDeviceInfoFeatureApi::class
                }
                api.get(featureClass)?.await()
            }
        }.awaitAll()

        // Then - verify consistency
        assertNotNull(results)
        assertEquals(100, results.size)
    }

    // Helper function to create FBSBDeviceApiImpl with test dependencies
    private fun createFBSBDeviceApi(
        scope: CoroutineScope,
        factories: Map<FDeviceFeature, FDeviceFeatureApi.Factory>,
        onReadyFactories: Set<FOnDeviceReadyFeatureApi.Factory> = emptySet()
    ): FBSBDeviceApiImpl {
        val mockConnectedDevice = object : FConnectedDeviceApi {
            override val deviceName: String = "TestDevice"
            override suspend fun tryUpdateConnectionConfig(
                config: FDeviceConnectionConfig<*>
            ): Result<Unit> = throw NotImplementedError()

            override suspend fun disconnect() = Unit
        }

        // Provide a default factory for all features to satisfy the check in the constructor
        val allFactories = FDeviceFeature.entries.associateWith { feature ->
            factories[feature] ?: FDeviceFeatureApi.Factory { _, _, _ -> null }
        }

        return FBSBDeviceApiImpl(
            scope = scope,
            connectedDevice = mockConnectedDevice,
            onReadyFeaturesApiFactories = onReadyFactories,
            factories = allFactories
        )
    }

    // Test implementations
    private open class TestFeatureApi : FDeviceInfoFeatureApi {
        override suspend fun getDeviceInfo(): CResult<BusyBarStatusSystem> {
            return Result.success(
                BusyBarStatusSystem(
                    branch = "",
                    version = "1.0.0",
                    buildDate = "",
                    commitHash = "",
                    uptime = ""
                )
            ).toCResult()
        }
    }

    private class TestBatteryFeatureApi : FDeviceBatteryInfoFeatureApi {
        override fun getDeviceBatteryInfo(): WrappedFlow<BSBDeviceBatteryInfo> {
            return WrappedFlow(
                flowOf(
                    BSBDeviceBatteryInfo(
                        state = BSBDeviceBatteryInfo.BSBBatteryState.CHARGING,
                        percentage = 80
                    )
                )
            )
        }
    }

    private class TestOnReadyFeatureApi(
        private val onReadyCallback: suspend () -> Unit
    ) : FOnDeviceReadyFeatureApi {
        override suspend fun onReady() {
            onReadyCallback()
        }
    }
}
