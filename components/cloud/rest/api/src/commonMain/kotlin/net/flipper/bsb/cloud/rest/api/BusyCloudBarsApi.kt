package net.flipper.bsb.cloud.rest.api

import net.flipper.bsb.auth.principal.api.BUSYLibUserPrincipal
import net.flipper.bsb.cloud.rest.model.BusyCloudBar
import kotlin.uuid.Uuid

interface BusyCloudBarsApi {
    suspend fun unlinkBusyBar(principal: BUSYLibUserPrincipal.Token, uuid: Uuid): Result<Unit>

    suspend fun linkBusyBar(principal: BUSYLibUserPrincipal.Token, pin: String): Result<Unit>

    suspend fun getBarsList(principal: BUSYLibUserPrincipal.Token): Result<List<BusyCloudBar>>
}
