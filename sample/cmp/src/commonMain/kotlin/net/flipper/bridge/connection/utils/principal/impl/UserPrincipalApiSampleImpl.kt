@file:Suppress("ClockNowForbiddenRule")

package net.flipper.bridge.connection.utils.principal.impl

import com.russhwolf.settings.Settings
import com.russhwolf.settings.get
import com.russhwolf.settings.serialization.decodeValue
import com.russhwolf.settings.serialization.encodeValue
import com.russhwolf.settings.serialization.removeValue
import com.russhwolf.settings.set
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.flipper.bridge.connection.utils.principal.impl.model.UserData
import net.flipper.bridge.connection.utils.principal.impl.token.AuthTokenProvider
import net.flipper.bridge.connection.utils.principal.impl.token.AuthTokens
import net.flipper.bsb.auth.principal.api.BUSYLibPrincipalApi
import net.flipper.bsb.auth.principal.api.BUSYLibUserPrincipal
import net.flipper.bsb.cloud.api.BUSYLibHostApi
import net.flipper.busylib.core.wrapper.wrap
import net.flipper.core.busylib.log.info
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.Uuid

private const val KEY_USER = "auth_user_data"

class UserPrincipalApiSampleImpl(
    private val scope: CoroutineScope,
    private val hostApi: BUSYLibHostApi,
    private val settings: Settings
) : BUSYLibPrincipalApi {
    private val principalFlow = MutableStateFlow<BUSYLibUserPrincipal>(BUSYLibUserPrincipal.Loading)
    private val mutex = Mutex()

    val authStateFlow: StateFlow<BUSYLibUserPrincipal> = principalFlow.asStateFlow()

    init {
        scope.launch {
            val userData = loadUserData()
            onNewUserData(userData)
        }
    }

    override fun getPrincipalFlow() = principalFlow.wrap()

    private suspend fun onNewUserData(userData: UserData?) {
        if (userData == null) {
            principalFlow.emit(BUSYLibUserPrincipal.Empty)
            return
        }
        val authTokens = AuthTokens(
            accessToken = userData.accessToken,
            refreshToken = userData.refreshToken,
            expiresIn = userData.expiresIn
        )
        val tokenProvider = AuthTokenProvider(
            initialAuthTokens = authTokens,
            saveNewTokens = { tokens ->
                set(
                    tokens = tokens,
                    email = userData.email,
                    userId = userData.userId
                )
            },
            exchangeTokens = ::exchangeTokens
        )
        val loggedUser = UserPrincipalImpl(
            tokenProvider = tokenProvider,
            email = userData.email,
            userId = userData.userId
        )
        principalFlow.emit(loggedUser)
    }

    suspend fun login(email: String, password: String): Result<Unit> = runCatching {
        val host = hostApi.getHost().value
        val tokens = SampleAuthClient.signIn(host, email, password)
        val me = SampleAuthClient.getMe(host, tokens.accessToken)

        val userData = UserData(
            accessToken = tokens.accessToken,
            refreshToken = tokens.refreshToken,
            expiresIn = Clock.System.now() + tokens.expiresInSec.seconds,
            email = me.email,
            userId = me.uid
        )
        saveUserData(userData)
        onNewUserData(userData)
    }

    fun logout() {
        scope.launch {
            val host = hostApi.getHost().value
            val principal = principalFlow.value
            if (principal is UserPrincipalImpl) {
                runCatching {
                    val token = principal.getToken(null)
                    SampleAuthClient.logout(host, token)
                }
            }
            clearUserData()
            principalFlow.emit(BUSYLibUserPrincipal.Empty)
        }
    }

    private suspend fun exchangeTokens(refreshToken: String): AuthTokens {
        val host = hostApi.getHost().value
        val tokens = SampleAuthClient.refreshTokens(host, refreshToken)
        return AuthTokens(
            accessToken = tokens.accessToken,
            refreshToken = tokens.refreshToken ?: refreshToken,
            expiresIn = Clock.System.now() + tokens.expiresInSec.seconds
        )
    }

    private suspend fun set(
        tokens: AuthTokens,
        email: String,
        userId: Uuid
    ) = mutex.withLock {
        saveUserData(
            UserData(
                accessToken = tokens.accessToken,
                refreshToken = tokens.refreshToken,
                expiresIn = tokens.expiresIn,
                email = email,
                userId = userId
            )
        )
    }

    private fun loadUserData(): UserData? {
        val userData = settings.decodeValue<UserData?>(KEY_USER, null)
        info { "Get user data: $userData" }
        return userData
    }

    private fun saveUserData(userData: UserData) {
        info { "Save user data $userData" }
        settings.encodeValue(KEY_USER, userData)
    }

    private fun clearUserData() {
        info { "Clear user data" }
        settings.removeValue<UserData>(KEY_USER)
    }
}
