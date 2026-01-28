package net.flipper.bridge.connection.feature.smarthome.model

sealed interface SmartHomeState {
    data object Disconnected : SmartHomeState
    data object Connected : SmartHomeState
}
