package net.flipper.bsb.cloud.rest.api

import net.flipper.bsb.auth.principal.api.BUSYLibUserPrincipal
import net.flipper.bsb.cloud.rest.model.BusyCloudAccessTokenResponse
import kotlin.uuid.Uuid

interface BusyCloudAccessTokenApi {
    suspend fun generateAccessToken(
        principal: BUSYLibUserPrincipal.Token,
        deviceId: Uuid
    ): Result<BusyCloudAccessTokenResponse>
}
