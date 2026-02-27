package net.flipper.bsb.cloud.rest.api

import net.flipper.bsb.cloud.rest.model.BusyFirmwareDirectory

interface BusyFirmwareDirectoryApi {
    suspend fun getFirmwareDirectory(): Result<BusyFirmwareDirectory>
}
