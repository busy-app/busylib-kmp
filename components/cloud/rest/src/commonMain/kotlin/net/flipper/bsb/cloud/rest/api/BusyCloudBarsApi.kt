package net.flipper.bsb.cloud.rest.api

import kotlin.uuid.Uuid

interface BusyCloudBarsApi {
    suspend fun unlinkBusyBar(uuid: Uuid): Result<Unit>
}
