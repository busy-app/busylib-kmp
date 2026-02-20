package net.flipper.bsb.cloud.rest.api

import net.flipper.bsb.auth.principal.api.BUSYLibUserPrincipal
import kotlin.uuid.Uuid

interface BusyCloudBarsApi {
    suspend fun unlinkBusyBar(principal: BUSYLibUserPrincipal.Token, uuid: Uuid): Result<Unit>
}
