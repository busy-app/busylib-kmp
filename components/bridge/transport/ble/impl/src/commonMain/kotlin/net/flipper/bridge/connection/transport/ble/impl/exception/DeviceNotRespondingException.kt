package net.flipper.bridge.connection.transport.ble.impl.exception

class DeviceNotRespondingException(operation: String) : Throwable("Device stopped answering $operation")
