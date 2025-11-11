package net.flipper.bridge.connection.transport.ble.common

import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.InternalAPI
import io.ktor.utils.io.core.remaining
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withContext
import kotlinx.io.Buffer
import kotlinx.io.Source
import net.flipper.core.busylib.ktx.common.FlipperDispatchers
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.info
import kotlin.coroutines.CoroutineContext

class ByteEndlessReadChannel(
    parent: CoroutineContext = FlipperDispatchers.default
) : ByteReadChannel, LogTagProvider {
    override val TAG = "ByteEndlessReadChannel"
    private val channel = Channel<ByteArray>(Int.MAX_VALUE)
    private val buffer = Buffer()

    override val closedCause: Throwable? = null

    override val isClosedForRead = false

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
            while (buffer.remaining < min) {
                info { "Buffer is ${buffer.remaining}, waiting for min bytes: $min" }
                val data = channel.receive()
                buffer.write(data)
                info { "Read ${data.size} bytes with min request $min, so current buffer size is ${buffer.remaining}" }
            }
        }

        return buffer.remaining >= min
    }

    override fun cancel(cause: Throwable?) {
        // This channel can't be clouse
    }
}
