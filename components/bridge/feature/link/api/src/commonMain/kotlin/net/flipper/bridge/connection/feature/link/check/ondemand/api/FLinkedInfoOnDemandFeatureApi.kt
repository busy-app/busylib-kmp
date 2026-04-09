package net.flipper.bridge.connection.feature.link.check.ondemand.api

import net.flipper.bridge.connection.feature.common.api.FDeviceFeatureApi
import net.flipper.bridge.connection.feature.link.model.LinkedAccountInfo
import net.flipper.busylib.core.wrapper.CResult
import net.flipper.busylib.core.wrapper.WrappedFlow

interface FLinkedInfoOnDemandFeatureApi : FDeviceFeatureApi {
    val status: WrappedFlow<LinkedAccountInfo>

    /**
     * Deletes current account from BusyBar and tries to
     * link it to current user
     */
    suspend fun deleteAndLinkAccount(): CResult<Unit>
}
