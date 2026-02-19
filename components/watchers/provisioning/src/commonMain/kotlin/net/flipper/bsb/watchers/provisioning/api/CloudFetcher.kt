package net.flipper.bsb.watchers.provisioning.api

import com.flipperdevices.core.network.BUSYLibNetworkStateApi
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.HttpHeaders
import io.ktor.http.URLProtocol
import io.ktor.http.path
import io.ktor.util.Hash.combine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import me.tatarka.inject.annotations.Inject
import net.flipper.bridge.connection.config.api.model.BUSYBar
import net.flipper.bsb.auth.principal.api.BUSYLibPrincipalApi
import net.flipper.bsb.auth.principal.api.BUSYLibUserPrincipal
import net.flipper.bsb.cloud.api.BUSYLibHostApi
import net.flipper.bsb.watchers.provisioning.api.model.CloudProvisioningBar
import net.flipper.bsb.watchers.provisioning.api.model.CloudProvisioningResponse
import net.flipper.core.busylib.ktx.common.runSuspendCatching
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.error
import net.flipper.core.ktor.getHttpClient

@Inject
class CloudFetcher(
    private val principalApi: BUSYLibPrincipalApi,
    private val hostApi: BUSYLibHostApi,
    private val networkStateApi: BUSYLibNetworkStateApi
) : LogTagProvider {
    override val TAG = "CloudFetcher"
    private val httpClient = getHttpClient()

    fun getBarsFlow(): Flow<List<CloudProvisioningBar>> {
        return combine(
            principalApi.getPrincipalFlow(),
            hostApi.getHost(),
            networkStateApi.isNetworkAvailableFlow
        ) { principal, host, isNetworkAvailable ->
            if (isNetworkAvailable && principal is BUSYLibUserPrincipal.Token) {
                getBarsFlow(principal, host)
            } else emptyFlow()
        }.flatMapLatest { it }
    }

    private fun getBarsFlow(
        principal: BUSYLibUserPrincipal.Token,
        host: String
    ): Flow<List<CloudProvisioningBar>> = flow {
        runSuspendCatching {
            getBars(principal, host)
        }.onSuccess { emit(it) }
            .onFailure { error(it) { "Failed to fetch bars" } }
    }

    private suspend fun getBars(
        principal: BUSYLibUserPrincipal.Token,
        busyHost: String
    ): List<CloudProvisioningBar> {
        val response = httpClient.get {
            url {
                host = busyHost
                protocol = URLProtocol.HTTPS
                port = 443

                path("/api/v0/bars/list")
            }
            headers[HttpHeaders.Authorization] = "Bearer ${principal.token}"
        }.body<CloudProvisioningResponse>()

        return response.success.bars
    }
}