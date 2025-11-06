package com.flipperdevices.bsb.auth.principal.impl

import com.flipperdevices.bsb.auth.principal.api.BsbUserPrincipal
import com.flipperdevices.bsb.auth.principal.api.BsbUserPrincipalApi
import com.flipperdevices.bsb.auth.principal.model.PreferenceBsbUser
import com.flipperdevices.bsb.preference.api.PreferenceApi
import com.flipperdevices.bsb.preference.api.getFlow
import com.flipperdevices.bsb.preference.api.set
import com.flipperdevices.bsb.preference.model.SettingsEnum
import com.flipperdevices.core.di.AppGraph
import com.flipperdevices.core.log.info
import com.flipperdevices.core.log.sensitive
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@Inject
@SingleIn(AppGraph::class)
@ContributesBinding(AppGraph::class, binding<BsbUserPrincipalApi>())
class UserPrincipalApiImpl(
    private val preferenceApi: PreferenceApi,
    coroutineScope: CoroutineScope
) : BsbUserPrincipalApi {
    private val principalStateFlow = MutableStateFlow<BsbUserPrincipal>(BsbUserPrincipal.Loading)

    init {
        combine(
            preferenceApi.getFlow<String?>(SettingsEnum.AUTH_TOKEN, null),
            preferenceApi.getFlow<PreferenceBsbUser?>(SettingsEnum.USER_DATA, null)
        ) { authToken, bsbUser ->
            return@combine if (authToken == null) {
                BsbUserPrincipal.Empty
            } else if (bsbUser == null) {
                BsbUserPrincipal.Token(authToken)
            } else {
                BsbUserPrincipal.Full(
                    authToken,
                    email = bsbUser.email,
                    userId = bsbUser.userId
                )
            }
        }.onEach {
            principalStateFlow.emit(it)
        }.launchIn(coroutineScope)
    }

    override fun getPrincipalFlow() = principalStateFlow

    override suspend fun <T> withTokenPrincipal(block: suspend (BsbUserPrincipal.Token) -> Result<T>): Result<T> {
        val principal = principalStateFlow
            .filterNotNull()
            .first()
        if (principal !is BsbUserPrincipal.Token) {
            return Result.failure(IllegalStateException("User not found"))
        }
        return block(principal)
    }

    override fun setPrincipal(principal: BsbUserPrincipal) {
        val newAuthToken: String?
        val newUserData: PreferenceBsbUser?
        when (principal) {
            BsbUserPrincipal.Empty,
            BsbUserPrincipal.Loading -> {
                newAuthToken = null
                newUserData = null
            }

            is BsbUserPrincipal.Token -> {
                newAuthToken = principal.token
                newUserData = when (principal) {
                    is BsbUserPrincipal.Full -> {
                        PreferenceBsbUser(
                            email = principal.email,
                            userId = principal.userId
                        )
                    }

                    is BsbUserPrincipal.Token.Impl -> {
                        null
                    }
                }
            }
        }

        sensitive { "Update token to $newAuthToken" }
        info { "Update user data to $newUserData" }

        preferenceApi.set(SettingsEnum.AUTH_TOKEN, newAuthToken)
        preferenceApi.set(SettingsEnum.USER_DATA, newUserData)
    }
}
