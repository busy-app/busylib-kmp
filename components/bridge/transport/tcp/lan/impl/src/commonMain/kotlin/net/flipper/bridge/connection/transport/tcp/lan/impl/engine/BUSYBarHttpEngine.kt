package net.flipper.bridge.connection.transport.tcp.lan.impl.engine

import com.mayakapps.rwmutex.ReadWriteMutex
import com.mayakapps.rwmutex.withReadLock
import com.mayakapps.rwmutex.withWriteLock
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.HttpClientEngineBase
import io.ktor.client.engine.HttpClientEngineCapability
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.client.request.takeFrom
import io.ktor.utils.io.InternalAPI
import kotlinx.coroutines.runBlocking
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.warn

class BUSYBarHttpEngine(
    private val delegate: HttpClientEngine,
    private val host: String
) : HttpClientEngineBase("busy-bar"), LogTagProvider {
    override val TAG = "BUSYBarHttpEngine"
    override val config: HttpClientEngineConfig = delegate.config
    private val rwMutex = ReadWriteMutex()
    private var closed = false

    @InternalAPI
    override suspend fun execute(data: HttpRequestData): HttpResponseData {
        return rwMutex.withReadLock {
            check(closed.not()) { "Engine is closed" }
            val newRequest = HttpRequestBuilder().takeFrom(data)

            newRequest.url.host = host

            val newRequestData = newRequest.build()

            return@withReadLock delegate.execute(newRequestData)
        }
    }

    override fun close() {
        runBlocking {
            if (rwMutex.writeMutex.isLocked) {
                warn { "Mutex is locked, so close will be wait until request finished" }
            }
            rwMutex.withWriteLock {
                if (closed) {
                    return@withWriteLock
                }
                delegate.close()
                super.close()
                closed = true
            }
        }
    }

    override val supportedCapabilities: Set<HttpClientEngineCapability<*>>
        get() = delegate.supportedCapabilities
}
