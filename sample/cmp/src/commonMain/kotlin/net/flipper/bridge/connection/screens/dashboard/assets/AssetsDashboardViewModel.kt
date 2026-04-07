package net.flipper.bridge.connection.screens.dashboard.assets

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import net.flipper.bridge.connection.feature.provider.api.FFeatureProvider
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcFeatureApi
import net.flipper.bridge.connection.screens.dashboard.common.DashboardFeatureViewModel
import net.flipper.bridge.connection.screens.dashboard.common.SAMPLE_APP_ID
import net.flipper.bridge.connection.screens.dashboard.common.SAMPLE_ASSET_PATH
import kotlin.time.Clock

class AssetsDashboardViewModel(
    private val featureProvider: FFeatureProvider
) : DashboardFeatureViewModel() {
    private val mutableState = MutableStateFlow(AssetsDashboardState())
    val state: StateFlow<AssetsDashboardState> = mutableState

    fun uploadSampleAsset() = runAction("assets upload") {
        val rpcFeatureApi = requireFeature<FRpcFeatureApi>(featureProvider, "RPC")
        val payload = buildString {
            append("rpc-contract sample\n")
            append("timestamp=")
            append(Clock.System.now())
        }.encodeToByteArray()
        rpcFeatureApi.fRpcAssetsApi.uploadAsset(
            appId = SAMPLE_APP_ID,
            file = SAMPLE_ASSET_PATH,
            content = payload
        ).getOrThrow()
        mutableState.value = mutableState.value.copy(lastUploadedAssetPath = SAMPLE_ASSET_PATH)
        appendLog("Asset uploaded to $SAMPLE_ASSET_PATH")
    }
}

data class AssetsDashboardState(
    val lastUploadedAssetPath: String? = null
)
