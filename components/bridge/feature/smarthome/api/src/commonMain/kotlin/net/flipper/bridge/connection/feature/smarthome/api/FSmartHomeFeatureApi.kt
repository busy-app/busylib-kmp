package net.flipper.bridge.connection.feature.smarthome.api

import net.flipper.bridge.connection.feature.common.api.FDeviceFeatureApi
import net.flipper.bridge.connection.feature.smarthome.model.SmartHomePairCodeData
import net.flipper.bridge.connection.feature.smarthome.model.SmartHomePairCodeTimeLeftData
import net.flipper.bridge.connection.feature.smarthome.model.SmartHomeState
import net.flipper.busylib.core.wrapper.CResult
import net.flipper.busylib.core.wrapper.WrappedFlow
import net.flipper.busylib.core.wrapper.WrappedStateFlow

interface FSmartHomeFeatureApi : FDeviceFeatureApi {

    fun getState(): WrappedStateFlow<SmartHomeState>
    suspend fun getPairCode(): CResult<SmartHomePairCodeData>
    suspend fun forgetAllPairings(): CResult<Unit>

    // todo @Programistich check on iOS
    fun getPairCodeWithTimeLeft(): WrappedFlow<SmartHomePairCodeTimeLeftData?>
}
