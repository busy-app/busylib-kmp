package com.flipperdevices.bridge.connection.feature.link.check.ondemand.api

import com.flipperdevices.bridge.connection.feature.common.api.FDeviceFeatureApi
import com.flipperdevices.bridge.connection.feature.link.model.LinkedAccountInfo
import kotlinx.coroutines.flow.Flow

interface FLinkedInfoOnDemandFeatureApi : FDeviceFeatureApi {
    val status: Flow<LinkedAccountInfo>

    fun tryCheckLinkedInfo()
}
