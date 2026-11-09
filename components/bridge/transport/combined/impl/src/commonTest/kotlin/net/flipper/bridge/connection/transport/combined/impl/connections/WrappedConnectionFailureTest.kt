@file:Suppress("FunctionNaming")

package net.flipper.bridge.connection.transport.combined.impl.connections

import kotlinx.coroutines.isActive
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import net.flipper.bridge.connection.transport.combined.impl.connections.helpers.MockConnectionBuilder
import net.flipper.bridge.connection.transport.combined.impl.connections.helpers.TestConfig
import net.flipper.bridge.connection.transport.common.api.FInternalDisconnectedReason
import net.flipper.bridge.connection.transport.common.api.FInternalTransportConnectionStatus
import net.flipper.bridge.connection.transport.common.api.FailedPairingConnectException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WrappedConnectionFailureTest {
    @Test
    fun GIVEN_rejected_pairing_WHEN_connection_scope_finishes_THEN_repair_reason_is_retained() = runTest {
        val builder = MockConnectionBuilder().apply {
            shouldAlwaysFail = true
            failureException = FailedPairingConnectException()
        }
        val connection = WrappedConnectionInternal(
            config = TestConfig(),
            parentScope = backgroundScope,
            connectionBuilder = builder,
            dispatcher = StandardTestDispatcher(testScheduler)
        )

        runCurrent()

        assertEquals(
            FInternalTransportConnectionStatus.Disconnected(FInternalDisconnectedReason.REQUIRES_REPAIRING),
            connection.stateFlow.value
        )
        assertTrue(backgroundScope.isActive)
    }
}
