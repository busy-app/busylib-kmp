package net.flipper.bsb.watchers.provisioning.fakes

import net.flipper.bsb.auth.principal.api.BUSYLibUserPrincipal
import net.flipper.bsb.cloud.rest.api.BusyCloudBarsApi
import net.flipper.bsb.cloud.rest.model.BusyCloudBar
import kotlin.uuid.Uuid

internal class FakeCloudBarsApi(
    private val barsResult: Result<List<BusyCloudBar>>
) : BusyCloudBarsApi {
    var callCount: Int = 0
        private set

    override suspend fun getBarsList(
        principal: BUSYLibUserPrincipal.Token
    ): Result<List<BusyCloudBar>> {
        callCount++
        return barsResult
    }

    override suspend fun unlinkBusyBar(
        principal: BUSYLibUserPrincipal.Token,
        uuid: Uuid
    ): Result<Unit> = error("Not used in test")

    override suspend fun linkBusyBar(
        principal: BUSYLibUserPrincipal.Token,
        pin: String
    ): Result<Unit> = error("Not used in test")
}
