package net.flipper.bridge.connection.transport.ble.impl.logging

import org.slf4j.ILoggerFactory
import org.slf4j.Logger
import java.util.concurrent.ConcurrentHashMap

internal class CoreLogLoggerFactory : ILoggerFactory {
    private val loggers = ConcurrentHashMap<String, Logger>()

    override fun getLogger(name: String): Logger =
        loggers.getOrPut(name) { CoreLogLogger("BLE-$name") }
}
