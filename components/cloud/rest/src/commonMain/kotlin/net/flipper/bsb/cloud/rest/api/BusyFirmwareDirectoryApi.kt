package net.flipper.bsb.cloud.rest.api

import net.flipper.bsb.cloud.rest.model.BusyFirmwareDirectory

interface BusyFirmwareDirectoryApi {
    suspend fun getFirmwareDirectory(): Result<BusyFirmwareDirectory>

    companion object {
        internal val URL = "https://update.flipperzero.one/busybar-firmware/directory.json"
    }
}