package net.flipper.bridge.device.firmwareupdate.updater.model

/**
 * Result of [net.flipper.bridge.device.firmwareupdate.updater.api.FirmwareUpdaterApi.checkForUpdatesAndAwaitResult]
 */
enum class FwUpdateCheckResult {
    NO_UPDATE_AVAILABLE,
    UPDATE_AVAILABLE,
    BATTERY_LOW,

    /**
     * The check request failed, the device could not check, or no result arrived in time
     */
    FAILED
}
