package net.flipper.bridge.connection.transport.tcp.common.engine

import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.darwin.Darwin

actual fun getPlatformEngineFactory(): HttpClientEngineFactory<*> {
    return Darwin
}
