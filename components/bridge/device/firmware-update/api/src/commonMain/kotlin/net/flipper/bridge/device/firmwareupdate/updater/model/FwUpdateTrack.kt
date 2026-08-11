package net.flipper.bridge.device.firmwareupdate.updater.model

/**
 * A firmware update tracked by the library, keyed by the device's uniqueId in
 * `FirmwareUpdaterApi.updatingDevices`. A device stays tracked while its update keeps
 * running anywhere — including autonomously on the device itself after the app
 * disconnected from it.
 */
data class FwUpdateTrack(
    val installType: InstallType,
    /**
     * The tracked device stopped being the connected one at least once since the
     * install started. Distinguishes a genuine post-reconnect "update is offered
     * again" (the install did not survive) from the ReadyToUpdate replay at
     * install start.
     */
    val targetOutOfSight: Boolean = false,
) {

    enum class InstallType {
        /** Cloud/BLE: the install RPC is sent, the device downloads and installs itself */
        DEFAULT,

        /** LAN: the app downloads the firmware and uploads it, then the device installs */
        LAN,
    }
}
