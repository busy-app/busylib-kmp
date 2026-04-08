@file:Suppress("ClockNowForbiddenRule")

package net.flipper.bridge.connection.utils.principal.impl.token

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Clock
import kotlin.time.Instant

data class AuthTokens(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Instant
)

class AuthTokenProvider(
    initialAuthTokens: AuthTokens,
    private val saveNewTokens: suspend (AuthTokens) -> Unit,
    private val exchangeTokens: suspend (refreshToken: String) -> AuthTokens
) {
    private val mutex = Mutex()
    private var cachedTokens = initialAuthTokens
    private var tokenExpireTime: Instant = initialAuthTokens.expiresIn

    suspend fun getToken(failedToken: String? = null): String = mutex.withLock {
        val accessToken = cachedTokens.accessToken
        if (failedToken == accessToken || shouldUpdateToken()) {
            return@withLock generateToken()
        }

        return@withLock accessToken
    }

    private fun shouldUpdateToken(): Boolean {
        return tokenExpireTime < Clock.System.now()
    }

    private suspend fun generateToken(): String {
        cachedTokens = exchangeTokens(cachedTokens.refreshToken)
        saveNewTokens(cachedTokens)
        tokenExpireTime = cachedTokens.expiresIn
        return cachedTokens.accessToken
    }
}
