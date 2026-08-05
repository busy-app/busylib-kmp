package net.flipper.bridge.device.firmwareupdate.updater.model

sealed interface StartUpdateResponse {
    data object BatteryLow : StartUpdateResponse
    data object Success : StartUpdateResponse
    class Failure(val t: Throwable) : StartUpdateResponse
}
