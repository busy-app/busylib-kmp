package net.flipper.bridge.connection.feature.oncall.api

import net.flipper.bridge.connection.feature.common.api.FDeviceFeatureApi

interface FOnCallFeatureApi : FDeviceFeatureApi {
    fun start()
    fun stop()
}
