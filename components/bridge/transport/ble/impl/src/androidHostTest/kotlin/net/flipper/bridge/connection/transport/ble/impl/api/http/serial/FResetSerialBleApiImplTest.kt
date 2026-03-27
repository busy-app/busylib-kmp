@file:Suppress("MagicNumber")

package net.flipper.bridge.connection.transport.ble.impl.api.http.serial

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import net.flipper.bridge.connection.transport.ble.impl.BleConstants.POLLING_RESET_INTERVAL
import no.nordicsemi.kotlin.ble.client.RemoteCharacteristic
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class FResetSerialBleApiImplTest {

    private fun createMockCharacteristic(vararg readValues: ByteArray): RemoteCharacteristic {
        val queue = ArrayDeque(readValues.toList())
        val fallback = readValues.lastOrNull() ?: byteArrayOf(0, 0, 0, 0)
        return mockk(relaxed = true) {
            coEvery { read() } answers {
                queue.removeFirstOrNull() ?: fallback
            }
        }
    }

    @Test
    fun GIVEN_reset_characteristic_available_WHEN_sut_created_THEN_polling_starts_eagerly_before_any_subscriber() =
        runTest {
            val characteristic = createMockCharacteristic(byteArrayOf(0x05, 0x00, 0x00, 0x00))
            val characteristicFlow = MutableStateFlow<RemoteCharacteristic?>(characteristic)

            FResetSerialBleApiImpl(
                scope = backgroundScope,
                resetCharacteristicFlow = characteristicFlow,
            )

            runCurrent()

            coVerify(atLeast = 1) { characteristic.read() }
        }

    @Test
    fun GIVEN_reset_characteristic_available_WHEN_sut_created_THEN_stateflow_is_populated_eagerly() =
        runTest {
            val characteristic = createMockCharacteristic(byteArrayOf(0x03, 0x00, 0x00, 0x00))
            val characteristicFlow = MutableStateFlow<RemoteCharacteristic?>(characteristic)

            val sut = FResetSerialBleApiImpl(
                scope = backgroundScope,
                resetCharacteristicFlow = characteristicFlow,
            )

            runCurrent()

            assertEquals(3, sut.getRequestCounterFlow().first())
        }

    @Test
    fun GIVEN_multiple_reset_counter_values_WHEN_polling_interval_passes_THEN_stateflow_updates_on_each_tick() =
        runTest {
            val characteristic = createMockCharacteristic(
                byteArrayOf(0x01, 0x00, 0x00, 0x00),
                byteArrayOf(0x02, 0x00, 0x00, 0x00),
                byteArrayOf(0x03, 0x00, 0x00, 0x00),
            )
            val characteristicFlow = MutableStateFlow<RemoteCharacteristic?>(characteristic)

            val sut = FResetSerialBleApiImpl(
                scope = backgroundScope,
                resetCharacteristicFlow = characteristicFlow,
            )

            runCurrent()
            assertEquals(1, sut.getRequestCounterFlow().first())

            advanceTimeBy(POLLING_RESET_INTERVAL)
            runCurrent()
            assertEquals(2, sut.getRequestCounterFlow().first())

            advanceTimeBy(POLLING_RESET_INTERVAL)
            runCurrent()
            assertEquals(3, sut.getRequestCounterFlow().first())
        }

    @Test
    fun GIVEN_characteristic_flow_initially_empty_WHEN_characteristic_is_emitted_THEN_polling_starts_immediately() =
        runTest {
            val characteristicFlow = MutableStateFlow<RemoteCharacteristic?>(null)

            val sut = FResetSerialBleApiImpl(
                scope = backgroundScope,
                resetCharacteristicFlow = characteristicFlow,
            )

            runCurrent()
            assertEquals(0, sut.getRequestCounterFlow().first())

            val characteristic = createMockCharacteristic(byteArrayOf(0x07, 0x00, 0x00, 0x00))
            characteristicFlow.value = characteristic
            runCurrent()

            assertEquals(7, sut.getRequestCounterFlow().first())
        }

    @Test
    fun GIVEN_characteristic_flow_initially_empty_WHEN_sut_created_THEN_stateflow_starts_with_zero() =
        runTest {
            val characteristicFlow = MutableStateFlow<RemoteCharacteristic?>(null)

            val sut = FResetSerialBleApiImpl(
                scope = backgroundScope,
                resetCharacteristicFlow = characteristicFlow,
            )

            runCurrent()

            assertEquals(0, sut.getRequestCounterFlow().first())
        }
}
