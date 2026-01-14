package net.flipper.bridge.connection.transport.ble.common

import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.InternalAPI
import io.ktor.utils.io.core.remaining
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlinx.io.Buffer
import kotlinx.io.Source
import net.flipper.core.busylib.ktx.common.FlipperDispatchers
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.info
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.coroutines.CoroutineContext

@OptIn(ExperimentalAtomicApi::class)
class ByteEndlessReadChannel(
    parent: CoroutineContext = FlipperDispatchers.default
) : ByteReadChannel, LogTagProvider {
    override val TAG = "ByteEndlessReadChannel"
    private val channel = Channel<ByteArray>(Int.MAX_VALUE)
    private val buffer = Buffer()

    private val _closedCause = AtomicReference<Throwable?>(null)
    override val closedCause: Throwable?
        get() = _closedCause.load()

    private val _isClosedForRead = AtomicBoolean(false)
    override val isClosedForRead: Boolean
        get() = _isClosedForRead.load()

    val job = Job(parent[Job])
    val coroutineContext = parent + job + CoroutineName("RawSourceChannel")

    @InternalAPI
    override val readBuffer: Source
        get() = buffer

    suspend fun onByteReceive(byteArray: ByteArray) {
        info { "Receive ${byteArray.size}" }
        channel.send(byteArray)
    }

    override suspend fun awaitContent(min: Int): Boolean {
        withContext(coroutineContext) {
            while (currentCoroutineContext().isActive && buffer.remaining < min) {
                info { "Buffer is ${buffer.remaining}, waiting for min bytes: $min" }
                val data = channel.receive()
                buffer.write(data)
                info { "Read ${data.size} bytes with min request $min, so current buffer size is ${buffer.remaining}" }
            }
        }

        return buffer.remaining >= min
    }

    override fun cancel(cause: Throwable?) {
        channel.close(cause)
        coroutineContext.cancel(cause as? CancellationException)
        _isClosedForRead.compareAndSet(expectedValue = false, newValue = true)
        _closedCause.compareAndSet(expectedValue = null, newValue = cause)
    }
}
