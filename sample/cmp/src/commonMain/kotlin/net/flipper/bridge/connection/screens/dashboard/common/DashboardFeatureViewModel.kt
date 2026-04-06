package net.flipper.bridge.connection.screens.dashboard.common

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import net.flipper.bridge.connection.feature.common.api.FDeviceFeatureApi
import net.flipper.bridge.connection.feature.provider.api.FFeatureProvider
import net.flipper.bridge.connection.feature.provider.api.FFeatureStatus
import net.flipper.bridge.connection.feature.provider.api.getSync
import net.flipper.bridge.connection.screens.decompose.DecomposeViewModel
import net.flipper.core.busylib.ktx.common.FlipperDispatchers
import kotlin.time.Clock

private const val STOP_TIMEOUT_MILLIS = 5_000L
private const val LOG_LIMIT = 16

data class DashboardActionState(
    val lastAction: String? = null,
    val logs: List<String> = emptyList()
)

abstract class DashboardFeatureViewModel : DecomposeViewModel() {
    protected val mutableActionState = MutableStateFlow(DashboardActionState())
    val actionState: StateFlow<DashboardActionState> = mutableActionState

    protected fun <T : FDeviceFeatureApi, R> Flow<FFeatureStatus<T>>.getResource(
        block: (T) -> Flow<R>
    ): StateFlow<R?> {
        return flatMapLatest { feature ->
            when (feature) {
                FFeatureStatus.NotFound,
                FFeatureStatus.Retrieving,
                FFeatureStatus.Unsupported -> flowOf(null)

                is FFeatureStatus.Supported<T> -> block(feature.featureApi)
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = null
        )
    }

    protected fun runAction(
        actionName: String,
        block: suspend () -> Unit
    ) {
        viewModelScope.launch(FlipperDispatchers.default) {
            mutableActionState.value = mutableActionState.value.copy(lastAction = actionName)
            runCatching { block() }
                .onFailure { error ->
                    appendLog("$actionName failed: ${error.message ?: error::class.simpleName.orEmpty()}")
                }
        }
    }

    protected suspend inline fun <reified T : FDeviceFeatureApi> requireFeature(
        featureProvider: FFeatureProvider,
        featureName: String
    ): T {
        return requireNotNull(featureProvider.getSync<T>()) {
            "$featureName feature is unavailable"
        }
    }

    protected fun appendLog(message: String) {
        val line = "[${Clock.System.now()}] $message"
        mutableActionState.value = mutableActionState.value.copy(
            logs = (listOf(line) + mutableActionState.value.logs).take(LOG_LIMIT)
        )
    }
}
