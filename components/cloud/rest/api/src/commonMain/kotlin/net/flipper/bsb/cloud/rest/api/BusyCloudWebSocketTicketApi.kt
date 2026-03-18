package net.flipper.bsb.cloud.rest.api

import net.flipper.bsb.auth.principal.api.BUSYLibUserPrincipal

interface BusyCloudWebSocketTicketApi {
    suspend fun getTicketToken(principal: BUSYLibUserPrincipal.Token): Result<String>
}
