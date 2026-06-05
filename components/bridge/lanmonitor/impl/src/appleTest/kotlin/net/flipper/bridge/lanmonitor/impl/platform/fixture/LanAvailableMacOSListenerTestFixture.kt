package net.flipper.bridge.lanmonitor.impl.platform.fixture

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import net.flipper.bridge.lanmonitor.impl.platform.LanAvailableMacOSListener
import net.flipper.bridge.lanmonitor.impl.platform.fixture.tcp.PosixTcpServer
import platform.Foundation.NSThread

/**
 * Wires up a [LanAvailableMacOSListener] against a local [PosixTcpServer] and a [RecordingLanAvailableListener].
 *
 * The listener starts monitoring in its `init`, so the recorder must subscribe to the (zero-replay)
 * availability flow before the first emission. [createListener] launches the collector and awaits its
 * subscription before returning, so the leading `false` (preparing/waiting) is never missed.
 *
 * The listener has no public stop API. Its monitoring runs on [monitorScope] — owned by the fixture and
 * independent of the test's coroutine scope — so [dispose] can tear it down without leaving the test's
 * `runTest`/`withContext` waiting on the never-ending restart loop.
 */
class LanAvailableMacOSListenerTestFixture {
    val server: PosixTcpServer = PosixTcpServer()
    private val monitorScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    suspend fun createListener(
        host: String = "127.0.0.1",
        port: Int = server.port,
    ): RecordingLanAvailableListener {
        val recorder = RecordingLanAvailableListener()
        val listener = LanAvailableMacOSListener(
            globalScope = monitorScope,
            host = host,
            port = port,
        )

        val subscribed = CompletableDeferred<Unit>()
        monitorScope.launch {
            listener.getLanAvailableFlow()
                .onStart { subscribed.complete(Unit) }
                .collect { value -> recorder.record(value) }
        }
        subscribed.await()

        return recorder
    }

    fun dispose() {
        monitorScope.cancel()
        NSThread.sleepForTimeInterval(1.0)
        server.stop()
    }
}
