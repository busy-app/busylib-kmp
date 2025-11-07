package com.flipperdevices.bridge.connection.feature.link.check.ondemand.api

import com.flipperdevices.bridge.connection.feature.link.model.LinkedAccountInfo
import com.flipperdevices.bridge.connection.feature.rpc.api.critical.FRpcCriticalFeatureApi
import com.flipperdevices.bridge.connection.feature.rpc.api.model.RpcLinkedAccountInfo
import com.flipperdevices.bsb.auth.principal.api.BsbUserPrincipal
import com.flipperdevices.bsb.auth.principal.api.BsbUserPrincipalApi
import com.flipperdevices.core.busylib.ktx.common.SingleJobMode
import com.flipperdevices.core.busylib.ktx.common.asSingleJobScope
import com.flipperdevices.core.busylib.ktx.common.exponentialRetry
import com.flipperdevices.core.busylib.log.LogTagProvider
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first

@Inject
class FLinkInfoOnDemandFeatureApiImpl(
    @Assisted private val rpcFeatureApi: FRpcCriticalFeatureApi,
    @Assisted private val scope: CoroutineScope,
    private val bsbUserPrincipalApi: BsbUserPrincipalApi
) : FLinkedInfoOnDemandFeatureApi, LogTagProvider {
    override val TAG = "FLinkedInfoFeatureApi"
    private val _status = MutableStateFlow<LinkedAccountInfo?>(null)
    override val status: Flow<LinkedAccountInfo> = _status.filterNotNull()
    private val singleJobScope = scope.asSingleJobScope()

    private fun RpcLinkedAccountInfo.asSealed(currentUserUuid: String?): LinkedAccountInfo {
        return when (this.state) {
            RpcLinkedAccountInfo.State.NOT_LINKED -> LinkedAccountInfo.NotLinked
            RpcLinkedAccountInfo.State.ERROR -> LinkedAccountInfo.Error
            RpcLinkedAccountInfo.State.LINKED -> {
                val linkedUuid = this.id?.toString()
                val linkedMail = this.email
                if (linkedMail != null && linkedUuid != null && linkedUuid == currentUserUuid) {
                    LinkedAccountInfo.Linked.SameUser(linkedMail)
                } else if (linkedUuid != null && linkedMail != null) {
                    LinkedAccountInfo.Linked.DifferentUser(linkedMail)
                } else {
                    LinkedAccountInfo.Error
                }
            }

            RpcLinkedAccountInfo.State.DISCONNECTED -> LinkedAccountInfo.Disconnected
        }
    }

    override fun tryCheckLinkedInfo() {
        singleJobScope.launch(SingleJobMode.CANCEL_PREVIOUS) {
            val principal = bsbUserPrincipalApi
                .getPrincipalFlow()
                .filter { principal -> principal !is BsbUserPrincipal.Loading }
                .first() as? BsbUserPrincipal.Full
            val info = exponentialRetry {
                rpcFeatureApi.checkLinkedUser(principal?.userId)
                    .map { result -> result.asSealed(principal?.userId) }
            }
            _status.emit(info)
        }
    }

    @AssistedFactory
    interface InternalFactory {
        operator fun invoke(
            rpcFeatureApi: FRpcCriticalFeatureApi,
            scope: CoroutineScope
        ): FLinkInfoOnDemandFeatureApiImpl
    }
}
