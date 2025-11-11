package net.flipper.bridge.connection.feature.link.check.ondemand.api

import kotlinx.coroutines.flow.Flow
import net.flipper.bridge.connection.feature.common.api.FDeviceFeatureApi
import net.flipper.bridge.connection.feature.link.model.LinkedAccountInfo

interface FLinkedInfoOnDemandFeatureApi : FDeviceFeatureApi {
    val status: Flow<LinkedAccountInfo>

    fun tryCheckLinkedInfo()
}
