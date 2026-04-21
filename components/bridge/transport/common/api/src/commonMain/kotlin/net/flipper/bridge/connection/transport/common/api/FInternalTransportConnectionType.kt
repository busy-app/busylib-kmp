package net.flipper.bridge.connection.transport.common.api

enum class FInternalTransportConnectionType(val priority: Int) {
    LAN(2),
    CLOUD(1),
    BLE(0),
    MOCK(-1)
}
