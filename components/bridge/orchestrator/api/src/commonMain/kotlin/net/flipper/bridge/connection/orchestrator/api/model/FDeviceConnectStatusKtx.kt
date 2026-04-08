package net.flipper.bridge.connection.orchestrator.api.model

val FDeviceConnectStatus.deviceOrNull
    get() = when (this) {
        is FDeviceConnectStatus.Connected -> device
        is FDeviceConnectStatus.Connecting -> device
        is FDeviceConnectStatus.Disconnected -> device
        is FDeviceConnectStatus.Disconnecting -> device
    }
