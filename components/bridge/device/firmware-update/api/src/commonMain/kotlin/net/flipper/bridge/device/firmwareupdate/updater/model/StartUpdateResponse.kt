package net.flipper.bridge.device.firmwareupdate.updater.model

sealed interface StartUpdateResponse {
    data object LowBattery : StartUpdateResponse
    data object Success : StartUpdateResponse
    class Failure(val t: Throwable) : StartUpdateResponse
}
