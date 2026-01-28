package net.flipper.bridge.connection.feature.smarthome.api

import net.flipper.bridge.connection.feature.common.api.FDeviceFeatureApi
import net.flipper.bridge.connection.feature.rpc.api.model.MatterCommissioningPayload
import net.flipper.bridge.connection.feature.smarthome.model.MatterCommissioningTimeLeftPayload
import net.flipper.bridge.connection.feature.smarthome.model.SmartHomeState
import net.flipper.busylib.core.wrapper.CResult
import net.flipper.busylib.core.wrapper.WrappedFlow
import net.flipper.busylib.core.wrapper.WrappedStateFlow

interface FSmartHomeFeatureApi : FDeviceFeatureApi {

    fun getState(): WrappedStateFlow<SmartHomeState>
    suspend fun getPairCode(): CResult<MatterCommissioningPayload>
    suspend fun forgetAllPairings(): CResult<Unit>

    // todo @Programistich check on iOS
    fun getPairCodeWithTimeLeft(): WrappedFlow<MatterCommissioningTimeLeftPayload?>
}
