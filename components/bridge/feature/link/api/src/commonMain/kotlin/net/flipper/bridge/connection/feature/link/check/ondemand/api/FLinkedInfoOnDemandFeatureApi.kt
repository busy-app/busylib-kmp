package net.flipper.bridge.connection.feature.link.check.ondemand.api

import net.flipper.bridge.connection.feature.common.api.FDeviceFeatureApi
import net.flipper.bridge.connection.feature.link.model.LinkedAccountInfo
import net.flipper.busylib.core.wrapper.WrappedFlow

interface FLinkedInfoOnDemandFeatureApi : FDeviceFeatureApi {
    val status: WrappedFlow<LinkedAccountInfo>

    fun tryCheckLinkedInfo()
}
