package net.flipper.bridge.connection.transport.ble.impl.ios.peripheral.queue

import kotlinx.atomicfu.atomic
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel

private const val OVERFLOW_RATIO = 0.75

class MeteredChannel<T>(
    private val capacity: Int
) {
    private val channel = Channel<T>(capacity)
    private val size = atomic(0)

    val fillRatio: Double
        get() = size.value.toDouble() / capacity

    val is75PercentFull: Boolean
        get() = size.value >= capacity * OVERFLOW_RATIO

    suspend fun send(value: T) {
        channel.send(value)
        size.incrementAndGet()
    }

    fun trySend(value: T): Boolean {
        val result = channel.trySend(value)
        if (result.isSuccess) {
            size.incrementAndGet()
            return true
        }
        return false
    }

    suspend fun receive(): T {
        val value = channel.receive()
        size.decrementAndGet()
        return value
    }

    fun asReceiveChannel(): ReceiveChannel<T> = channel
    fun asSendChannel(): SendChannel<T> = channel
}
