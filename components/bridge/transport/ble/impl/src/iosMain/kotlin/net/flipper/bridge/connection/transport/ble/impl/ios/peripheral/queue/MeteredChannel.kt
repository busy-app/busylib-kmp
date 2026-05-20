package net.flipper.bridge.connection.transport.ble.impl.ios.peripheral.queue

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.decrementAndFetch
import kotlin.concurrent.atomics.incrementAndFetch

private const val OVERFLOW_RATIO = 0.75

@OptIn(ExperimentalAtomicApi::class)
class MeteredChannel<T>(
    private val capacity: Int
) {
    private val channel = Channel<T>(capacity)
    private val size = AtomicInt(0)

    val fillRatio: Double
        get() = size.load().toDouble() / capacity

    val is75PercentFull: Boolean
        get() = size.load() >= capacity * OVERFLOW_RATIO

    suspend fun send(value: T) {
        channel.send(value)
        size.incrementAndFetch()
    }

    fun trySend(value: T): Boolean {
        val result = channel.trySend(value)
        if (result.isSuccess) {
            size.incrementAndFetch()
            return true
        }
        return false
    }

    suspend fun receive(): T {
        val value = channel.receive()
        size.decrementAndFetch()
        return value
    }

    fun asReceiveChannel(): ReceiveChannel<T> = channel
    fun asSendChannel(): SendChannel<T> = channel
}
