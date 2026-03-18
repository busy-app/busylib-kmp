@file:Suppress("MagicNumber")

package net.flipper.bridge.connection.transport.ble.impl.api.http.serial

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
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
    fun `polling starts eagerly before any subscriber`() = runTest {
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
    fun `state flow value is populated eagerly without calling getRequestCounterStateFlow`() = runTest {
        val characteristic = createMockCharacteristic(byteArrayOf(0x03, 0x00, 0x00, 0x00))
        val characteristicFlow = MutableStateFlow<RemoteCharacteristic?>(characteristic)

        val sut = FResetSerialBleApiImpl(
            scope = backgroundScope,
            resetCharacteristicFlow = characteristicFlow,
        )

        runCurrent()

        assertEquals(3, sut.getRequestCounterStateFlow().value)
    }

    @Test
    fun `polling continues at POLLING_RESET_INTERVAL`() = runTest {
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
        assertEquals(1, sut.getRequestCounterStateFlow().value)

        advanceTimeBy(POLLING_RESET_INTERVAL)
        runCurrent()
        assertEquals(2, sut.getRequestCounterStateFlow().value)

        advanceTimeBy(POLLING_RESET_INTERVAL)
        runCurrent()
        assertEquals(3, sut.getRequestCounterStateFlow().value)
    }

    @Test
    fun `characteristic flow emission triggers immediate polling`() = runTest {
        val characteristicFlow = MutableStateFlow<RemoteCharacteristic?>(null)

        val sut = FResetSerialBleApiImpl(
            scope = backgroundScope,
            resetCharacteristicFlow = characteristicFlow,
        )

        runCurrent()
        assertEquals(0, sut.getRequestCounterStateFlow().value)

        val characteristic = createMockCharacteristic(byteArrayOf(0x07, 0x00, 0x00, 0x00))
        characteristicFlow.value = characteristic
        runCurrent()

        assertEquals(7, sut.getRequestCounterStateFlow().value)
    }

    @Test
    fun `initial state flow value is zero before characteristic emits`() = runTest {
        val characteristicFlow = MutableStateFlow<RemoteCharacteristic?>(null)

        val sut = FResetSerialBleApiImpl(
            scope = backgroundScope,
            resetCharacteristicFlow = characteristicFlow,
        )

        runCurrent()

        assertEquals(0, sut.getRequestCounterStateFlow().value)
    }
}
