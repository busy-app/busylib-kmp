package net.flipper.bridge.device.firmwareupdate.updater.model

internal enum class FwInstallRequest {
    /**
     * Nothing was requested, whatever the device reports is the whole truth
     */
    NONE,

    /**
     * The install RPC is in flight, BSB has not accepted it yet
     */
    REQUESTED,

    /**
     * BSB accepted the install. The session lasts until the device reboots into the new firmware
     */
    STARTED
}
