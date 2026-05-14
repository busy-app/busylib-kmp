package net.flipper.core.ktor

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.engine.darwin.DarwinClientEngineConfig

/**
 * Cap NSURLSession's HTTP/1.1 keep-alive pool per host. Combined with the
 * dedicated [FAppleLanConnectionMonitor] probe and the WebSocket task, this
 * bounds total TCP sockets per BUSY Bar to roughly 4.
 */
private const val MAX_CONNECTIONS_PER_HOST: Long = 3

actual fun getPlatformEngineFactory(): HttpClientEngineFactory<*> = ConfiguredDarwinFactory

private object ConfiguredDarwinFactory : HttpClientEngineFactory<DarwinClientEngineConfig> {
    override fun create(block: DarwinClientEngineConfig.() -> Unit): HttpClientEngine {
        return Darwin.create {
            block()
            configureSession {
                HTTPMaximumConnectionsPerHost = MAX_CONNECTIONS_PER_HOST
            }
        }
    }
}
