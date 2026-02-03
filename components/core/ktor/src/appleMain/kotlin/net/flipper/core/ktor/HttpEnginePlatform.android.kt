package net.flipper.core.ktor

import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.darwin.Darwin

actual fun getPlatformEngineFactory(): HttpClientEngineFactory<*> {
    return Darwin
}
