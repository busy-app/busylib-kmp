package net.flipper.bridge.connection.transport.ble.impl

import kotlin.time.Duration.Companion.seconds

object BleConstants {
    val CONNECT_TIME = 10.seconds
    val DISCONNECT_TIME = 10.seconds

    val POLLING_RESET_INTERVAL = 5.seconds
}
