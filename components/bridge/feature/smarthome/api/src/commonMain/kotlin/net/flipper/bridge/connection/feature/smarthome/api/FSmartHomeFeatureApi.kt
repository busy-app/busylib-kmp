package net.flipper.bridge.connection.feature.smarthome.api

import net.flipper.bridge.connection.feature.common.api.FDeviceFeatureApi
import net.flipper.bridge.connection.feature.smarthome.model.BsbMatterCommissionedFabrics
import net.flipper.bridge.connection.feature.smarthome.model.BsbMatterCommissioningPayload
import net.flipper.bridge.connection.feature.smarthome.model.BsbMatterCommissioningTimeLeftPayload
import net.flipper.busylib.core.wrapper.CResult
import net.flipper.busylib.core.wrapper.WrappedFlow

interface FSmartHomeFeatureApi : FDeviceFeatureApi {

    fun getCommissionedFabricsFlow(): WrappedFlow<BsbMatterCommissionedFabrics>
    suspend fun getPairCode(): CResult<BsbMatterCommissioningPayload>
    suspend fun forgetAllPairings(): CResult<Unit>

    // todo @Programistich check on iOS
    fun getPairCodeWithTimeLeft(): WrappedFlow<BsbMatterCommissioningTimeLeftPayload?>
}
