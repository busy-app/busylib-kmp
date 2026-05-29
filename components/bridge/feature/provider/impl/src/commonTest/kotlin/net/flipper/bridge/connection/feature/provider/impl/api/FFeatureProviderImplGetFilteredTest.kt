package net.flipper.bridge.connection.feature.provider.impl.api

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import net.flipper.bridge.connection.config.api.model.BUSYBar
import net.flipper.bridge.connection.config.api.model.BUSYBar.ConnectionWay
import net.flipper.bridge.connection.device.bsb.api.FBSBDeviceApi
import net.flipper.bridge.connection.feature.common.api.FDeviceFeatureApi
import net.flipper.bridge.connection.feature.provider.api.FFeatureStatus
import net.flipper.bridge.connection.orchestrator.api.FDeviceOrchestrator
import net.flipper.bridge.connection.orchestrator.api.model.DisconnectStatus
import net.flipper.bridge.connection.orchestrator.api.model.FDeviceConnectStatus
import net.flipper.bridge.connection.orchestrator.api.model.FDeviceTransportType
import net.flipper.bridge.connection.transport.common.api.FConnectedDeviceApi
import net.flipper.bridge.connection.transport.common.api.FDeviceConnectionConfig
import net.flipper.busylib.core.wrapper.WrappedStateFlow
import net.flipper.busylib.core.wrapper.wrap
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class FFeatureProviderImplGetFilteredTest {

    private fun device() = BUSYBar(
        humanReadableName = "Test",
        mock = ConnectionWay.Mock,
    )

    private class FakeOrchestrator(initial: FDeviceConnectStatus) : FDeviceOrchestrator {
        val state: MutableStateFlow<FDeviceConnectStatus> = MutableStateFlow(initial)
        override fun getState(): WrappedStateFlow<FDeviceConnectStatus> = state.wrap()
    }

    private class FakeConnectedDeviceApi : FConnectedDeviceApi {
        override val deviceName: String = "Test"
        override suspend fun tryUpdateConnectionConfig(
            config: FDeviceConnectionConfig<*>
        ): Result<Unit> = Result.success(Unit)

        override suspend fun disconnect() = Unit
    }

    private class FakeFeature : FDeviceFeatureApi

    /**
     * Models a single physical device. [isSame] reports identity against the
     * [FConnectedDeviceApi] this api was built for, mirroring the real contract that a
     * BSB api represents exactly one connected device. [feature] is what [get] resolves to.
     */
    private class FakeBSBDeviceApi(
        private val ownDevice: FConnectedDeviceApi,
        private val feature: FDeviceFeatureApi?,
    ) : FBSBDeviceApi {
        override fun isSame(connectedDevice: FConnectedDeviceApi): Boolean =
            ownDevice === connectedDevice

        @Suppress("UNCHECKED_CAST")
        override suspend fun <T : FDeviceFeatureApi> get(clazz: KClass<T>): Deferred<T?>? =
            CompletableDeferred(feature as T?)
    }

    private class FeatureFactory(private val feature: FDeviceFeatureApi?) : FBSBDeviceApi.Factory {
        override fun invoke(
            scope: CoroutineScope,
            connectedDevice: FConnectedDeviceApi
        ): FBSBDeviceApi = FakeBSBDeviceApi(ownDevice = connectedDevice, feature = feature)
    }

    private fun connectedStatus(
        scope: CoroutineScope,
        deviceApi: FConnectedDeviceApi,
    ) = FDeviceConnectStatus.Connected(
        scope = scope,
        device = device(),
        deviceApi = deviceApi,
        transportType = FDeviceTransportType.BLE,
    )

    // The impl requires the Connected status's scope to carry a CoroutineExceptionHandler
    // (see FFeatureProviderImpl line 60), so every scope here provides one.
    private fun TestScope.scope(): CoroutineScope = CoroutineScope(
        SupervisorJob(backgroundScope.coroutineContext.job) +
            UnconfinedTestDispatcher(testScheduler) +
            CoroutineExceptionHandler { _, _ -> }
    )

    private fun TestScope.collectFiltered(
        sut: FFeatureProviderImpl,
        status: FDeviceConnectStatus.Connected,
    ): List<FFeatureStatus<FakeFeature>> {
        val emissions = mutableListOf<FFeatureStatus<FakeFeature>>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            sut.getFiltered(status, FakeFeature::class).collect { emissions += it }
        }
        return emissions
    }

    @Test
    fun GIVEN_live_device_matches_status_and_feature_supported_THEN_emits_Supported() = runTest {
        val deviceApi = FakeConnectedDeviceApi()
        val feature = FakeFeature()
        val status = connectedStatus(scope(), deviceApi)
        val sut = FFeatureProviderImpl(
            orchestrator = FakeOrchestrator(status),
            fBSBDeviceApiFactory = FeatureFactory(feature),
            globalScope = scope(),
        )

        val emissions = collectFiltered(sut, status)
        advanceUntilIdle()

        assertIs<FFeatureStatus.Retrieving>(
            emissions.first(),
            "getFiltered must always start by reporting Retrieving",
        )
        val last = emissions.last()
        assertIs<FFeatureStatus.Supported<FakeFeature>>(
            last,
            "When the live device matches the status's device, the feature must be surfaced",
        )
        assertSame(
            feature,
            last.featureApi,
            "The exact feature resolved from the matching device must be emitted",
        )
    }

    @Test
    fun GIVEN_live_device_matches_status_but_feature_absent_THEN_emits_Unsupported() = runTest {
        val deviceApi = FakeConnectedDeviceApi()
        val status = connectedStatus(scope(), deviceApi)
        val sut = FFeatureProviderImpl(
            orchestrator = FakeOrchestrator(status),
            fBSBDeviceApiFactory = FeatureFactory(feature = null),
            globalScope = scope(),
        )

        val emissions = collectFiltered(sut, status)
        advanceUntilIdle()

        assertIs<FFeatureStatus.Unsupported>(
            emissions.last(),
            "A matching device that does not provide the feature must settle on Unsupported",
        )
    }

    @Test
    fun GIVEN_live_device_differs_from_status_THEN_never_resolves_and_stays_Retrieving() = runTest {
        // The orchestrator reports a DIFFERENT physical device than the one the caller's
        // status refers to. Even though that device DOES provide the feature, getFiltered
        // must not surface it — this is the wrong-device reaction the filter prevents.
        val liveDevice = FakeConnectedDeviceApi()
        val callerStatus = connectedStatus(scope(), FakeConnectedDeviceApi())
        val sut = FFeatureProviderImpl(
            orchestrator = FakeOrchestrator(connectedStatus(scope(), liveDevice)),
            fBSBDeviceApiFactory = FeatureFactory(FakeFeature()),
            globalScope = scope(),
        )

        val emissions = collectFiltered(sut, callerStatus)
        advanceUntilIdle()

        assertTrue(emissions.isNotEmpty(), "getFiltered must emit at least Retrieving")
        assertTrue(
            emissions.all { it is FFeatureStatus.Retrieving },
            "While the live device differs from the status's device, only Retrieving is allowed. " +
                "Got: $emissions",
        )
    }

    @Test
    fun GIVEN_no_device_connected_THEN_stays_Retrieving() = runTest {
        // The caller holds a Connected status, but the device has since dropped.
        val callerStatus = connectedStatus(scope(), FakeConnectedDeviceApi())
        val orchestrator = FakeOrchestrator(
            FDeviceConnectStatus.Disconnected(
                device = device(),
                reason = DisconnectStatus.REPORTED_BY_TRANSPORT,
            )
        )
        val sut = FFeatureProviderImpl(
            orchestrator = orchestrator,
            fBSBDeviceApiFactory = FeatureFactory(FakeFeature()),
            globalScope = scope(),
        )

        val emissions = collectFiltered(sut, callerStatus)
        advanceUntilIdle()

        assertTrue(emissions.isNotEmpty(), "getFiltered must emit at least Retrieving")
        assertTrue(
            emissions.all { it is FFeatureStatus.Retrieving },
            "With no live device, getFiltered must never resolve a feature. Got: $emissions",
        )
    }

    @Test
    fun GIVEN_supported_WHEN_a_different_device_connects_THEN_falls_back_to_Retrieving() = runTest {
        // The crux of the wrong-device fix: once a foreign device replaces the matching one,
        // the previously surfaced feature must be withdrawn rather than left stale.
        val deviceApi = FakeConnectedDeviceApi()
        val status = connectedStatus(scope(), deviceApi)
        val orchestrator = FakeOrchestrator(status)
        val sut = FFeatureProviderImpl(
            orchestrator = orchestrator,
            fBSBDeviceApiFactory = FeatureFactory(FakeFeature()),
            globalScope = scope(),
        )

        val emissions = collectFiltered(sut, status)
        advanceUntilIdle()
        assertIs<FFeatureStatus.Supported<FakeFeature>>(
            emissions.last(),
            "Precondition: matching device must surface the feature first",
        )

        orchestrator.state.value = connectedStatus(scope(), FakeConnectedDeviceApi())
        advanceUntilIdle()

        assertIs<FFeatureStatus.Retrieving>(
            emissions.last(),
            "When a different device connects, the stale feature must be dropped to Retrieving",
        )
    }

    @Test
    fun GIVEN_wrong_device_WHEN_matching_device_connects_THEN_resolves_Supported() = runTest {
        val matchingDevice = FakeConnectedDeviceApi()
        val callerStatus = connectedStatus(scope(), matchingDevice)
        val feature = FakeFeature()
        val orchestrator = FakeOrchestrator(connectedStatus(scope(), FakeConnectedDeviceApi()))
        val sut = FFeatureProviderImpl(
            orchestrator = orchestrator,
            fBSBDeviceApiFactory = FeatureFactory(feature),
            globalScope = scope(),
        )

        val emissions = collectFiltered(sut, callerStatus)
        advanceUntilIdle()
        assertTrue(
            emissions.all { it is FFeatureStatus.Retrieving },
            "Precondition: a foreign device must keep getFiltered in Retrieving. Got: $emissions",
        )

        orchestrator.state.value = connectedStatus(scope(), matchingDevice)
        advanceUntilIdle()

        val last = emissions.last()
        assertIs<FFeatureStatus.Supported<FakeFeature>>(
            last,
            "Once the matching device connects, the feature must finally be surfaced",
        )
        assertSame(feature, last.featureApi)
    }

    @Test
    fun GIVEN_matching_device_WHEN_same_feature_class_requested_THEN_uses_requested_class() = runTest {
        // Guards that the requested KClass is forwarded to the device unchanged.
        val deviceApi = FakeConnectedDeviceApi()
        val status = connectedStatus(scope(), deviceApi)
        val requested = mutableListOf<KClass<*>>()
        val recordingFactory = object : FBSBDeviceApi.Factory {
            override fun invoke(scope: CoroutineScope, connectedDevice: FConnectedDeviceApi) =
                object : FBSBDeviceApi {
                    override fun isSame(connectedDevice: FConnectedDeviceApi) =
                        connectedDevice === deviceApi

                    @Suppress("UNCHECKED_CAST")
                    override suspend fun <T : FDeviceFeatureApi> get(
                        clazz: KClass<T>
                    ): Deferred<T?> {
                        requested += clazz
                        return CompletableDeferred(FakeFeature() as T?)
                    }
                }
        }
        val sut = FFeatureProviderImpl(
            orchestrator = FakeOrchestrator(status),
            fBSBDeviceApiFactory = recordingFactory,
            globalScope = scope(),
        )

        collectFiltered(sut, status)
        advanceUntilIdle()

        assertEquals(
            listOf<KClass<*>>(FakeFeature::class),
            requested,
            "getFiltered must request exactly the feature class it was given",
        )
    }
}
