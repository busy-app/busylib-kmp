package net.flipper.bridge.connection.transport.tcp.lan.impl.engine.token

import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.HttpHeaders
import io.ktor.http.URLProtocol
import io.ktor.http.path
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject
import net.flipper.bsb.auth.principal.api.BUSYLibPrincipalApi
import net.flipper.bsb.auth.principal.api.BUSYLibUserPrincipal
import net.flipper.bsb.cloud.api.BUSYLibHostApi
import net.flipper.core.ktor.getHttpClient
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

typealias ProxyTokenProviderFactory = (httpEngine: HttpClientEngine, deviceId: String) -> ProxyTokenProvider

@Inject
class ProxyTokenProvider(
    @Assisted httpEngine: HttpClientEngine,
    @Assisted private val deviceId: String,
    private val principalApi: BUSYLibPrincipalApi,
    private val hostApi: BUSYLibHostApi
) {
    private val httpClient = getHttpClient(httpEngine)
    private val mutex = Mutex()

    private var cachedToken: String? = null
    private var tokenExpireTime: Instant = Instant.DISTANT_PAST

    suspend fun getToken(failedToken: String? = null): String = mutex.withLock {
        if (failedToken == cachedToken || shouldUpdateToken()) {
            return@withLock generateToken()
        }

        return@withLock cachedToken!!
    }

    private fun shouldUpdateToken(): Boolean {
        if (cachedToken == null) {
            return true
        }
        if (tokenExpireTime < Clock.System.now()) {
            return true
        }

        return false
    }

    private suspend fun generateToken(): String {
        val principal = principalApi.getPrincipalFlow().value
        if (principal !is BUSYLibUserPrincipal.Token) {
            error("Not found user principal")
        }
        val requestTimestamp = Clock.System.now()
        val response = httpClient.post {
            url {
                host = hostApi.getHost().value
                protocol = URLProtocol.HTTPS
                port = 443

                path("/api/v0/bars/$deviceId/access-token")
            }
            setBody(ProxyTokenRequest())
            headers[HttpHeaders.Authorization] = "Bearer ${principal.token}"
        }.body<ProxyTokenResponse>()

        cachedToken = response.accessToken
        tokenExpireTime = requestTimestamp + response.expiresInSec.seconds

        return response.accessToken
    }
}