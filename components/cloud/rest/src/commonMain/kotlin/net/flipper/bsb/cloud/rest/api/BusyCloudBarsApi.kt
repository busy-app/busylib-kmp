package net.flipper.bsb.cloud.rest.api

interface BusyCloudBarsApi {
    suspend fun unlinkBusyBar(uuid: String): Result<Unit>
}
