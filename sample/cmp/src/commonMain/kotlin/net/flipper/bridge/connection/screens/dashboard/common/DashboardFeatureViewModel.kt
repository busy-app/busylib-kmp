package net.flipper.bridge.connection.screens.dashboard.common

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import net.flipper.bridge.connection.feature.common.api.FDeviceFeatureApi
import net.flipper.bridge.connection.feature.provider.api.FFeatureStatus
import net.flipper.bridge.connection.screens.decompose.DecomposeViewModel

private const val STOP_TIMEOUT_MILLIS = 5_000L

abstract class DashboardFeatureViewModel : DecomposeViewModel() {
    protected fun <T : FDeviceFeatureApi, R> Flow<FFeatureStatus<T>>.getResource(
        block: (T) -> Flow<R>
    ): StateFlow<R?> {
        return flatMapLatest { feature ->
            when (feature) {
                FFeatureStatus.NotFound,
                FFeatureStatus.Retrieving,
                FFeatureStatus.Unsupported -> MutableStateFlow(null)

                is FFeatureStatus.Supported<T> -> block(feature.featureApi)
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = null
        )
    }
}
