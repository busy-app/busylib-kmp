package net.flipper.bridge.device.firmwareupdate.updater.model

sealed class FirmwareUpdateCheckError(message: String) : Exception(message) {
    class RpcUnavailable : FirmwareUpdateCheckError("RPC feature is unavailable")
    class BatteryLow : FirmwareUpdateCheckError("Battery is too low to check for updates")
    class CouldNotCheckUpdate : FirmwareUpdateCheckError("Device could not check for updates")
    class Timeout : FirmwareUpdateCheckError("No update check result arrived in time")
}
