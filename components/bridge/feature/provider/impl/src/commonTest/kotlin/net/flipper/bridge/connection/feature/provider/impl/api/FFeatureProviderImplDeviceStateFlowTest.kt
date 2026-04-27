package net.flipper.bridge.connection.feature.provider.impl.api

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
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
import net.flipper.bridge.connection.orchestrator.api.FDeviceOrchestrator
import net.flipper.bridge.connection.orchestrator.api.model.FDeviceConnectStatus
import net.flipper.bridge.connection.orchestrator.api.model.FDeviceTransportType
import net.flipper.bridge.connection.transport.common.api.FConnectedDeviceApi
import net.flipper.bridge.connection.transport.common.api.FDeviceConnectionConfig
import net.flipper.busylib.core.wrapper.WrappedStateFlow
import net.flipper.busylib.core.wrapper.wrap
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class FFeatureProviderImplDeviceStateFlowTest {

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

    private class FakeBSBDeviceApi : FBSBDeviceApi {
        override suspend fun <T : FDeviceFeatureApi> get(clazz: KClass<T>): Deferred<T?>? =
            CompletableDeferred<T?>(null)
    }

    private class CapturingFactory : FBSBDeviceApi.Factory {
        val capturedScopes = mutableListOf<CoroutineScope>()
        override fun invoke(
            scope: CoroutineScope,
            connectedDevice: FConnectedDeviceApi
        ): FBSBDeviceApi {
            capturedScopes += scope
            return FakeBSBDeviceApi()
        }
    }

    private fun connectedStatus(scope: CoroutineScope) = FDeviceConnectStatus.Connected(
        scope = scope,
        device = device(),
        deviceApi = FakeConnectedDeviceApi(),
        transportType = FDeviceTransportType.BLE,
    )

    private fun TestScope.scopeWith(
        handler: CoroutineExceptionHandler = CoroutineExceptionHandler { _, _ -> }
    ): CoroutineScope = CoroutineScope(
        SupervisorJob(backgroundScope.coroutineContext.job) +
            UnconfinedTestDispatcher(testScheduler) +
            handler
    )

    @Test
    fun GIVEN_connected_status_WHEN_child_in_factory_scope_throws_THEN_status_scope_handler_handles_it() =
        runTest {
            val statusExceptions = mutableListOf<Throwable>()
            val globalExceptions = mutableListOf<Throwable>()
            val statusScope =
                scopeWith(CoroutineExceptionHandler { _, e -> statusExceptions += e })
            val globalScope =
                scopeWith(CoroutineExceptionHandler { _, e -> globalExceptions += e })
            val factory = CapturingFactory()
            val orchestrator = FakeOrchestrator(connectedStatus(statusScope))

            FFeatureProviderImpl(
                orchestrator = orchestrator,
                fBSBDeviceApiFactory = factory,
                globalScope = globalScope,
            )
            advanceUntilIdle()
            val capturedScope = factory.capturedScopes.single()

            val expected = RuntimeException("kaboom")
            capturedScope.launch { throw expected }
            advanceUntilIdle()

            assertEquals(
                listOf<Throwable>(expected),
                statusExceptions,
                "Crashes inside factory's scope must be routed to status.scope's CoroutineExceptionHandler"
            )
            assertEquals(
                emptyList<Throwable>(),
                globalExceptions,
                "Crashes from factory's scope must NOT bubble up to globalScope"
            )
        }

    @Test
    fun GIVEN_connected_status_WHEN_statusPairScope_dies_THEN_deviceStateFlow_emits_null() =
        runTest {
            val statusScope = scopeWith()
            val globalScope = scopeWith()
            val factory = CapturingFactory()
            val orchestrator = FakeOrchestrator(connectedStatus(statusScope))

            val sut = FFeatureProviderImpl(
                orchestrator = orchestrator,
                fBSBDeviceApiFactory = factory,
                globalScope = globalScope,
            )
            advanceUntilIdle()

            val nonNullValue = sut.deviceStateFlow.first { it != null }
            assertNotNull(
                nonNullValue,
                "Precondition: deviceStateFlow must publish the fBSBDeviceApi after the factory runs"
            )

            statusScope.cancel()
            advanceUntilIdle()

            assertNull(
                sut.deviceStateFlow.value,
                "When statusPairScope dies, deviceStateFlow must publish null again. " +
                    "Current value: ${sut.deviceStateFlow.value}"
            )
        }

    @Test
    fun GIVEN_both_parents_alive_WHEN_factory_scope_captured_THEN_factory_scope_is_active() =
        runTest {
            val statusScope = scopeWith()
            val globalScope = scopeWith()
            val factory = CapturingFactory()
            val orchestrator = FakeOrchestrator(connectedStatus(statusScope))

            FFeatureProviderImpl(
                orchestrator = orchestrator,
                fBSBDeviceApiFactory = factory,
                globalScope = globalScope,
            )
            advanceUntilIdle()
            val capturedScope = factory.capturedScopes.single()

            assertTrue(
                statusScope.coroutineContext.job.isActive,
                "Precondition: status.scope is alive"
            )
            assertTrue(
                globalScope.coroutineContext.job.isActive,
                "Precondition: globalScope is alive"
            )
            assertTrue(
                capturedScope.coroutineContext.job.isActive,
                "Factory scope must be alive while BOTH status.scope and globalScope are alive"
            )
        }

    @Test
    fun GIVEN_factory_scope_captured_WHEN_statusPairScope_cancelled_THEN_factory_scope_is_cancelled() =
        runTest {
            val statusScope = scopeWith()
            val globalScope = scopeWith()
            val factory = CapturingFactory()
            val orchestrator = FakeOrchestrator(connectedStatus(statusScope))

            FFeatureProviderImpl(
                orchestrator = orchestrator,
                fBSBDeviceApiFactory = factory,
                globalScope = globalScope,
            )
            advanceUntilIdle()
            val capturedScope = factory.capturedScopes.single()
            assertTrue(capturedScope.coroutineContext.job.isActive)

            statusScope.cancel()
            advanceUntilIdle()

            assertFalse(
                capturedScope.coroutineContext.job.isActive,
                "Factory scope must die when status.scope dies (one of the two parents is gone)"
            )
        }

    @Test
    fun GIVEN_factory_scope_captured_WHEN_globalScope_cancelled_THEN_factory_scope_is_cancelled() =
        runTest {
            val statusScope = scopeWith()
            val globalScope = scopeWith()
            val factory = CapturingFactory()
            val orchestrator = FakeOrchestrator(connectedStatus(statusScope))

            FFeatureProviderImpl(
                orchestrator = orchestrator,
                fBSBDeviceApiFactory = factory,
                globalScope = globalScope,
            )
            advanceUntilIdle()
            val capturedScope = factory.capturedScopes.single()
            assertTrue(capturedScope.coroutineContext.job.isActive)

            globalScope.cancel()
            advanceUntilIdle()

            assertFalse(
                capturedScope.coroutineContext.job.isActive,
                "Factory scope must die when globalScope dies (one of the two parents is gone)"
            )
        }

    @Test
    fun GIVEN_factory_scope_captured_WHEN_factory_scope_cancelled_THEN_status_scope_remains_alive() =
        runTest {
            val statusScope = scopeWith()
            val globalScope = scopeWith()
            val factory = CapturingFactory()
            val orchestrator = FakeOrchestrator(connectedStatus(statusScope))

            FFeatureProviderImpl(
                orchestrator = orchestrator,
                fBSBDeviceApiFactory = factory,
                globalScope = globalScope,
            )
            advanceUntilIdle()
            val capturedScope = factory.capturedScopes.single()

            capturedScope.cancel()
            advanceUntilIdle()

            assertTrue(
                statusScope.coroutineContext.job.isActive,
                "Cancelling the factory's child scope must NOT cancel status.scope"
            )
            assertTrue(
                globalScope.coroutineContext.job.isActive,
                "Cancelling the factory's child scope must NOT cancel globalScope either"
            )
        }
}
