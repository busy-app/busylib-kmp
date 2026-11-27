package net.flipper.bridge.connection.feature.oncall.api

import net.flipper.bridge.connection.feature.common.api.FDeviceFeatureApi
import net.flipper.bridge.connection.feature.oncall.api.model.OnCallAnimation

interface FOnCallFeatureApi : FDeviceFeatureApi {
    fun start(animation: OnCallAnimation)
    fun stop()
}
