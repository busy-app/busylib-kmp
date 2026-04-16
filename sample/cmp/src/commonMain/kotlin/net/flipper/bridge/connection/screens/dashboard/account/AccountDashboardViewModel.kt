package net.flipper.bridge.connection.screens.dashboard.account

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.flipper.bridge.connection.feature.link.check.ondemand.api.FLinkedInfoOnDemandFeatureApi
import net.flipper.bridge.connection.feature.provider.api.FFeatureProvider
import net.flipper.bridge.connection.screens.dashboard.common.DashboardFeatureViewModel
import net.flipper.bridge.connection.utils.principal.impl.UserPrincipalApiSampleImpl
import net.flipper.bsb.auth.principal.api.BUSYLibUserPrincipal
import net.flipper.core.busylib.ktx.common.FlipperDispatchers
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.info

class AccountDashboardViewModel(
    private val featureProvider: FFeatureProvider,
    private val principalApi: UserPrincipalApiSampleImpl
) : DashboardFeatureViewModel(), LogTagProvider {
    override val TAG = "AccountDashboardViewModel"
    val linkedAccountStatusFlow = featureProvider
        .get(FLinkedInfoOnDemandFeatureApi::class)
        .getResource { it.status }

    val authState = principalApi.authStateFlow

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError = _loginError.asStateFlow()

    private val _isLoggingIn = MutableStateFlow(false)
    val isLoggingIn = _isLoggingIn.asStateFlow()

    fun login(email: String, password: String) {
        info { "#login $email and $password, principal api is $principalApi" }
        val api = principalApi
        viewModelScope.launch(FlipperDispatchers.default) {
            _isLoggingIn.value = true
            _loginError.value = null
            api.login(email, password)
                .onFailure { _loginError.value = it.message ?: "Login failed" }
            _isLoggingIn.value = false
        }
    }

    fun logout() {
        principalApi.logout()
    }

    fun deleteLinkedAccount() {
        viewModelScope.launch(FlipperDispatchers.default) {
            featureProvider.getSync(FLinkedInfoOnDemandFeatureApi::class)?.deleteAndLinkAccount()
        }
    }
}
