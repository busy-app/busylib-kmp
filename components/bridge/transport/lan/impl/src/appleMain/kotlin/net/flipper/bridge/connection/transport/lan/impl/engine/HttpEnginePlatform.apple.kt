package net.flipper.bridge.connection.transport.lan.impl.engine

import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.darwin.Darwin

actual fun getPlatformEngineFactory(): HttpClientEngineFactory<*> {
    return Darwin
}
