package net.flipper.bsb.cloud.rest.api

interface BusyCloudRestApi {
    val barsApi: BusyCloudBarsApi
    val busyFirmwareDirectoryApi: BusyFirmwareDirectoryApi
}
