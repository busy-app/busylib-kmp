package net.flipper.bridge.connection.transport.ble.impl.api.http.serial

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import net.flipper.bridge.connection.transport.ble.impl.api.serial.FSerialUnsafeApiImpl
import no.nordicsemi.kotlin.ble.client.RemoteCharacteristic
import no.nordicsemi.kotlin.ble.core.CharacteristicProperty
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.minutes

private val READINESS_PROBE = 1.minutes

class FSerialUnsafeApiImplTest {

    private fun createRxCharacteristic(
        subscriptionAcknowledged: CompletableDeferred<Unit>
    ): RemoteCharacteristic {
        val characteristic = mockk<RemoteCharacteristic>(relaxed = true)
        every { characteristic.properties } returns setOf(CharacteristicProperty.NOTIFY)
        every { characteristic.subscribe(any()) } answers {
            val onSubscription = firstArg<suspend (RemoteCharacteristic) -> Unit>()
            flow {
                subscriptionAcknowledged.await()
                onSubscription.invoke(characteristic)
                awaitCancellation()
            }
        }
        return characteristic
    }

    private fun createSut(scope: CoroutineScope, characteristic: RemoteCharacteristic): FSerialUnsafeApiImpl {
        return FSerialUnsafeApiImpl(
            rxCharacteristic = MutableStateFlow(characteristic),
            txCharacteristic = MutableStateFlow(characteristic),
            scope = scope,
            onResetServices = {},
            context = mockk<Context>(relaxed = true)
        )
    }

    @Test
    fun GIVEN_rx_subscription_not_acknowledged_WHEN_readiness_awaited_THEN_caller_keeps_waiting() = runTest {
        val sut = createSut(backgroundScope, createRxCharacteristic(CompletableDeferred()))

        val readiness = withTimeoutOrNull(READINESS_PROBE) { sut.awaitReady() }

        assertNull(readiness, "Reported readiness before the device acknowledged the subscription")
    }

    @Test
    fun GIVEN_rx_subscription_acknowledged_WHEN_readiness_awaited_THEN_caller_resumes() = runTest {
        val subscriptionAcknowledged = CompletableDeferred<Unit>()
        val sut = createSut(backgroundScope, createRxCharacteristic(subscriptionAcknowledged))

        subscriptionAcknowledged.complete(Unit)
        val readiness = withTimeoutOrNull(READINESS_PROBE) { sut.awaitReady() }

        assertNotNull(readiness, "Never reported readiness after the subscription was acknowledged")
    }
}
