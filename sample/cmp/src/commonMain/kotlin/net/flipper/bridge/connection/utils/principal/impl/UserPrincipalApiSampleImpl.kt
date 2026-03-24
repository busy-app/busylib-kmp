package net.flipper.bridge.connection.utils.principal.impl

import com.russhwolf.settings.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.flipper.bsb.auth.principal.api.BUSYLibPrincipalApi
import net.flipper.bsb.auth.principal.api.BUSYLibUserPrincipal
import net.flipper.bsb.auth.principal.api.BUSYLibUserPrincipalToken
import net.flipper.bsb.cloud.api.BUSYLibHostApi
import net.flipper.busylib.core.wrapper.wrap
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlin.uuid.Uuid

private const val KEY_ACCESS_TOKEN = "auth_access_token"
private const val KEY_REFRESH_TOKEN = "auth_refresh_token"
private const val KEY_EXPIRES_AT_EPOCH_SEC = "auth_expires_at_epoch_sec"
private const val KEY_USER_ID = "auth_user_id"
private const val KEY_EMAIL = "auth_email"

class UserPrincipalApiSampleImpl(
    private val scope: CoroutineScope,
    private val hostApi: BUSYLibHostApi,
    private val settings: Settings
) : BUSYLibPrincipalApi {
    private val principalFlow = MutableStateFlow<BUSYLibUserPrincipal>(BUSYLibUserPrincipal.Loading)
    private val mutex = Mutex()

    private var cachedAccessToken: String? = null
    private var cachedRefreshToken: String? = null
    private var tokenExpireTime: Instant = Instant.DISTANT_PAST
    private var cachedUserId: Uuid? = null
    private var cachedEmail: String? = null

    val authStateFlow: StateFlow<BUSYLibUserPrincipal> = principalFlow.asStateFlow()

    init {
        scope.launch {
            loadPersistedTokens()
        }
    }

    override fun getPrincipalFlow() = principalFlow.wrap()

    suspend fun login(email: String, password: String): Result<Unit> = runCatching {
        val host = hostApi.getHost().value
        val tokens = SampleAuthClient.signIn(host, email, password)

        cachedAccessToken = tokens.accessToken
        cachedRefreshToken = tokens.refreshToken
        tokenExpireTime = Clock.System.now() + tokens.expiresInSec.seconds

        val me = SampleAuthClient.getMe(host, tokens.accessToken)
        cachedUserId = me.uid
        cachedEmail = me.email

        saveTokens()
        principalFlow.emit(createTokenPrincipal(me.uid))
    }

    fun logout() {
        scope.launch {
            val host = hostApi.getHost().value
            val token = cachedAccessToken
            if (token != null) {
                runCatching { SampleAuthClient.logout(host, token) }
            }
            clearTokens()
            principalFlow.emit(BUSYLibUserPrincipal.Empty)
        }
    }

    private suspend fun loadPersistedTokens() {
        val accessToken = settings.getStringOrNull(KEY_ACCESS_TOKEN)
        val refreshToken = settings.getStringOrNull(KEY_REFRESH_TOKEN)
        val expiresAtSec = settings.getLongOrNull(KEY_EXPIRES_AT_EPOCH_SEC)
        val userId = settings.getStringOrNull(KEY_USER_ID)

        if (accessToken != null && refreshToken != null && userId != null && expiresAtSec != null) {
            cachedAccessToken = accessToken
            cachedRefreshToken = refreshToken
            tokenExpireTime = Instant.fromEpochSeconds(expiresAtSec)
            cachedUserId = Uuid.parse(userId)
            cachedEmail = settings.getStringOrNull(KEY_EMAIL)

            principalFlow.emit(createTokenPrincipal(cachedUserId!!))
        } else {
            principalFlow.emit(BUSYLibUserPrincipal.Empty)
        }
    }

    private fun saveTokens() {
        settings.putString(KEY_ACCESS_TOKEN, cachedAccessToken ?: return)
        settings.putString(KEY_REFRESH_TOKEN, cachedRefreshToken ?: return)
        settings.putLong(KEY_EXPIRES_AT_EPOCH_SEC, tokenExpireTime.epochSeconds)
        settings.putString(KEY_USER_ID, cachedUserId?.toString() ?: return)
        cachedEmail?.let { settings.putString(KEY_EMAIL, it) }
    }

    private fun clearTokens() {
        cachedAccessToken = null
        cachedRefreshToken = null
        tokenExpireTime = Instant.DISTANT_PAST
        cachedUserId = null
        cachedEmail = null
        settings.remove(KEY_ACCESS_TOKEN)
        settings.remove(KEY_REFRESH_TOKEN)
        settings.remove(KEY_EXPIRES_AT_EPOCH_SEC)
        settings.remove(KEY_USER_ID)
        settings.remove(KEY_EMAIL)
    }

    private fun shouldUpdateToken(): Boolean {
        return tokenExpireTime < Clock.System.now()
    }

    private suspend fun refreshTokensInternal(): String {
        val refreshToken = cachedRefreshToken ?: error("No refresh token")
        val host = hostApi.getHost().value
        val tokens = SampleAuthClient.refreshTokens(host, refreshToken)

        cachedAccessToken = tokens.accessToken
        if (tokens.refreshToken != null) {
            cachedRefreshToken = tokens.refreshToken
        }
        tokenExpireTime = Clock.System.now() + tokens.expiresInSec.seconds
        saveTokens()
        return tokens.accessToken
    }

    private fun createTokenPrincipal(userId: Uuid): BUSYLibUserPrincipal.Token {
        return BUSYLibUserPrincipalToken(userId) { failedToken ->
            mutex.withLock {
                val accessToken = cachedAccessToken ?: error("No access token")
                if (failedToken == accessToken || shouldUpdateToken()) {
                    return@withLock refreshTokensInternal()
                }
                return@withLock accessToken
            }
        }
    }
}
