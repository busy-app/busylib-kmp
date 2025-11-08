package com.flipperdevices.bridge.connection.feature.link.check.ondemand.api

import com.flipperdevices.bridge.connection.feature.link.model.LinkedAccountInfo
import com.flipperdevices.bridge.connection.feature.rpc.api.critical.FRpcCriticalFeatureApi
import com.flipperdevices.bridge.connection.feature.rpc.api.model.RpcLinkedAccountInfo
import com.flipperdevices.bsb.auth.principal.api.BsbUserPrincipal
import com.flipperdevices.bsb.auth.principal.api.BsbUserPrincipalApi
import com.flipperdevices.bsb.cloud.api.BSBBarsApi
import com.flipperdevices.core.busylib.ktx.common.SingleJobMode
import com.flipperdevices.core.busylib.ktx.common.asSingleJobScope
import com.flipperdevices.core.busylib.ktx.common.exponentialRetry
import com.flipperdevices.core.busylib.log.LogTagProvider
import com.flipperdevices.core.busylib.log.error
import com.flipperdevices.core.busylib.log.info
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration.Companion.seconds

private val ACCOUNT_PROVIDING_TIMEOUT = 3.seconds

@Inject
class FLinkInfoOnDemandFeatureApiImpl(
    @Assisted private val rpcFeatureApi: FRpcCriticalFeatureApi,
    @Assisted private val scope: CoroutineScope,
    private val bsbUserPrincipalApi: BsbUserPrincipalApi,
    private val bsbBarsApi: BSBBarsApi
) : FLinkedInfoOnDemandFeatureApi, LogTagProvider {
    override val TAG = "FLinkedInfoFeatureApi"
    private val _status = MutableStateFlow<LinkedAccountInfo?>(null)
    override val status: Flow<LinkedAccountInfo> = _status.filterNotNull()
    private val singleJobScope = scope.asSingleJobScope()

    private fun RpcLinkedAccountInfo.asSealed(currentUserEmail: String?): LinkedAccountInfo {
        info { "Calculate state from $this and current user $currentUserEmail" }
        return when (this.state) {
            RpcLinkedAccountInfo.State.NOT_LINKED -> LinkedAccountInfo.NotLinked
            RpcLinkedAccountInfo.State.ERROR -> LinkedAccountInfo.Error
            RpcLinkedAccountInfo.State.LINKED -> {
                val linkedMail = this.email
                if (linkedMail != null && linkedMail == currentUserEmail) {
                    LinkedAccountInfo.Linked.SameUser(linkedMail)
                } else if (linkedMail != null) {
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
            val principal = bsbUserPrincipalApi.getPrincipalFlow()
                .filter { principal -> principal !is BsbUserPrincipal.Loading }
                .first() as? BsbUserPrincipal.Full

            info { "Local principal is ${principal?.userId}/${principal?.email}" }

            val info = exponentialRetry {
                rpcFeatureApi.invalidateLinkedUser(principal?.email)
                    .map { result -> result.asSealed(principal?.email) }
            }

            info { "BUSY Bar info is $info" }
            if (info is LinkedAccountInfo.NotLinked && principal != null) {
                info { "Start authorization for BUSY Bar..." }
                authBusyBar(principal).onFailure {
                    error(it) { "Failed authorize for BUSY Bar" }
                }
            }
            _status.emit(info)
        }
    }

    private suspend fun authBusyBar(
        principal: BsbUserPrincipal.Full
    ): Result<Unit> = runCatching {
        val linkCode = rpcFeatureApi.getLinkCode().getOrThrow()

        info { "Receive link code from BUSY Bar: $linkCode" }

        bsbBarsApi.registerBusyBar(principal, linkCode.code).getOrThrow()

        info { "BUSY Bar registered, waiting user registration from BUSY Bar" }

        var busyBarUser: RpcLinkedAccountInfo

        withTimeout(ACCOUNT_PROVIDING_TIMEOUT) {
            do {
                busyBarUser = rpcFeatureApi.invalidateLinkedUser(principal.email).getOrThrow()
                info { "Requested BUSY Bar user info: $busyBarUser" }
            } while (principal.email != busyBarUser.email)
        }

        info { "Completed authorization for BUSY Bar" }
    }

    @Inject
    class InternalFactory(
        protected val factory: (FRpcCriticalFeatureApi, CoroutineScope) -> FLinkInfoOnDemandFeatureApiImpl
    ) {
        operator fun invoke(
            rpcFeatureApi: FRpcCriticalFeatureApi,
            scope: CoroutineScope
        ): FLinkInfoOnDemandFeatureApiImpl = factory(rpcFeatureApi, scope)
    }
}
