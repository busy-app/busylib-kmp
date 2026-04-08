package net.flipper.bridge.connection.transport.tcp.lan.impl.engine.token

import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import net.flipper.bsb.auth.principal.api.BUSYLibPrincipalApi
import net.flipper.bsb.auth.principal.api.BUSYLibUserPrincipal
import net.flipper.bsb.cloud.rest.api.BusyCloudAccessTokenApi
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlin.uuid.Uuid

@AssistedInject
class ProxyTokenProvider(
    @Assisted private val deviceId: Uuid,
    private val principalApi: BUSYLibPrincipalApi,
    private val accessTokenApi: BusyCloudAccessTokenApi
) {
    private val mutex = Mutex()

    private var cachedToken: String? = null
    private var tokenExpireTime: Instant = Instant.DISTANT_PAST

    suspend fun getToken(failedToken: String? = null): String = mutex.withLock {
        if (failedToken == cachedToken || shouldUpdateToken()) {
            return@withLock generateToken()
        }

        return@withLock cachedToken ?: error("Token should not be null")
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

    @Suppress("MagicNumber")
    private suspend fun generateToken(): String {
        val principal = principalApi.getPrincipalFlow()
            .filterNot { it is BUSYLibUserPrincipal.Loading }
            .first()
        if (principal !is BUSYLibUserPrincipal.Token) {
            error("Not found user principal")
        }
        val requestTimestamp = Clock.System.now()
        val response = accessTokenApi.generateAccessToken(principal, deviceId).getOrThrow()

        cachedToken = response.accessToken
        tokenExpireTime = requestTimestamp + response.expiresInSec.seconds

        return response.accessToken
    }

    @AssistedFactory
    fun interface Factory {
        fun create(deviceId: Uuid): ProxyTokenProvider
    }
}
