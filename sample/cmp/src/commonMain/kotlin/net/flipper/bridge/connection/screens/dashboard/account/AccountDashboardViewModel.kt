package net.flipper.bridge.connection.screens.dashboard.account

import kotlinx.coroutines.launch
import net.flipper.bridge.connection.feature.link.check.ondemand.api.FLinkedInfoOnDemandFeatureApi
import net.flipper.bridge.connection.feature.provider.api.FFeatureProvider
import net.flipper.bridge.connection.screens.dashboard.common.DashboardFeatureViewModel
import net.flipper.core.busylib.ktx.common.FlipperDispatchers

class AccountDashboardViewModel(
    private val featureProvider: FFeatureProvider
) : DashboardFeatureViewModel() {
    val linkedAccountStatusFlow = featureProvider
        .get(FLinkedInfoOnDemandFeatureApi::class)
        .getResource { it.status }

    fun deleteLinkedAccount() {
        viewModelScope.launch(FlipperDispatchers.default) {
            featureProvider.getSync(FLinkedInfoOnDemandFeatureApi::class)?.deleteAccount()
        }
    }
}
